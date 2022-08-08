/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.adapter.moshi.type_adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import walletconnect.core.session.model.json_rpc.CustomRpcMethod
import walletconnect.core.session.model.json_rpc.EthRpcMethod
import walletconnect.core.session.model.json_rpc.JsonRpcMethod
import walletconnect.core.session.model.json_rpc.SessionRpcMethod

class JsonRpcMethodTypeAdapter {

    @ToJson
    fun toJson(type: JsonRpcMethod?)
            : String? {
        return type?.value
    }

    @FromJson
    fun fromJson(json: String?)
            : JsonRpcMethod? {
        if (json == null) {
            return null
        } else {
            return try {
                when (json) {
                    "wc_sessionRequest" -> SessionRpcMethod.Request
                    "wc_sessionUpdate" -> SessionRpcMethod.Update

                    "eth_sign" -> EthRpcMethod.Sign
                    "personal_sign" -> EthRpcMethod.PersonalSign
                    "eth_signTypedData" -> EthRpcMethod.SignTypedData
                    "eth_signTransaction" -> EthRpcMethod.SignTransaction
                    "eth_sendRawTransaction" -> EthRpcMethod.SendRawTransaction
                    "eth_sendTransaction" -> EthRpcMethod.SendTransaction

                    else -> CustomRpcMethod(json)
                }
            } catch (_: Exception) {
                throw Exception("Invalid JsonRpcMethod: $json")
            }
        }
    }

}