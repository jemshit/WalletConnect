/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.store.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import walletconnect.core.session_state.SessionStore
import walletconnect.core.session_state.SessionStoreTestBase
import walletconnect.core.session_state.model.FakeSessionStates

@RunWith(AndroidJUnit4::class)
class SharedPrefsSessionStoreTest : SessionStoreTestBase() {

    private val gson = Gson()
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val sharedPrefs = appContext.getSharedPreferences("temp_shared_prefs", Context.MODE_PRIVATE)

    override fun createSessionStore()
            : SessionStore {
        return SharedPrefsSessionStore(
                sharedPrefs,
                coroutineTestRule.dispatcherProvider,
                logger
        )
    }

    @Before
    fun before() {
        // NoOp
    }

    @After
    fun after() = runTest {
        sessionStore.removeAll()
        sharedPrefs.edit().clear().commit()
    }

    @Test
    fun restoreFromAllSessionsFile() = runTest {
        populateAllSessionsFile()

        FakeSessionStates.AllFakes.forEach { sessionState ->
            assertEquals(sessionState, sessionStore.get(sessionState.connectionParams.topic))
        }
    }

    private suspend fun populateAllSessionsFile() {
        logger.debug("SharedPrefsSessionStoreTest", "-> populateAllSessionsFile()")
        withContext(coroutineTestRule.testDispatcher) {
            val prefsEditor = sharedPrefs.edit()
            FakeSessionStates.AllFakes.forEach { sessionState ->
                val stateJson = gson.toJson(sessionState)
                prefsEditor.putString(sessionState.connectionParams.topic, stateJson)
            }
            prefsEditor.commit()
        }
        logger.debug("SharedPrefsSessionStoreTest", "<- populateAllSessionsFile()")
    }

}