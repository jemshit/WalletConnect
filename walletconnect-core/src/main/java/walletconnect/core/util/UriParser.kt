/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.util

import walletconnect.core.Failure
import walletconnect.core.FailureType
import java.net.URL
import java.net.URLDecoder

/**
 * [Source](https://stackoverflow.com/a/13592567/3736955)
 *
 * @return Map<Key, Values>, values can contain List of nulls
 * @throws [Failure]
 */
fun URL.splitQuery()
        : Map<String, List<String?>> {

    // LinkedHashMap = insertion-ordered
    val queryMap: MutableMap<String, MutableList<String?>> = LinkedHashMap()
    val pairs: List<String> = this.query?.split("&") ?: emptyList()

    for (pair in pairs) {
        val index = pair.indexOf("=")
        val key = if (index > 0) {
            try {
                URLDecoder.decode(pair.substring(0, index), Charsets.UTF_8)
            } catch (error: Exception) {
                throw Failure(type = FailureType.InvalidUri,
                              message = error.message,
                              cause = error)
            }
        } else {
            pair
        }
        if (!queryMap.containsKey(key)) {
            queryMap[key] = mutableListOf()
        }

        val value: String? = if (index > 0 && pair.length > index + 1) {
            try {
                URLDecoder.decode(pair.substring(index + 1), Charsets.UTF_8)
            } catch (error: Exception) {
                throw Failure(type = FailureType.InvalidUri,
                              message = error.message,
                              cause = error)
            }
        } else {
            null
        }
        queryMap[key]!!.add(value)
    }

    return queryMap
}

/**
 * [Source](https://stackoverflow.com/a/41268655/3736955)
 * - Checks according to [RFC2396](https://www.rfc-editor.org/rfc/rfc2396.html), but must be known scheme.
 *   `www.google.com`, `telnet://melvyl.ucop.edu` are not valid
 *
 * `<scheme>://<authority><path>?<query>#<fragment>`
 *
 * @return false if input is null or empty
 */
fun String?.isValidURL()
        : Boolean {
    if (this.isNullOrBlank()) {
        return false
    }

    return try {
        val url = URL(this)
        url.toURI()
        true
    } catch (_: Exception) {
        false
    }
}