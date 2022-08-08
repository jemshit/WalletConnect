/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.adapter.gson.type_adapter

import com.google.gson.*
import walletconnect.core.session.model.json_rpc.CustomRpcMethod
import walletconnect.core.session.model.json_rpc.EthRpcMethod
import walletconnect.core.session.model.json_rpc.JsonRpcMethod
import walletconnect.core.session.model.json_rpc.SessionRpcMethod
import java.lang.reflect.Type

class JsonRpcMethodTypeAdapter
    : JsonSerializer<JsonRpcMethod>, JsonDeserializer<JsonRpcMethod> {

    override fun serialize(src: JsonRpcMethod?,
                           typeOfSrc: Type?,
                           context: JsonSerializationContext?)
            : JsonElement {
        return JsonPrimitive(src?.value)
    }

    override fun deserialize(json: JsonElement?,
                             typeOfT: Type?,
                             context: JsonDeserializationContext?)
            : JsonRpcMethod? {
        if (json == null) {
            return null
        } else {
            return try {
                when (val method = json.asString) {
                    "wc_sessionRequest" -> SessionRpcMethod.Request
                    "wc_sessionUpdate" -> SessionRpcMethod.Update

                    "eth_sign" -> EthRpcMethod.Sign
                    "personal_sign" -> EthRpcMethod.PersonalSign
                    "eth_signTypedData" -> EthRpcMethod.SignTypedData
                    "eth_signTransaction" -> EthRpcMethod.SignTransaction
                    "eth_sendRawTransaction" -> EthRpcMethod.SendRawTransaction
                    "eth_sendTransaction" -> EthRpcMethod.SendTransaction

                    else -> CustomRpcMethod(method)
                }
            } catch (_: Exception) {
                throw JsonParseException("Invalid JsonRpcMethod: $json")
            }
        }
    }

}