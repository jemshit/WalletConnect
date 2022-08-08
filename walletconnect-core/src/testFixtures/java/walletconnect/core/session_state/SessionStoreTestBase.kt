/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session_state

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import walletconnect.core.CoroutineTestRule
import walletconnect.core.session_state.model.FakeSessionStates
import walletconnect.core.session_state.model.SessionState
import walletconnect.core.util.Logger
import walletconnect.core.util.logger_impl.ConsoleLogger

abstract class SessionStoreTestBase {

    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    protected val logger: Logger = ConsoleLogger()
    protected val sessionStore: SessionStore by lazy {
        // lazy because we want to pre-populate file before creating this
        createSessionStore()
    }

    abstract fun createSessionStore(): SessionStore

    @Test
    fun emptyStoreShouldReturnNull() = runTest {
        Assert.assertNull(sessionStore.get("unknown"))
        Assert.assertTrue(sessionStore.getAll().isNullOrEmpty())
    }

    @Test
    fun persistAndGetSingle() = runTest {
        val topic = FakeSessionStates.Normal.connectionParams.topic

        sessionStore.persist(topic,
                             FakeSessionStates.Normal)

        Assert.assertEquals(FakeSessionStates.Normal, sessionStore.get(topic))
    }

    @Test
    fun persistAndGetMultiple() = runTest {
        FakeSessionStates.AllFakes.forEach { sessionState ->
            sessionStore.persist(sessionState.connectionParams.topic,
                                 sessionState)
        }

        FakeSessionStates.AllFakes.forEach { sessionState ->
            Assert.assertEquals(sessionState, sessionStore.get(sessionState.connectionParams.topic))
        }

        sessionStore.getAll()!!.forEach { sessionState ->
            Assert.assertTrue(FakeSessionStates.AllFakes.contains(sessionState))
        }
    }

    @Test
    fun updateSession() = runTest {
        val topic = FakeSessionStates.Normal.connectionParams.topic
        val original = FakeSessionStates.Normal
        val updated = FakeSessionStates.Normal.copy(remotePeerId = "updated",
                                                    updatedAt = System.currentTimeMillis())

        sessionStore.persist(topic,
                             original)
        sessionStore.persist(topic,
                             updated)

        Assert.assertEquals(updated, sessionStore.get(topic))
    }

    @Test
    fun removeAll() = runTest {
        sessionStore.removeAll()
        Assert.assertTrue(sessionStore.getAll().isNullOrEmpty())

        FakeSessionStates.AllFakes.forEach { sessionState ->
            sessionStore.persist(sessionState.connectionParams.topic,
                                 sessionState)
        }
        Assert.assertFalse(sessionStore.getAll().isNullOrEmpty())

        sessionStore.removeAll()
        Assert.assertTrue(sessionStore.getAll().isNullOrEmpty())
    }

    protected suspend fun waitUntilReceived(actual: () -> Set<SessionState>,
                                            expected: Set<SessionState>,
                                            timeout: Long = 10_000) {
        // wait for real, don't use TestDispatcher
        withContext(Dispatchers.Default) {
            withTimeout(timeout) {
                while (!actual().equals(expected)) {
                    delay(50)
                }
            }
        }
    }

    @Test
    fun persistAndGetMultipleFlow() = runTest {
        var states: Set<SessionState> = emptySet()
        val observer = launch {
            sessionStore.getAllAsFlow()
                    .collectLatest { newStates ->
                        if (newStates.isNotEmpty() && states.isNotEmpty()) {
                            // assert distinctUntilChanged
                            assertNotEquals(states, newStates)
                        }
                        states = newStates
                    }
        }

        // single persist
        sessionStore.persist(FakeSessionStates.Normal.connectionParams.topic,
                             FakeSessionStates.Normal)
        waitUntilReceived(actual = { states },
                          expected = setOf(FakeSessionStates.Normal))

        // multiple same persist
        sessionStore.persist(FakeSessionStates.Normal.connectionParams.topic,
                             FakeSessionStates.Normal)
        sessionStore.persist(FakeSessionStates.Normal.connectionParams.topic,
                             FakeSessionStates.Normal)
        delay(1_000L)
        waitUntilReceived(actual = { states },
                          expected = setOf(FakeSessionStates.Normal))

        // multiple unique persist
        sessionStore.persist(FakeSessionStates.Normal.connectionParams.topic,
                             FakeSessionStates.Normal)
        sessionStore.persist(FakeSessionStates.NegativeChainId.connectionParams.topic,
                             FakeSessionStates.NegativeChainId)
        delay(1_000L)
        waitUntilReceived(actual = { states },
                          expected = setOf(FakeSessionStates.Normal, FakeSessionStates.NegativeChainId))


        // remove
        sessionStore.remove(FakeSessionStates.NegativeChainId.connectionParams.topic)
        delay(1_000L)
        waitUntilReceived(actual = { states },
                          expected = setOf(FakeSessionStates.Normal))

        sessionStore.remove(FakeSessionStates.Normal.connectionParams.topic)
        delay(1_000L)
        waitUntilReceived(actual = { states },
                          expected = setOf())

        // removeAll
        sessionStore.persist(FakeSessionStates.Normal.connectionParams.topic,
                             FakeSessionStates.Normal)
        sessionStore.persist(FakeSessionStates.NegativeChainId.connectionParams.topic,
                             FakeSessionStates.NegativeChainId)
        delay(1_000L)
        waitUntilReceived(actual = { states },
                          expected = setOf(FakeSessionStates.Normal, FakeSessionStates.NegativeChainId))

        sessionStore.removeAll()
        delay(1_000L)
        waitUntilReceived(actual = { states },
                          expected = setOf())

        // multiple observers
        var states2: Set<SessionState> = emptySet()
        val observer2 = launch {
            sessionStore.getAllAsFlow()
                    .collectLatest { newStates ->
                        if (newStates.isNotEmpty() && states2.isNotEmpty()) {
                            // assert distinctUntilChanged
                            assertNotEquals(states2, newStates)
                        }
                        states2 = newStates
                    }
        }

        sessionStore.persist(FakeSessionStates.Normal.connectionParams.topic,
                             FakeSessionStates.Normal)
        sessionStore.persist(FakeSessionStates.NegativeChainId.connectionParams.topic,
                             FakeSessionStates.NegativeChainId)

        delay(1_000L)
        waitUntilReceived(actual = { states },
                          expected = setOf(FakeSessionStates.Normal, FakeSessionStates.NegativeChainId))
        waitUntilReceived(actual = { states2 },
                          expected = setOf(FakeSessionStates.Normal, FakeSessionStates.NegativeChainId))

        observer.cancel()
        observer2.cancel()
    }

}