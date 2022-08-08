/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.store.file

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import walletconnect.core.session_state.SessionStore
import walletconnect.core.session_state.SessionStoreTestBase
import walletconnect.core.session_state.model.FakeSessionStates
import java.io.File

class FileSessionStoreTest : SessionStoreTestBase() {

    private val gson = Gson()
    private val storeFile = File("temp_file_session_store.txt")

    override fun createSessionStore()
            : SessionStore {
        return FileSessionStore(
                storeFile,
                coroutineTestRule.dispatcherProvider,
                logger
        )
    }

    @Before
    fun before() {
        storeFile.createNewFile()
    }

    @After
    fun after() = runTest {
        sessionStore.removeAll()
        storeFile.delete()
    }

    @Test
    fun restoreFromEmptyFile() = runTest {
        populateEmptyFile()

        assertTrue(sessionStore.getAll().isNullOrEmpty())
    }

    @Test
    fun restoreFromEmptyMapFile() = runTest {
        populateEmptyMapFile()

        assertTrue(sessionStore.getAll().isNullOrEmpty())
    }

    @Test
    fun restoreFromAllSessionsFile() = runTest {
        populateAllSessionsFile()

        FakeSessionStates.AllFakes.forEach { sessionState ->
            assertEquals(sessionState, sessionStore.get(sessionState.connectionParams.topic))
        }
    }

    private suspend fun populateEmptyFile() {
        logger.debug("FileSessionStoreTest", "-> populateEmptyFile()")
        withContext(coroutineTestRule.testDispatcher) {
            storeFile.writeText("")
        }
        logger.debug("FileSessionStoreTest", "<- populateEmptyFile()")
    }

    private suspend fun populateEmptyMapFile() {
        logger.debug("FileSessionStoreTest", "-> populateEmptyMapFile()")
        withContext(coroutineTestRule.testDispatcher) {
            storeFile.writeText("{}")
        }
        logger.debug("FileSessionStoreTest", "-> populateEmptyMapFile()")
    }

    private suspend fun populateAllSessionsFile() {
        logger.debug("FileSessionStoreTest", "-> populateAllSessionsFile()")
        withContext(coroutineTestRule.testDispatcher) {
            val map = FakeSessionStates.AllFakes.associateBy { sessionState ->
                sessionState.connectionParams.topic
            }
            val json = gson.toJson(map)
            storeFile.writeText(json)
        }
        logger.debug("FileSessionStoreTest", "<- populateAllSessionsFile()")
    }

}