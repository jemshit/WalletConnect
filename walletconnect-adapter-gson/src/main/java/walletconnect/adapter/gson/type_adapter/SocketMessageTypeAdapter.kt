/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.adapter.gson.type_adapter

import com.google.gson.*
import walletconnect.core.socket.model.SocketMessageType
import java.lang.reflect.Type
import java.util.*

class SocketMessageTypeAdapter
    : JsonSerializer<SocketMessageType>, JsonDeserializer<SocketMessageType> {

    override fun serialize(src: SocketMessageType?,
                           typeOfSrc: Type?,
                           context: JsonSerializationContext?)
            : JsonElement {
        return JsonPrimitive(src?.name?.lowercase(Locale.ROOT))
    }

    override fun deserialize(json: JsonElement?,
                             typeOfT: Type?,
                             context: JsonDeserializationContext?)
            : SocketMessageType? {
        if (json == null) {
            return null
        } else {
            return try {
                when (json.asString) {
                    "pub" -> SocketMessageType.Pub
                    "sub" -> SocketMessageType.Sub
                    else -> throw JsonParseException("${json.asString} is not valid SocketMessageType")
                }
            } catch (_: Exception) {
                throw JsonParseException("Invalid SocketMessageType: $json")
            }
        }
    }

}