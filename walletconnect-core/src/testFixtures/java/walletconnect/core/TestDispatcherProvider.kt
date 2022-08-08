/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import walletconnect.core.util.DispatcherProvider

class TestDispatcherProvider(private val dispatcher: TestDispatcher = StandardTestDispatcher())
    : DispatcherProvider {

    override fun io() = dispatcher
    override fun computation() = dispatcher
    override fun ui() = dispatcher

}