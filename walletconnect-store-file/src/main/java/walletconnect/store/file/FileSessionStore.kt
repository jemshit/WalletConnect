/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.store.file

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class FileSessionStore(private val storageFile: File,
                       private val dispatcherProvider: DispatcherProvider,
                       private val logger: Logger = EmptyLogger)
    : SessionStore {

    private val LogTag = "FileSessionStore"
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcherProvider.io() + SupervisorJob())

    private val gson = Gson()
    private val mapType: Type = object : TypeToken<Map<String, SessionState>>() {}.type
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
        restoreFromFileAsync()
    }

    /** Sets [cacheReady] to true when done */
    private fun restoreFromFileAsync() {
        logger.debug(LogTag, "-> restoreFromFileAsync($storageFile)")
        coroutineScope.launch {
            storageFile.createNewFile()

            // content can be "" OR {}. {} is valid Map, but "" is not valid Map
            val storeContent = storageFile.readText()

            if (storeContent.isNotBlank()) {
                logger.debug(LogTag, "  restoreFromFileAsync() file.notBlank")
                try {
                    val allState: Map<String, SessionState> = withContext(dispatcherProvider.computation()) {
                        gson.fromJson(storeContent, mapType)
                    }
                    cachedStates.putAll(allState)
                } catch (error: Exception) {
                    logger.error(LogTag, error.stackTraceToString())
                }
            }

            // notify hot flow
            statesFlow.tryEmit(cachedStates.values.map { it.copy() }.toSet())

            // update flag
            logger.debug(LogTag, "<- restoreFromFileAsync(${cachedStates.size}) cacheReady.set(true)")
            cacheReady.set(true)
        }
    }

    override suspend fun get(topic: String)
            : SessionState? {
        if (cacheReady.get()) {
            return cachedStates[topic]
        }

        withContext(dispatcherProvider.io()) {
            // restoreFromFileAsync() must be in progress
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
            // restoreFromFileAsync() must be in progress
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

    override suspend fun persist(topic: String,
                                 state: SessionState) {
        withContext(dispatcherProvider.io()) {
            // restoreFromFileAsync() must be in progress
            while (!cacheReady.get()) {
                logger.debug(LogTag, "-> persist($topic) waiting")
                delay(50L)
            }
        }

        writeMutex.withLock {
            // update cache first
            cachedStates[topic] = state
            writeStatesToFile()
            // notify hot flow
            statesFlow.tryEmit(cachedStates.values.map { it.copy() }.toSet())
            logger.debug(LogTag, "<- persist($topic) done")
        }
    }

    override suspend fun remove(topic: String) {
        withContext(dispatcherProvider.io()) {
            // restoreFromFileAsync() must be in progress
            while (!cacheReady.get()) {
                logger.debug(LogTag, "-> remove($topic) waiting")
                delay(50L)
            }
        }

        writeMutex.withLock {
            // update cache first
            val existingValue = cachedStates.remove(topic)
            if (existingValue != null) {
                writeStatesToFile()
            }
            // notify hot flow
            statesFlow.tryEmit(cachedStates.values.map { it.copy() }.toSet())
            logger.debug(LogTag, "<- remove($topic) done")
        }
    }

    override suspend fun removeAll() {
        withContext(dispatcherProvider.io()) {
            // restoreFromFileAsync() must be in progress
            while (!cacheReady.get()) {
                logger.debug(LogTag, "-> reset() waiting")
                delay(50L)
            }
        }

        writeMutex.withLock {
            val size = cachedStates.size
            if (size > 0) {
                // update cache first
                cachedStates.clear()
                withContext(dispatcherProvider.io()) {
                    storageFile.writeText("")
                }
                // notify hot flow
                statesFlow.tryEmit(cachedStates.values.map { it.copy() }.toSet())
            }
            logger.debug(LogTag, "<- reset($size) done")
        }
    }

    /** Write states from [cachedStates] to [storageFile] */
    private suspend fun writeStatesToFile() {
        while (!cacheReady.get()) {
            throw Exception("This is unexpected!")
        }

        logger.debug(LogTag, "  -> writeStatesToFile(${cachedStates.size})")
        withContext(dispatcherProvider.io()) {
            try {
                val json = withContext(dispatcherProvider.computation()) {
                    gson.toJson(cachedStates)
                }
                // even write "", because that is latest state
                storageFile.writeText(json)
            } catch (error: Exception) {
                logger.error(LogTag, error.stackTraceToString())
            }
            logger.debug(LogTag, "  <- writeStatesToFile(${cachedStates.size}) done")
        }
    }

}