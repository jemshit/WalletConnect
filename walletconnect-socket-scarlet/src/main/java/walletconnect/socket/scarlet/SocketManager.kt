/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.socket.scarlet

import com.google.gson.Gson
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Message
import com.tinder.scarlet.State
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import walletconnect.core.Failure
import walletconnect.core.FailureType
import walletconnect.core.socket.Socket
import walletconnect.core.socket.model.SocketConnectionState
import walletconnect.core.socket.model.SocketMessage
import walletconnect.core.util.DispatcherProvider
import walletconnect.core.util.Logger
import walletconnect.core.util.logger_impl.EmptyLogger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

class SocketManager(private val socketServiceFactory: (LifecycleRegistry) -> SocketService,
                    private val gson: Gson,
                    private val dispatcherProvider: DispatcherProvider,
                    private val logger: Logger = EmptyLogger,
                    private val logTagSuffix: String = "")
    : Socket {

    private val LogTag = "Socket$logTagSuffix"

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var socketService: SocketService
    private lateinit var lifecycleRegistry: LifecycleRegistry

    /** [open], [reconnect], [disconnect], [close] method calls are synchronized */
    private val openCloseLock = ReentrantLock(true)
    private val initialized = AtomicBoolean(false)
    private val connectionState = AtomicReference<State>(State.Destroyed)
    private val isConnected = AtomicBoolean(false)
    private val MaxRetryCount = 20
    private var retryCounter = AtomicInteger(0)

    @Volatile
    private var connectionListener: ((SocketConnectionState) -> Unit)? = null
    @Volatile
    private var lastNotifiedConnectionState: SocketConnectionState? = null
    @Volatile
    private var errorListener: ((Failure, Boolean) -> Unit)? = null
    @Volatile
    private var messageListener: ((SocketMessage) -> Unit)? = null

    private val publishMessageQueue = MutableSharedFlow<SocketMessage>(
            replay = 0,
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.SUSPEND
    )

    // region Connection
    // Idempotent
    override fun open(connectionListener: (SocketConnectionState) -> Unit,
                      errorListener: (Failure, Boolean) -> Unit,
                      messageListener: ((SocketMessage) -> Unit)?) {
        if (initialized.get()) {
            return
        }

        openCloseLock.lock()
        // double-check. multiple threads can pass initial check and wait in queue for lock
        if (initialized.get()) {
            openCloseLock.unlock()
            return
        }
        logger.info(LogTag, "open()")

        this.lifecycleRegistry = LifecycleRegistry(0L)
        this.socketService = socketServiceFactory(lifecycleRegistry)
        this.coroutineScope = CoroutineScope(dispatcherProvider.io() + SupervisorJob())
        this.connectionListener = connectionListener
        this.errorListener = errorListener

        // set as late as possible, but methods below check this
        initialized.set(true)

        observeConnectionEvents()
        processMessageQueue()
        observeReceivedMessages()
        messageListener?.let { subscribeToAll(it) }

        // start connection
        notifyConnectionListener(SocketConnectionState.Connecting)
        lifecycleRegistry.onNext(Lifecycle.State.Started)

        openCloseLock.unlock()
    }

    private fun observeConnectionEvents() {
        logger.info(LogTag, "observeConnectionEvents()")

        socketService.observeConnectionEvents()
                .onEach {
                    logger.debug(LogTag, "ConnectionState:${it.getName()}")
                    connectionState.set(it)
                    isConnected.set(it is State.Connected)
                }
                .onEach { state ->
                    when (state) {
                        is State.WaitingToRetry -> {
                            // disconnected, manual retry can be triggered
                            notifyConnectionListener(SocketConnectionState.Disconnected)
                        }
                        is State.Connecting -> {
                            val retryCount = retryCounter.incrementAndGet()

                            if (retryCount >= MaxRetryCount) {
                                logger.warning(LogTag, "MaxRetry reached internally. Disconnect!")
                                disconnect()
                            } else {
                                notifyConnectionListener(SocketConnectionState.Connecting)
                            }
                        }
                        is State.Connected -> {
                            retryCounter.set(0)
                            notifyConnectionListener(SocketConnectionState.Connected)
                        }
                        is State.Disconnecting -> {

                        }
                        is State.Disconnected -> {
                            notifyConnectionListener(SocketConnectionState.Disconnected)
                        }
                        is State.Destroyed -> {
                            notifyConnectionListener(SocketConnectionState.Closed)
                        }
                    }
                }
                .catch {
                    notifyErrorListener(error = Failure(type = FailureType.SocketConnectionFlow,
                                                        cause = it),
                                        fatal = true)
                    logger.error(LogTag, "observeConnectionEvents():\n${it.stackTraceToString()}")
                    close()
                }
                .launchIn(coroutineScope)
    }

    override fun isConnected()
            : Boolean {
        return isConnected.get()
    }

    /** if same state is emitted consecutively, [connectionListener] is called only once */
    private fun notifyConnectionListener(connectionState: SocketConnectionState) {
        if (connectionState != lastNotifiedConnectionState) {
            lastNotifiedConnectionState = connectionState
            connectionListener?.invoke(connectionState)
        }
    }

    private fun notifyErrorListener(error: Failure,
                                    fatal: Boolean) {
        errorListener?.invoke(error, fatal)
    }

    override fun disconnect() {
        val isRetrying = connectionState.get() is State.Connecting || connectionState.get() is State.WaitingToRetry
        if (!isConnected.get() && !isRetrying) {
            // while retrying, allow to disconnect
            return
        }

        openCloseLock.lock()
        logger.info(LogTag, "disconnect()")

        retryCounter.set(0)
        lifecycleRegistry.onNext(Lifecycle.State.Stopped.AndAborted)

        openCloseLock.unlock()
    }

    override fun reconnect() {
        // when retried manually, then auto retry delay starts after this manual retry

        // if connected, Lifecycle.Start does not change anything
        // if waitingToRetry, Lifecycle.Start triggers retry
        // if disconnected, Lifecycle.Start does not change anything
        // if destroyed, Lifecycle.Start triggers connecting

        if (!initialized.get() || connectionState.get() is State.Destroyed) {
            logger.warning(
                    LogTag,
                    "reconnect() is called but !initialized:${!initialized.get()} " +
                    "|| connectionState:${connectionState.get().getName()}"
            )
            return
        }
        if (isConnected.get()) {
            return
        }

        openCloseLock.lock()
        logger.info(LogTag, "reconnect()")
        lifecycleRegistry.onNext(Lifecycle.State.Started)
        openCloseLock.unlock()
    }

    override fun close() {
        if (!initialized.get()) {
            return
        }

        openCloseLock.lock()
        // double-check. multiple threads can pass initial check and wait in queue for lock
        if (!initialized.get()) {
            openCloseLock.unlock()
            return
        }
        logger.info(LogTag, "close()")

        // set asap
        initialized.set(false)

        // reset
        retryCounter.set(0)
        messageListener = null // unsubscribeFromAll() checks 'initialized'
        publishMessageQueue.resetReplayCache()

        // close
        try {
            lifecycleRegistry.onNext(Lifecycle.State.Destroyed)
        } catch (_: Exception) {
            // internal NullPointerException
        }
        coroutineScope.cancel()

        // coroutineScope is cancelled, update these manually
        connectionState.set(State.Destroyed)
        isConnected.set(false)
        notifyConnectionListener(SocketConnectionState.Closed)

        // reset listeners
        connectionListener = null
        errorListener = null
        lastNotifiedConnectionState = null

        openCloseLock.unlock()
    }
    // endregion

    // region Subscribe to Messages
    override fun subscribeToAll(listener: (SocketMessage) -> Unit) {
        if (!initialized.get()) {
            logger.warning(LogTag, "subscribeToAll() is called but !initialized")
            return
        }
        logger.debug(LogTag, "subscribeToAll()")

        this.messageListener = listener
    }

    private fun observeReceivedMessages() {
        logger.info(LogTag, "observeReceivedMessages()")

        socketService.observeMessages()
                .filter { it is WebSocket.Event.OnMessageReceived }
                // after buffer, it runs on parallel coroutine (separate from upstream)
                .buffer(50, BufferOverflow.SUSPEND)
                // map to SocketMessage, filter result if error
                .mapNotNull { event ->
                    val messageEvent = event as WebSocket.Event.OnMessageReceived
                    deserializeEvent(messageEvent)
                }
                .onEach { socketMessage ->
                    onMessage(socketMessage)
                }
                .catch {
                    notifyErrorListener(error = Failure(type = FailureType.SocketMessageFlow,
                                                        cause = it),
                                        fatal = true)
                    logger.error(LogTag, "observeReceivedMessages():\n${it.stackTraceToString()}")
                    close()
                }
                .launchIn(coroutineScope)
    }

    private suspend fun deserializeEvent(event: WebSocket.Event.OnMessageReceived)
            : SocketMessage? {
        return withContext(dispatcherProvider.computation()) {
            var notifiedError = false

            // get json string
            val json = try {
                (event.message as Message.Text).value
            } catch (error: Throwable) {
                notifiedError = true
                notifyErrorListener(error = Failure(type = FailureType.DeserializeSocketMessage,
                                                    cause = error),
                                    fatal = false)
                logger.error(LogTag, "Received NullOrEmpty Message.\n${error.stackTraceToString()}")
                null
            }
            if (json.isNullOrBlank()) {
                if (!notifiedError) {
                    notifyErrorListener(error = Failure(type = FailureType.DeserializeSocketMessage,
                                                        message = "Message: $json"),
                                        fatal = false)
                }
                logger.error(LogTag, "Received Message: $json")
                return@withContext null
            }

            // deserialize json
            val socketMessage: SocketMessage = try {
                gson.fromJson(json, SocketMessage::class.java)
            } catch (error: Throwable) {
                notifyErrorListener(error = Failure(type = FailureType.DeserializeSocketMessage,
                                                    cause = error),
                                    fatal = false)
                logger.error(LogTag, "Received Message Deserialize: $json \n${error.stackTraceToString()}")
                return@withContext null
            }

            return@withContext socketMessage
        }
    }

    private fun onMessage(message: SocketMessage) {
        if (messageListener == null) {
            logger.warning(LogTag, "onMessage(): No Listeners for $message")
            return
        }

        logger.debug(LogTag, "onMessage(): $message")
        messageListener?.invoke(message)
    }

    override fun unsubscribeFromAll() {
        if (!initialized.get()) {
            logger.warning(LogTag, "unsubscribeFromAll() is called but !initialized")
            return
        }
        logger.debug(LogTag, "unsubscribeFromAll()")

        messageListener = null
    }
    // endregion

    // region Publish Messages
    override fun publish(message: SocketMessage,
                         queueIfDisconnected: Boolean) {
        if (!initialized.get()) {
            logger.warning(LogTag,
                           "publish() is called but !initialized. message:$message, queueOnDisconnected:$queueIfDisconnected")
            return
        }

        if (!isConnected.get()) {
            if (!queueIfDisconnected) {
                notifyErrorListener(error = Failure(type = FailureType.SocketPublishMessage(message),
                                                    message = "!queueOnDisconnected && !isConnected"),
                                    fatal = false)
                logger.warning(LogTag, "publish(): !queueOnDisconnected && !isConnected")
                return
            } else {
                logger.info(LogTag, "Message is queued: $message")
            }
        }

        publishMessageQueue.tryEmit(message)
    }

    private fun processMessageQueue() {
        logger.info(LogTag, "processMessageQueue()")

        publishMessageQueue
                .onEach { message ->
                    if (!initialized.get()) {
                        return@onEach
                    }

                    // wait till connected
                    while (!isConnected.get()) {
                        delay(500L)

                        if (!initialized.get()) {
                            return@onEach
                        }
                    }

                    // retry onError even after isConnected=true
                    var success = false
                    var retryCount = 0
                    while (!success && retryCount < 5) {
                        retryCount += 1
                        try {
                            socketService.sendMessage(message)
                            logger.info(LogTag, "processMessageQueue(): Sent: $message")
                            success = true
                        } catch (error: Throwable) {
                            logger.warning(LogTag, "processMessageQueue(): $message. \n${error.stackTraceToString()}")
                            delay(1_000L * retryCount)
                        }

                        if (!initialized.get()) {
                            return@onEach
                        }
                    }
                    if (!success) {
                        notifyErrorListener(error = Failure(type = FailureType.SocketPublishMessage(message)),
                                            fatal = false)
                        logger.error(LogTag, "processMessageQueue(): Failed to send even after connected: $message")
                    }

                }
                .catch {
                    notifyErrorListener(error = Failure(type = FailureType.SocketPublishFlow,
                                                        cause = it),
                                        fatal = true)
                    logger.error(LogTag, "processMessageQueue(): ${it.stackTraceToString()}")
                    close()
                }
                .launchIn(coroutineScope)
    }
    // endregion

    private fun State.getName(): String {
        return when (this) {
            is State.Connected -> "Connected"
            is State.WaitingToRetry -> "WaitingToRetry"
            is State.Connecting -> "Connecting"
            is State.Disconnecting -> "Disconnecting"
            is State.Disconnected -> "Disconnected"
            is State.Destroyed -> "Destroyed"
        }
    }

}