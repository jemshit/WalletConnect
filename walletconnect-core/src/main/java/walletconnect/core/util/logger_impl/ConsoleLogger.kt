/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.util.logger_impl

import walletconnect.core.util.Logger

class ConsoleLogger : Logger {
    override fun debug(tag: String, parameters: String?) {
        println("$tag\tDEBUG\t:$parameters")
    }

    override fun info(tag: String, parameters: String?) {
        println("$tag\tINFO\t:$parameters")
    }

    override fun warning(tag: String, parameters: String?) {
        println("$tag\tWARN\t:$parameters")
    }

    override fun error(tag: String, parameters: String?) {
        println("$tag\tERROR\t:$parameters")
    }
}