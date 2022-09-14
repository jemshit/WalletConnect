/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.retry.ExponentialBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import walletconnect.DAppManager
import walletconnect.WalletManager
import walletconnect.adapter.gson.type_adapter.JsonRpcMethodTypeAdapter
import walletconnect.adapter.gson.type_adapter.SocketMessageTypeAdapter
import walletconnect.adapter.moshi.MoshiAdapter
import walletconnect.core.DApp
import walletconnect.core.Wallet
import walletconnect.core.adapter.JsonAdapter
import walletconnect.core.session.SessionLifecycle
import walletconnect.core.session.callback.CallbackData
import walletconnect.core.session.callback.SessionCallback
import walletconnect.core.session.callback.SocketCallback
import walletconnect.core.session.callback.simplifiedName
import walletconnect.core.session.model.InitialSessionState
import walletconnect.core.session.model.json_rpc.JsonRpcMethod
import walletconnect.core.session_state.SessionStore
import walletconnect.core.session_state.model.ConnectionParams
import walletconnect.core.session_state.model.toInitialSessionState
import walletconnect.core.socket.Socket
import walletconnect.core.socket.model.SocketMessageType
import walletconnect.core.util.DispatcherProvider
import walletconnect.core.util.Logger
import walletconnect.sample.impl.AndroidDispatcherProvider
import walletconnect.sample.impl.TextViewLogger
import walletconnect.sample.store.SessionEvent
import walletconnect.socket.scarlet.FlowStreamAdapter
import walletconnect.socket.scarlet.SocketManager
import walletconnect.socket.scarlet.SocketService
import walletconnect.store.file.FileSessionStore
import java.io.File
import java.util.concurrent.TimeUnit

abstract class BaseFragment : Fragment() {

    protected lateinit var logger: Logger
    protected val dispatcherProvider: DispatcherProvider = AndroidDispatcherProvider()

    protected abstract val fragmentTag: String
    protected lateinit var sessionLifecycle: SessionLifecycle
    protected abstract val initialSessionState: InitialSessionState
    protected val connectionParams by lazy {
        ConnectionParams(
                topic = viewModel.topic,
                version = "1",
                bridgeUrl = viewModel.bridgeUrl,
                symmetricKey = viewModel.symmetricKey
        )
    }
    protected var approvedAddress: String? = null
    protected var approvedChainId: Int? = null
    protected var latestState: String? = null
    protected var lastState2: String? = null
    protected var lastState3: String? = null
    protected var lastState4: String? = null

    protected val viewModel: OneViewModel by activityViewModels()

    // region lifecycle
    override fun onViewCreated(view: View,
                               savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger = TextViewLogger(viewLifecycleOwner.lifecycle.coroutineScope,
                                consoleTextView(),
                                ::onConsoleUpdate)

        observeSessionListEvents()
    }

    abstract fun consoleTextView(): TextView

    override fun onDestroyView() {
        super.onDestroyView()
        sessionLifecycle.closeAsync(deleteLocal = false,
                                    deleteRemote = false)
    }
    // endregion

    protected abstract fun onConsoleUpdate()

    // region Session callbacks
    protected fun updateLatestState(state: CallbackData)
            : String? {
        if (state is SessionCallback.LocalSessionStateUpdated) {
            return null
        }
        if (state is SocketCallback.SocketMessage && !state.beforeHandling) {
            return null
        }

        lastState4 = lastState3
        lastState3 = lastState2
        lastState2 = latestState
        latestState = state.simplifiedName()

        return "$latestState <- $lastState2 <- $lastState3 <- $lastState4"
    }

    protected abstract fun onSessionCallback(callbackData: CallbackData)

    private fun observeSessionListEvents() {
        viewModel.sessionEvents
                .filter { it.tag == fragmentTag }
                .onEach { event ->
                    val currentInitialSessionState = sessionLifecycle.getInitialSessionState()
                    val toDelete = event is SessionEvent.Delete

                    if (currentInitialSessionState == null) {
                        // closed
                        sessionLifecycle.openSocket(
                                event.sessionState.toInitialSessionState(),
                                ::onSessionCallback
                        )
                        if (toDelete) {
                            sessionLifecycle.close(deleteLocal = true,
                                                   deleteRemote = true,
                                                   delayMs = 2_000L)
                        }
                    } else {
                        // open
                        if (currentInitialSessionState == event.sessionState.toInitialSessionState()) {
                            // same session
                            if (toDelete) {
                                sessionLifecycle.close(deleteLocal = true,
                                                       deleteRemote = true,
                                                       delayMs = 2_000L)
                            }
                        } else {
                            // different session
                            sessionLifecycle.close(
                                    deleteLocal = false,
                                    deleteRemote = false,
                                    delayMs = 500L
                            )
                            sessionLifecycle.openSocket(
                                    event.sessionState.toInitialSessionState(),
                                    ::onSessionCallback
                            )
                            if (toDelete) {
                                sessionLifecycle.close(deleteLocal = true,
                                                       deleteRemote = true,
                                                       delayMs = 2_000L)
                            }
                        }
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycle.coroutineScope)
    }
    // endregion

    // region Factory
    private fun createSocketService(url: String,
                                    lifecycleRegistry: LifecycleRegistry)
            : SocketService {

        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val okHttpClient = OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                // "https://bridge.walletconnect.org" -> i think BridgeServer responds with "missing or invalid socket data"
                // "https://safe-walletconnect.gnosis.io" -> ping works fine
                .pingInterval(4, TimeUnit.SECONDS)
                .addNetworkInterceptor(interceptor)
                .cache(null)
                .build()

        val webSocketFactory = okHttpClient.newWebSocketFactory(url)

        val gson = GsonBuilder()
                .registerTypeAdapter(SocketMessageType::class.java, SocketMessageTypeAdapter())
                .registerTypeAdapter(JsonRpcMethod::class.java, JsonRpcMethodTypeAdapter())
                .create()

        val scarlet = Scarlet.Builder()
                .webSocketFactory(webSocketFactory)
                .addMessageAdapterFactory(GsonMessageAdapter.Factory(gson))
                .addStreamAdapterFactory(FlowStreamAdapter.Factory)
                .backoffStrategy(ExponentialBackoffStrategy(initialDurationMillis = 1_000L,
                                                            maxDurationMillis = 8_000L))
                .lifecycle(lifecycleRegistry)
                .build()

        return scarlet.create(SocketService::class.java)
    }

    private fun createSocket()
            : Socket {
        val gson = GsonBuilder()
                .registerTypeAdapter(SocketMessageType::class.java, SocketMessageTypeAdapter())
                .registerTypeAdapter(JsonRpcMethod::class.java, JsonRpcMethodTypeAdapter())
                .create()

        return SocketManager(
                socketServiceFactory = { url, lifecycleRegistry -> createSocketService(url, lifecycleRegistry) },
                gson,
                dispatcherProvider,
                logger
        )
    }

    private fun createJsonAdapter()
            : JsonAdapter {
        /*val gson = GsonBuilder()
                .registerTypeAdapter(SocketMessageType::class.java, SocketMessageTypeAdapter())
                .registerTypeAdapter(JsonRpcMethod::class.java, JsonRpcMethodTypeAdapter())
                .serializeNulls()
                .create()

        return GsonAdapter(gson)*/

        val moshi = Moshi.Builder()
                .add(walletconnect.adapter.moshi.type_adapter.SocketMessageTypeAdapter())
                .add(walletconnect.adapter.moshi.type_adapter.JsonRpcMethodTypeAdapter())
                .addLast(KotlinJsonAdapterFactory())
                .build()
        return MoshiAdapter(moshi)
    }

    private fun createSessionStore(name: String)
            : SessionStore {
        //val sharedPrefs = requireContext().applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

        /*return SharedPrefsSessionStore(
                sharedPrefs,
                dispatcherProvider,
                logger
        )*/
        return FileSessionStore(
                File(requireContext().filesDir, "$name.json"),
                dispatcherProvider,
                logger
        )
    }

    protected fun createDApp(sessionStoreName: String)
            : DApp {
        return DAppManager(
                socket = createSocket(),
                sessionStore = createSessionStore(sessionStoreName),
                jsonAdapter = createJsonAdapter(),
                dispatcherProvider,
                logger
        )
    }

    protected fun createWallet(sessionStoreName: String)
            : Wallet {
        return WalletManager(
                createSocket(),
                createSessionStore(sessionStoreName),
                createJsonAdapter(),
                dispatcherProvider,
                logger
        )
    }
    // endregion

}