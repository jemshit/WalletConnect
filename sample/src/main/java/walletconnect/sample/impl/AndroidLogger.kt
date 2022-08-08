/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample.impl

import android.util.Log
import walletconnect.core.util.Logger

class AndroidLogger : Logger {
    override fun debug(tag: String, parameters: String?) {
        Log.d(tag, "$parameters")
    }

    override fun info(tag: String, parameters: String?) {
        Log.i(tag, "$parameters")
    }

    override fun warning(tag: String, parameters: String?) {
        Log.w(tag, "$parameters")
    }

    override fun error(tag: String, parameters: String?) {
        Log.e(tag, "$parameters")
    }
}