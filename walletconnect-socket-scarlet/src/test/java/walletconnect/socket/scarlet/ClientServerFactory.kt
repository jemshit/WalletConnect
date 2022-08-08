/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.socket.scarlet

import com.google.gson.Gson
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.retry.ExponentialBackoffStrategy
import com.tinder.scarlet.websocket.mockwebserver.newWebSocketFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import walletconnect.core.socket.Socket
import walletconnect.core.socket.model.SocketMessage
import walletconnect.core.util.DispatcherProvider
import walletconnect.core.util.Logger
import java.util.concurrent.TimeUnit

abstract class ClientServerFactory {

    protected abstract val logger: Logger
    protected abstract val dispatcherProvider: DispatcherProvider

    protected lateinit var mockWebServer: MockWebServer
    protected lateinit var serverSocket: Socket
    protected lateinit var clientSocket: Socket

    private fun createServer(url: String,
                             lifecycleRegistry: LifecycleRegistry)
            : SocketService {
        // create new for each test!
        mockWebServer = MockWebServer()

        val scarlet = Scarlet.Builder()
                .webSocketFactory(mockWebServer.newWebSocketFactory())
                .addMessageAdapterFactory(GsonMessageAdapter.Factory())
                .addStreamAdapterFactory(FlowStreamAdapter.Factory)
                .backoffStrategy(ExponentialBackoffStrategy(initialDurationMillis = 1_000L,
                                                            maxDurationMillis = 8_000L))
                .lifecycle(lifecycleRegistry)
                .build()
        return scarlet.create(SocketService::class.java)
    }

    private fun createClient(url: String,
                             lifecycleRegistry: LifecycleRegistry)
            : SocketService {
        val okHttpClient = OkHttpClient.Builder()
                .writeTimeout(1000, TimeUnit.MILLISECONDS)
                .readTimeout(1000, TimeUnit.MILLISECONDS)
                .build()
        val webSocketFactory = okHttpClient.newWebSocketFactory(mockWebServer.url("/").toString())

        val scarlet = Scarlet.Builder()
                .webSocketFactory(webSocketFactory)
                .addMessageAdapterFactory(GsonMessageAdapter.Factory())
                .addStreamAdapterFactory(FlowStreamAdapter.Factory)
                .backoffStrategy(ExponentialBackoffStrategy(initialDurationMillis = 1_000L,
                                                            maxDurationMillis = 4_000L))
                .lifecycle(lifecycleRegistry)
                .build()
        return scarlet.create(SocketService::class.java)
    }

    private fun createSockets() {
        serverSocket = SocketManager(
                ::createServer,
                Gson(),
                dispatcherProvider,
                logger,
                logTagSuffix = "[Wallet]"
        )
        clientSocket = SocketManager(
                ::createClient,
                Gson(),
                dispatcherProvider,
                logger,
                logTagSuffix = "[DApp]"
        )
    }

    protected suspend fun blockUntilIsConnected(timeout: Long = 10_000) {
        // wait for real, don't use TestDispatcher
        withContext(Dispatchers.Default) {
            withTimeout(timeout) {
                val job1 = launch {
                    //println("before server:"+Date().toString())
                    while (!serverSocket.isConnected()) {
                        delay(50)
                    }
                    //println("after server:"+Date().toString())
                }
                val job2 = launch {
                    //println("before client:"+Date().toString())
                    while (!clientSocket.isConnected()) {
                        delay(50)
                    }
                    //println("after client:"+Date().toString())
                }
                job1.join()
                job2.join()
            }
        }
    }

    protected suspend fun blockUntilClientNotConnected(timeout: Long = 10_000) {
        // wait for real, don't use TestDispatcher
        withContext(Dispatchers.Default) {
            withTimeout(timeout) {
                while (clientSocket.isConnected()) {
                    delay(50)
                }
            }
        }
    }

    protected suspend fun blockUntilServerNotConnected(timeout: Long = 10_000) {
        // wait for real, don't use TestDispatcher
        withContext(Dispatchers.Default) {
            withTimeout(timeout) {
                while (serverSocket.isConnected()) {
                    delay(50)
                }
            }
        }
    }

    protected suspend fun waitUntilReceived(count: Int,
                                            items: List<SocketMessage>,
                                            timeout: Long = 10_000,
                                            assertions: () -> Unit) {
        // wait for real, don't use TestDispatcher
        withContext(Dispatchers.Default) {
            withTimeout(timeout) {
                while (items.size != count) {
                    delay(50)
                }
                assertions()
            }
        }
    }

    open fun before() {
        createSockets()
    }

    open fun after() {
        serverSocket.close()
        clientSocket.close()
        mockWebServer.shutdown()
    }

}