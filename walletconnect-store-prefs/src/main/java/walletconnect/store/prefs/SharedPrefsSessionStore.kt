/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.store.prefs

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import walletconnect.core.session_state.SessionStore
import walletconnect.core.session_state.model.SessionState
import walletconnect.core.util.DispatcherProvider
import walletconnect.core.util.Logger
import walletconnect.core.util.logger_impl.EmptyLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class SharedPrefsSessionStore(private val sharedPrefs: SharedPreferences,
                              private val dispatcherProvider: DispatcherProvider,
                              private val logger: Logger = EmptyLogger)
    : SessionStore {

    private val LogTag = "SharedPrefSessionStore"
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcherProvider.io() + SupervisorJob())

    private val gson = Gson()
    private val cachedStates: MutableMap<String, SessionState> = ConcurrentHashMap()
    /** Only set to 'true' after restore succeeds. All methods suspend the caller until this becomes 'true' */
    private val cacheReady = AtomicBoolean(false)
    /** Only single write is allowed, we iterate through cachedStates. Used in [persist], [remove], [removeAll] */
    private val writeMutex = Mutex()
    private val statesFlow: MutableSharedFlow<Set<SessionState>> = MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 1,
            BufferOverflow.DROP_OLDEST
    )

    init {
        logger.debug(LogTag, "----init----")
        restoreFromPrefsAsync()
    }

    @Suppress("UNCHECKED_CAST")
    /** Sets [cacheReady] to true when done */
    private fun restoreFromPrefsAsync() {
        logger.debug(LogTag, "-> restoreFromPrefsAsync()")
        coroutineScope.launch {
            // do not modify 'storeContent' (SharedPreferences doc says so)
            val storeContent: Map<String, String> = try {
                sharedPrefs.all as? Map<String, String>
            } catch (error: Exception) {
                logger.error(LogTag, error.stackTraceToString())
                mapOf()
            } ?: mapOf()

            if (storeContent.isNotEmpty()) {
                logger.debug(LogTag, "  restoreFromPrefsAsync() prefs.notBlank")
                try {
                    val allState: Map<String, SessionState> = storeContent.mapValues {
                        withContext(dispatcherProvider.computation()) {
                            gson.fromJson(it.value, SessionState::class.java)
                        }
                    }
                    cachedStates.putAll(allState)
                } catch (error: Exception) {
                    logger.error(LogTag, error.stackTraceToString())
                }
            }

            // notify hot flow
            statesFlow.tryEmit(cachedStates.values.map { it.copy() }.toSet())

            // update flag
            logger.debug(LogTag, "<- restoreFromPrefsAsync(${cachedStates.size}) cacheReady.set(true)")
            cacheReady.set(true)
        }
    }

    override suspend fun get(topic: String)
            : SessionState? {
        if (cacheReady.get()) {
            return cachedStates[topic]
        }

        withContext(dispatcherProvider.io()) {
            // restoreFromPrefsAsync() must be in progress
            logger.debug(LogTag, "-> get($topic) waiting")
            while (!cacheReady.get()) {
                delay(50L)
            }
        }

        logger.debug(LogTag, "<- get($topic) wait.ended")
        return cachedStates[topic]
    }

    override suspend fun getAll()
            : List<SessionState>? {
        if (cacheReady.get()) {
            return cachedStates.values.toList()
        }

        withContext(dispatcherProvider.io()) {
            // restoreFromPrefsAsync() must be in progress
            logger.debug(LogTag, "-> getAll() waiting")
            while (!cacheReady.get()) {
                delay(50L)
            }
        }

        logger.debug(LogTag, "<- getAll(${cachedStates.size}) wait.ended")
        return cachedStates.values.toList()
    }

    override fun getAllAsFlow()
            : Flow<Set<SessionState>> {
        logger.debug(LogTag, "<- getAllAsFlow()")

        return statesFlow
                .asSharedFlow()
                .distinctUntilChanged { old, new -> old.equals(new) }
    }

    @SuppressLint("ApplySharedPref")
    override suspend fun persist(topic: String,
                                 state: SessionState) {
        withContext(dispatcherProvider.io()) {
            // restoreFromPrefsAsync() must be in progress
            while (!cacheReady.get()) {
                logger.debug(LogTag, "-> persist($topic) waiting")
                delay(50L)
            }
        }

        writeMutex.withLock {
            try {
                val stateJson = withContext(dispatcherProvider.computation()) {
                    gson.toJson(state)
                }
                // update cache if Json is valid
                cachedStates[topic] = state
                withContext(dispatcherProvider.io()) {
                    sharedPrefs.edit().putString(topic, stateJson).commit()
                }
                // notify hot flow
                statesFlow.tryEmit(cachedStates.values.map { it.copy() }.toSet())
            } catch (error: Exception) {
                logger.error(LogTag, error.stackTraceToString())
            }
            logger.debug(LogTag, "<- persist($topic) done")
        }
    }

    @SuppressLint("ApplySharedPref")
    override suspend fun remove(topic: String) {
        withContext(dispatcherProvider.io()) {
            // restoreFromPrefsAsync() must be in progress
            while (!cacheReady.get()) {
                logger.debug(LogTag, "-> remove($topic) waiting")
                delay(50L)
            }
        }

        writeMutex.withLock {
            cachedStates.remove(topic)
            withContext(dispatcherProvider.io()) {
                sharedPrefs.edit().remove(topic).commit()
            }
            // notify hot flow
            statesFlow.tryEmit(cachedStates.values.map { it.copy() }.toSet())
            logger.debug(LogTag, "<- remove($topic) done")
        }
    }

    override suspend fun removeAll() {
        withContext(dispatcherProvider.io()) {
            // restoreFromPrefsAsync() must be in progress
            while (!cacheReady.get()) {
                logger.debug(LogTag, "-> reset() waiting")
                delay(50L)
            }
        }

        writeMutex.withLock {
            val size = cachedStates.size
            cachedStates.clear()
            withContext(dispatcherProvider.io()) {
                sharedPrefs.edit().clear().commit()
            }
            // notify hot flow
            statesFlow.tryEmit(cachedStates.values.map { it.copy() }.toSet())
            logger.debug(LogTag, "<- reset($size) done")
        }
    }

}