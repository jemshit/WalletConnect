/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import walletconnect.core.util.DispatcherProvider

class CoroutineTestRule(val testDispatcher: TestDispatcher = StandardTestDispatcher(),
                        val scope: CoroutineScope = CoroutineScope(testDispatcher),
                        val dispatcherProvider: DispatcherProvider = TestDispatcherProvider(testDispatcher))
    : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
        scope.cancel()
    }

}