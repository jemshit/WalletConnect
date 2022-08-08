/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.util

interface Logger {

    fun debug(tag: String,
              parameters: String?) {
    }

    fun info(tag: String,
             parameters: String?) {
    }

    fun warning(tag: String,
                parameters: String?) {
    }

    fun error(tag: String,
              parameters: String?) {
    }

}