/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.adapter.moshi.type_adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import walletconnect.core.socket.model.SocketMessageType
import java.util.*

class SocketMessageTypeAdapter {

    @ToJson
    fun toJson(type: SocketMessageType?)
            : String? {
        return type?.name?.lowercase(Locale.ROOT)
    }

    @FromJson
    fun fromJson(json: String?)
            : SocketMessageType? {
        if (json == null) {
            return null
        } else {
            return try {
                when (json) {
                    "pub" -> SocketMessageType.Pub
                    "sub" -> SocketMessageType.Sub
                    else -> throw Exception("${json} is not valid SocketMessageType")
                }
            } catch (_: Exception) {
                throw Exception("Invalid SocketMessageType: $json")
            }
        }
    }

}