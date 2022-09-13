/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import walletconnect.core.Failure
import walletconnect.core.FailureType
import walletconnect.core.adapter.JsonAdapter
import walletconnect.core.cryptography.Cryptography
import walletconnect.core.cryptography.hexToByteArray
import walletconnect.core.session.SessionLifecycle
import walletconnect.core.session.callback.CallbackData
import walletconnect.core.session.callback.FailureCallback
import walletconnect.core.session.callback.SessionCallback
import walletconnect.core.session.callback.SocketCallback
import walletconnect.core.session.model.EncryptedPayload
import walletconnect.core.session.model.InitialSessionState
import walletconnect.core.session.model.SessionUpdate
import walletconnect.core.session.model.json_rpc.*
import walletconnect.core.session_state.SessionStore
import walletconnect.core.session_state.model.SessionState
import walletconnect.core.socket.Socket
import walletconnect.core.socket.model.SocketConnectionState
import walletconnect.core.socket.model.SocketMessage
import walletconnect.core.socket.model.SocketMessageType
import walletconnect.core.toFailure
import walletconnect.core.util.DispatcherProvider
import walletconnect.core.util.Logger
import walletconnect.core.util.logger_impl.EmptyLogger
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max

abstract class WalletConnectCore(private val isDApp: Boolean,
                                 protected val socket: Socket,
                                 protected val sessionStore: SessionStore,
                                 protected val jsonAdapter: JsonAdapter,
                                 protected val dispatcherProvider: DispatcherProvider,
                                 protected val logger: Logger = EmptyLogger)
    : SessionLifecycle {

    private val LogTag = "WalletConnect"

    protected lateinit var coroutineScope: CoroutineScope
    protected val initialized = AtomicBoolean(false)
    /** [openSocket], [disconnectSocket], [reconnectSocket], [close] method calls are synchronized */
    private val socketConnectionLock = ReentrantLock(true)
    @Volatile
    /** Initialized in [openSocket] and never changes until next [openSocket] */
    protected lateinit var initialState: InitialSessionState
    @Volatile
    /**
     * Old Session:
     * - Restored on [openSocket]
     *
     * New Session:
     * - Wallet: partially initialized on [SessionCallback.SessionRequested], fully initialized on [SessionCallback.SessionApproved]
     * - DApp: fully initialized on [SessionCallback.SessionApproved]
     *
     * It is persisted on [SessionCallback.SessionApproved] and removed on ([SessionCallback.SessionDeleted] or [SessionCallback.SessionRejected]).
     *  [close].deleteLocal hints for removal
     */
    protected var sessionState: SessionState? = null
        set(value) {
            field = value

            // notifySessionState
            callback(SessionCallback.LocalSessionStateUpdated(sessionState))
        }
    @Volatile
    /** Used by wallet to approve, reject; used by dApp to find out if approved, rejected */
    protected var sessionRequestId: Long? = null
    @Volatile
    protected var sessionApproved = AtomicBoolean(false)
    protected val subscribedToAllMessages = AtomicBoolean(false)
    private val incomingMessageQueue = MutableSharedFlow<SocketMessage>(
            replay = 0,
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val mapType by lazy {
        jsonAdapter.getParametrizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
        )
    }
    private val outgoingMessageCount = AtomicInteger(0)
    /**
     * All [SocketMessageType.Pub] messages should be published after [SocketMessageType.Sub] messages ([subscribedToAllMessages])
     */
    private val outgoingMessageQueue = MutableSharedFlow<Pair<SocketMessage, Boolean>>(
            replay = 0,
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.SUSPEND
    )
    /**
     * - used to invoke right callback
     * - this might get lost in case of activity/process death etc..
     * - don't clean this after processing, user might want to resend/retry
     */
    protected val messageMethodMap: MutableMap<Long, JsonRpcMethod> = mutableMapOf()
    private val requestTypeCache: MutableMap<Type, Type> = mutableMapOf()
    private val responseTypeCache: MutableMap<Type, Type> = mutableMapOf()
    private var sessionCallback: ((CallbackData) -> Unit)? = null

    /** It might emit same item twice. some cases require it, so don't filter here */
    protected fun callback(callbackData: CallbackData) {
        sessionCallback?.invoke(callbackData)
    }

    protected fun failureCallback(failure: Failure) {
        callback(FailureCallback(failure))
    }

    protected fun getRequestType(itemType: Type)
            : Type {
        if (requestTypeCache.containsKey(itemType)) {
            logger.debug(LogTag, "#Type: returning from requestList cache(${itemType})")
            return requestTypeCache[itemType]!!
        }

        // put to cache
        requestTypeCache[itemType] = jsonAdapter.getParametrizedType(
                JsonRpcRequest::class.java,
                itemType
        )

        return requestTypeCache[itemType]!!
    }

    protected fun getResponseType(resultType: Type)
            : Type {
        if (responseTypeCache.containsKey(resultType)) {
            logger.debug(LogTag, "#Type: returning from response cache(${resultType})")
            return responseTypeCache[resultType]!!
        }

        // put to cache
        responseTypeCache[resultType] = jsonAdapter.getParametrizedType(
                JsonRpcResponse::class.java,
                resultType
        )

        return responseTypeCache[resultType]!!
    }

    // region Lifecycle
    override fun openSocket(initialState: InitialSessionState,
                            callback: ((CallbackData) -> Unit)?,
                            onOpen: (() -> Unit)?) {
        if (initialized.get()) {
            return
        }
        if (socket.isConnected()) {
            logger.error(LogTag, "#openSocket(): Already socket.isConnected")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "socket isConnected, can't reuse"))
            return
        }

        Thread {
            socketConnectionLock.lock()
            // double-check. multiple threads can pass initial check and wait in queue for lock
            if (!initialized.get()) {
                val prevSessionState = runBlocking {
                    // it returns from in-memory cache if [sessionStore] was initialized before
                    sessionStore.get(topic = initialState.connectionParams.topic)
                }
                logger.info(LogTag, "#openSocket($initialState)\nprevSessionState=$prevSessionState")

                try {
                    this.coroutineScope = CoroutineScope(dispatcherProvider.io() + SupervisorJob())
                    this.initialState = initialState
                    this.sessionCallback = callback // init before [sessionState]
                    this.sessionState = prevSessionState // this uses [sessionCallback]
                    if (prevSessionState != null) {
                        this.sessionApproved.set(true)
                        callback(SessionCallback.SessionRestoredLocally)
                    }

                    processIncomingMessageQueue()
                    processOutgoingMessageQueue()

                    // set(true) as late as possible so other methods do not start to function before [openSocket] is finalized
                    // but [subscribeToAllMessages] below checks this
                    initialized.set(true)

                    socket.open(
                            url = initialState.connectionParams.bridgeUrl,
                            connectionListener = { connectionState ->
                                when (connectionState) {
                                    SocketConnectionState.Connecting -> {
                                        callback(SocketCallback.SocketConnecting)
                                    }
                                    SocketConnectionState.Connected -> {
                                        callback(SocketCallback.SocketConnected)
                                        subscribeToAllMessages()
                                    }
                                    SocketConnectionState.Disconnected -> {
                                        callback(SocketCallback.SocketDisconnected)
                                        // if disconnected & reconnected, you are not subscribed to topic, myPeerId anymore. resubscribe
                                        subscribedToAllMessages.set(false)
                                    }
                                    SocketConnectionState.Closed -> {
                                        callback(SocketCallback.SocketClosed)
                                        //closeSocketAndSession(notifyPeer = false)
                                    }
                                }
                            },
                            errorListener = { failure, isFatal ->
                                failureCallback(failure)
                                if (isFatal) {
                                    // deadlock (because of 'socketConnectionLock') doesn't happen
                                    close(deleteLocal = false,
                                          deleteRemote = false)
                                }
                            },
                            messageListener = { socketMessage ->
                                incomingMessageQueue.tryEmit(socketMessage)
                            }
                    )
                } catch (error: Exception) {
                    logger.error(LogTag, error.stackTraceToString())
                } finally {
                    socketConnectionLock.unlock()
                    onOpen?.invoke()
                }
            } else {
                socketConnectionLock.unlock()
                onOpen?.invoke()
            }
        }.start()
    }

    override fun getInitialSessionState()
            : InitialSessionState? {
        if (!initialized.get()) {
            return null
        }
        return initialState
    }

    override fun disconnectSocket(onRequested: (() -> Unit)?) {
        if (!initialized.get()) {
            logger.error(LogTag, "#disconnect(): !initialized")
            return
        }

        Thread {
            socketConnectionLock.lock()
            // double-check. multiple threads can pass initial check and wait in queue for lock
            if (initialized.get()) {
                logger.info(LogTag, "#disconnect()")
                socket.disconnect()
                socketConnectionLock.unlock()
                onRequested?.invoke()
            } else {
                socketConnectionLock.unlock()
                onRequested?.invoke()
            }
        }.start()
    }

    override fun reconnectSocket(onRequested: (() -> Unit)?) {
        if (!initialized.get()) {
            logger.error(LogTag, "#reconnectSocket(): !initialized")
            return
        }

        Thread {
            socketConnectionLock.lock()
            // double-check. multiple threads can pass initial check and wait in queue for lock
            if (initialized.get()) {
                logger.info(LogTag, "#reconnectSocket()")
                socket.reconnect()
                socketConnectionLock.unlock()
                onRequested?.invoke()
            } else {
                socketConnectionLock.unlock()
                onRequested?.invoke()
            }
        }.start()
    }

    /**
     * Blocking version of `updateSession(chainId = null, accounts = null, approved = false)`. Only called from [close]
     */
    private fun deleteSessionInternal() {
        val chainId: Int? = null
        val accounts: List<String>? = null
        val approved = false

        runBlocking {
            if (!initialized.get()) {
                logger.error(LogTag, "#deleteSessionInternal(): !initialized")
                return@runBlocking
            }
            if (!sessionApproved.get()) {
                logger.warning(LogTag, "#deleteSessionInternal(): session wasn't approved before")
                failureCallback(Failure(type = FailureType.SessionError,
                                        message = "#deleteSessionInternal(): session wasn't approved before"))
                return@runBlocking
            }
            val currentSessionState = sessionState?.copy()
            if (currentSessionState == null) {
                // [sessionState] was updated on sessionRequest, it shouldn't be null
                logger.warning(LogTag, "#deleteSessionInternal(): sessionState is null")
                return@runBlocking
            }

            logger.info(LogTag, "#deleteSessionInternal()")

            val model = SessionUpdate(approved = approved,
                                      chainId = chainId,
                                      accounts = accounts)
            val messageId = generateMessageId()
            val payload = JsonRpcRequest(id = messageId,
                                         method = SessionRpcMethod.Update,
                                         params = listOf(model))

            val payloadAsJson = serializePayload(payload,
                                                 getRequestType(SessionUpdate::class.java))
                                ?: return@runBlocking

            val encryptedPayload = encryptPayload(payloadAsJson)
                                   ?: return@runBlocking

            val message = SocketMessage(topic = currentSessionState.remotePeerId,
                                        type = SocketMessageType.Pub,
                                        payload = encryptedPayload)
            publish(message,
                    queueIfDisconnected = true)

            // update [sessionState] of Wallet. remotePeerId, remotePeerMeta was updated on sessionRequest
            sessionState = currentSessionState.copy(
                    chainId = chainId,
                    accounts = accounts,

                    updatedAt = System.currentTimeMillis()
            )
            sessionApproved.set(approved)

            if (approved) {
                if (!isDApp) {
                    sessionStore.persist(initialState.connectionParams.topic,
                                         sessionState!!)

                    // notify wallet
                    callback(SessionCallback.SessionUpdated(
                            messageId = messageId,
                            chainId = chainId,
                            accounts = accounts
                    ))
                }
            } else {
                // dApp || wallet
                callback(SessionCallback.SessionDeleted(byMe = true))
            }
        }
    }

    override fun close(deleteLocal: Boolean,
                       deleteRemote: Boolean,
                       delayMs: Long,
                       onClosed: (() -> Unit)?) {
        if (!initialized.get()) {
            onClosed?.invoke()
            return
        }

        Thread {
            socketConnectionLock.lock()
            // double-check. multiple threads can pass initial check and wait in queue for lock
            if (initialized.get()) {
                logger.info(LogTag, "#closeSocketAndSession()")

                // delay is important, otherwise Socket do not get a chance to send the last message
                val delay = max(500L, delayMs)
                try {
                    if (!deleteLocal && !deleteRemote) {
                        callback(SessionCallback.SessionClosedLocally)
                    }

                    // send updateSession event with approved=false
                    // don't check [socket].isConnected, [close] can be called right after [openSocket]
                    if (deleteRemote && sessionApproved.get()) {
                        deleteSessionInternal()
                    }
                    // updateSession(false), rejectSession, ... messages are just queued in [Socket]
                    var totalWaited = 0
                    while (outgoingMessageCount.get() != 0 && totalWaited < delay) {
                        Thread.sleep(50)
                        totalWaited += 50
                    }
                    // wait little more (messages are in queue of [Socket] now)
                    Thread.sleep(500)

                    // set(false) asap, because other method calls should return immediately if [close] is in progress
                    // but [deleteSessionInternal] & [encryptPayloadAndPublish] uses this
                    initialized.set(false)

                    // stop actions
                    socket.close() // closed callback is received immediately
                    incomingMessageQueue.resetReplayCache()
                    outgoingMessageQueue.resetReplayCache()
                    coroutineScope.cancel()

                    // reset all
                    sessionState = null
                    sessionCallback = null
                    subscribedToAllMessages.set(false)
                    sessionRequestId = null
                    sessionApproved.set(false)
                    outgoingMessageCount.set(0)
                    messageMethodMap.clear()
                    requestTypeCache.clear()
                    responseTypeCache.clear()

                    // delete sessionState lastly, as above operations might persist updated state
                    if (deleteLocal) {
                        runBlocking {
                            sessionStore.remove(initialState.connectionParams.topic)
                        }
                    }

                } catch (error: Exception) {
                    logger.error(LogTag, error.stackTraceToString())
                } finally {
                    socketConnectionLock.unlock()
                    onClosed?.invoke()
                }
            } else {
                socketConnectionLock.unlock()
                onClosed?.invoke()
            }
        }.start()
    }
    // endregion

    // region Incoming Messages
    /**
     * Subscribes to myPeerId, topic
     * - Idempotent
     * - [Socket] queues outgoing messages if not connected
     */
    private fun subscribeToAllMessages() {
        if (!initialized.get()) {
            logger.warning(LogTag, "#subscribeToMessages(): !initialized")
            return
        }
        if (subscribedToAllMessages.get()) {
            return
        }

        // topic
        if (!isDApp) {
            // subscribe always: dApp sees as disconnected and can sen sessionRequest,
            //    wallet needs to listen even wallet sees as connected
            val message = SocketMessage(topic = initialState.connectionParams.topic,
                                        type = SocketMessageType.Sub,
                                        payload = "")
            logger.info(LogTag, "subscribeToTopic(${initialState.connectionParams.topic}): $message")
            socket.publish(message,
                           queueIfDisconnected = true)
        }

        // myPeerId
        val message2 = SocketMessage(topic = initialState.myPeerId,
                                     type = SocketMessageType.Sub,
                                     payload = "")
        logger.info(LogTag, "subscribeToMyId(${initialState.myPeerId}): $message2")
        socket.publish(message2,
                       queueIfDisconnected = true)

        subscribedToAllMessages.set(true)

        callback(SessionCallback.SubscribedToMessages)
    }

    /**
     * If there is unexpected error, [reportInvalidSocketMessage] is called.
     * - [handleJsonRpcRequest], [handleJsonRpcResponse], [handleJsonRpcError], [handleCustom] are called depending on message
     */
    private fun processIncomingMessageQueue() {
        logger.info(LogTag, "#processIncomingMessageQueue()")

        incomingMessageQueue
                .onEach { socketMessage ->
                    if (!initialized.get()) {
                        return@onEach
                    }

                    // bridgeServer shouldn't have sent this message
                    if (!socketMessage.topic.equals(initialState.connectionParams.topic, ignoreCase = true)
                        && !socketMessage.topic.equals(initialState.myPeerId, ignoreCase = true)) {
                        logger.error(LogTag, "#processIncomingMessageQueue(): topics not equal, " +
                                             "${socketMessage.topic}!=${initialState.connectionParams.topic} " +
                                             "|| ${socketMessage.topic}!=${initialState.myPeerId}")
                        return@onEach
                    }
                    if (socketMessage.type == SocketMessageType.Sub) {
                        logger.error(LogTag, "#processIncomingMessageQueue(): messageType is sub!")
                        return@onEach
                    }

                    // decrypt payload
                    val decryptedPayload: String? = decryptPayload(socketMessage)
                    if (decryptedPayload.isNullOrBlank()) {
                        reportInvalidSocketMessage(messageId = null,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestObject,
                                                                    "Can't decrypt payload"))
                        return@onEach
                    }

                    // notify
                    callback(SocketCallback.SocketMessage(
                            beforeHandling = true,
                            decryptedPayload,
                            messageId = null
                    ))

                    // deserialize into Map
                    val deserializedPayload: Map<String, Any?>? = deserializePayloadAsMap(decryptedPayload)
                    if (deserializedPayload == null) {
                        reportInvalidSocketMessage(messageId = null,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestObject,
                                                                    "Can't deserialize payload as map"))
                        return@onEach
                    }

                    // all payload models must have [jsonRpc, id] fields
                    if (deserializedPayload["jsonrpc"] == null || deserializedPayload["id"] == null) {
                        logger.error(LogTag, "deserializedPayload doesn't have 'jsonrpc' " +
                                             "or 'id' fields: $deserializedPayload")
                        failureCallback(Failure(type = FailureType.DeserializeSessionMessage))
                        reportInvalidSocketMessage(messageId = null,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestObject,
                                                                    "Field 'jsonrpc'/'id' doesn't exist"))
                        return@onEach
                    }
                    val messageId: Long? = (deserializedPayload["id"] as? Double)?.toLong()
                    if (messageId == null) {
                        logger.error(LogTag, "deserializedPayload doesn't have valid id:${deserializedPayload["id"]}")
                        failureCallback(Failure(type = FailureType.DeserializeSessionMessage))
                        reportInvalidSocketMessage(messageId,
                                                   JsonRpcErrorData(RpcErrorCode.InvalidRequestObject,
                                                                    "Field 'id' isn't valid Long"))
                        return@onEach
                    }

                    // handle payload depending on which fields exist
                    if (deserializedPayload["method"] != null && deserializedPayload["params"] != null) {
                        handleJsonRpcRequest(decryptedPayload,
                                             deserializedPayload["method"] as? String,
                                             messageId)
                    } else if (deserializedPayload["result"] != null) {
                        handleJsonRpcResponse(decryptedPayload,
                                              messageId)
                    } else if (deserializedPayload["error"] != null) {
                        handleJsonRpcError(decryptedPayload,
                                           messageId)
                    } else {
                        handleCustom(decryptedPayload,
                                     messageId)
                    }

                    // notify
                    if (initialized.get()) {
                        callback(SocketCallback.SocketMessage(
                                beforeHandling = false,
                                decryptedPayload,
                                messageId = messageId
                        ))
                    }
                }
                .catch {
                    logger.error(LogTag, "#processIncomingMessageQueue(): ${it.stackTraceToString()}")
                    failureCallback(Failure(type = FailureType.SessionIncomeFlow,
                                            cause = it))
                    close(deleteLocal = false,
                          deleteRemote = false)
                }
                .launchIn(coroutineScope)
    }

    abstract suspend fun handleJsonRpcRequest(decryptedPayload: String,
                                              method: String?,
                                              messageId: Long)

    abstract suspend fun handleJsonRpcResponse(decryptedPayload: String,
                                               messageId: Long)

    abstract suspend fun handleJsonRpcError(decryptedPayload: String,
                                            messageId: Long)

    abstract suspend fun handleCustom(decryptedPayload: String,
                                      messageId: Long)

    /** Decrypts and validates [SocketMessage.payload] */
    private suspend fun decryptPayload(socketMessage: SocketMessage)
            : String? {
        logger.debug(LogTag, "#decryptIncomingPayload()")

        return withContext(dispatcherProvider.computation()) {
            val encryptedPayload: EncryptedPayload = try {
                jsonAdapter.fromJson(socketMessage.payload, EncryptedPayload::class.java)!!
            } catch (error: Exception) {
                logger.error(LogTag, "SocketMessage.payload can't be deserialized: " +
                                     "${socketMessage.payload}: ${error.stackTraceToString()}")
                failureCallback(error.toFailure(type = FailureType.DeserializeSessionMessage))
                return@withContext null
            }

            try {
                encryptedPayload.validate()
            } catch (error: Exception) {
                logger.error(LogTag, "SocketMessage.payload fields have invalid format: ${error.stackTraceToString()}")
                failureCallback(error.toFailure(type = FailureType.DeserializeSessionMessage))
                return@withContext null
            }

            val key = initialState.connectionParams.symmetricKey
            val decrypted: ByteArray = try {
                Cryptography.decrypt(encryptedPayload,
                                     key.hexToByteArray())
            } catch (error: Exception) {
                logger.error(LogTag, "encryptedPayload can't be decrypted: ${error.stackTraceToString()}")
                failureCallback(error.toFailure(type = FailureType.Decryption))
                return@withContext null
            }

            return@withContext String(decrypted, Charsets.UTF_8)
        }
    }

    /**
     * Calls [reportInvalidSocketMessage] internally if [ignoreError] is false
     */
    protected suspend fun <T> deserializePayload(decryptedPayload: String,
                                                 messageId: Long,
                                                 type: Type,
                                                 ignoreError: Boolean = false)
            : T? {
        logger.debug(LogTag, "#deserializePayloadAsJsonRpc()")

        return try {
            withContext(dispatcherProvider.computation()) {
                jsonAdapter.fromJson(decryptedPayload, type)!!
            }
        } catch (error: Exception) {
            if (!ignoreError) {
                failureCallback(error.toFailure(type = FailureType.DeserializeSessionMessage))
                logger.error(LogTag,
                             "deserializePayloadAsJsonRpc($messageId): $decryptedPayload. \n${error.stackTraceToString()}")
                reportInvalidSocketMessage(messageId,
                                           JsonRpcErrorData(RpcErrorCode.InvalidRequestObject,
                                                            "Can't deserialize payload")
                )
            }
            null
        }
    }

    /**
     * @return Map is guaranteed to be not empty
     */
    private suspend fun deserializePayloadAsMap(payload: String)
            : Map<String, Any?>? {
        logger.debug(LogTag, "#deserializePayloadAsMap()")

        return withContext(dispatcherProvider.computation()) {
            val jsonItems: Map<String, Any?> = try {
                jsonAdapter.fromJson(payload, mapType)!!
            } catch (error: Exception) {
                logger.error(LogTag, "Decrypted payload can't be deserialized: " +
                                     "${payload}: ${error.stackTraceToString()}")
                failureCallback(error.toFailure(type = FailureType.DeserializeSessionMessage))
                return@withContext null
            }
            if (jsonItems.isEmpty()) {
                logger.error(LogTag, "Decrypted payload is empty map!")
                failureCallback(Failure(type = FailureType.DeserializeSessionMessage))
                return@withContext null
            }

            return@withContext jsonItems
        }
    }
    // endregion

    // region Outgoing Messages
    fun generateMessageId(): Long {
        // smaller than 2^53-1 (=max js number=900 719 925 474 0991), but same digit count
        return System.currentTimeMillis() * 1000 + Random().nextInt(999)
    }

    protected suspend fun serializePayload(payload: Any,
                                           type: Type)
            : String? {
        logger.info(LogTag, "#serializePayloadAsJson:")

        return withContext(dispatcherProvider.computation()) {
            try {
                val json = jsonAdapter.toJson(payload, type)!!
                logger.info(LogTag, "\t" + json)
                return@withContext json
            } catch (error: Exception) {
                logger.error(LogTag, "Can't serialize(${payload}): ${error.stackTraceToString()}")
                failureCallback(error.toFailure(type = FailureType.SerializeSessionMessage))
                return@withContext null
            }
        }
    }

    protected suspend fun encryptPayload(payload: String)
            : String? {
        logger.debug(LogTag, "#encryptOutgoingPayload()")

        return withContext(dispatcherProvider.computation()) {
            // encrypt
            val key = initialState.connectionParams.symmetricKey
            val encryptedPayload: EncryptedPayload = try {
                Cryptography.encrypt(payload.toByteArray(Charsets.UTF_8),
                                     key.hexToByteArray())
            } catch (error: Exception) {
                logger.error(LogTag, "payload can't be encrypted: ${error.stackTraceToString()}")
                failureCallback(error.toFailure(type = FailureType.Encryption))
                return@withContext null
            }

            // serialize
            val encryptedPayloadAsJson: String = try {
                jsonAdapter.toJson(encryptedPayload, EncryptedPayload::class.java)!!
            } catch (error: Exception) {
                logger.error(LogTag, "encryptedPayload can't be serialized: " +
                                     "${encryptedPayload}: ${error.stackTraceToString()}")
                failureCallback(error.toFailure(type = FailureType.SerializeSessionMessage))
                return@withContext null
            }

            return@withContext encryptedPayloadAsJson
        }
    }

    suspend fun encryptPayloadAndPublish(messageId: Long,
                                         payload: Any,
                                         type: Type,
                                         queueIfDisconnected: Boolean)
            : Long? {
        if (!initialized.get()) {
            logger.error(LogTag, "#encryptPayloadAndPublish(): !initialized")
            return null
        }
        if (!sessionApproved.get()) {
            logger.error(LogTag, "#encryptPayloadAndPublish(): !sessionApproved")
            return null
        }
        val remotePeerId = sessionState?.remotePeerId
        if (remotePeerId == null) {
            logger.error(LogTag, "#encryptPayloadAndPublish(): remotePeerId is null")
            failureCallback(Failure(type = FailureType.SessionError,
                                    message = "#encryptPayloadAndPublish(): remotePeerId is null"))
            return null
        }

        val payloadAsJson = serializePayload(payload, type)
                            ?: return null

        val encryptedPayload = encryptPayload(payloadAsJson)
                               ?: return null

        val message = SocketMessage(topic = remotePeerId,
                                    type = SocketMessageType.Pub,
                                    payload = encryptedPayload)

        publish(message,
                queueIfDisconnected)

        return messageId
    }

    /**
     * All [SocketMessageType.Pub] messages should be published after [SocketMessageType.Sub] messages.
     * Otherwise they are not delivered to the peer.
     */
    protected fun publish(message: SocketMessage,
                          queueIfDisconnected: Boolean) {
        outgoingMessageCount.incrementAndGet()
        outgoingMessageQueue.tryEmit(Pair(message, queueIfDisconnected))
    }

    private fun processOutgoingMessageQueue() {
        logger.info(LogTag, "#processOutgoingMessageQueue()")

        outgoingMessageQueue
                .onEach { (socketMessage, queueIfDisconnected) ->
                    while (!subscribedToAllMessages.get()) {
                        delay(50)
                    }
                    socket.publish(socketMessage, queueIfDisconnected)
                    outgoingMessageCount.decrementAndGet()
                }
                .catch {
                    logger.error(LogTag, "#processOutgoingMessageQueue(): ${it.stackTraceToString()}")
                    failureCallback(Failure(type = FailureType.SessionOutgoingFlow,
                                            cause = it))
                    close(deleteLocal = false,
                          deleteRemote = false)
                }
                .launchIn(coroutineScope)
    }

    /** Sends [JsonRpcError] if remotePeerId exists */
    protected suspend fun reportInvalidSocketMessage(messageId: Long? = null,
                                                     errorData: JsonRpcErrorData? = null) {
        val remotePeerId = sessionState?.remotePeerId ?: return

        val encryptedPayload: String? = withContext(dispatcherProvider.computation()) {
            val payload = JsonRpcError(
                    id = messageId ?: generateMessageId(),
                    error = errorData ?: JsonRpcErrorData(code = RpcErrorCode.InvalidRequestParams,
                                                          "Invalid SocketMessage")
            )
            val payloadAsJson = try {
                jsonAdapter.toJson(payload, JsonRpcError::class.java)!!
            } catch (error: Exception) {
                logger.error(LogTag, "JsonRpcError can't be serialized: ${payload}: ${error.stackTraceToString()}")
                failureCallback(error.toFailure(type = FailureType.SerializeSessionMessage))
                return@withContext null
            }
            encryptPayload(payloadAsJson) ?: return@withContext null
        }
        if (encryptedPayload.isNullOrBlank()) {
            return
        }

        val message = SocketMessage(topic = remotePeerId,
                                    type = SocketMessageType.Pub,
                                    payload = encryptedPayload)

        publish(message,
                queueIfDisconnected = true)
    }
    // endregion

}