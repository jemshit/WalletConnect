/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.model.json_rpc

/**
 * [params] is always a [Collection](https://docs.walletconnect.com/tech-spec#cross-peer-events)
 */
data class JsonRpcRequest<T>(val jsonrpc: String = "2.0",
                             val id: Long,
                             val method: JsonRpcMethod,
                             val params: List<T>)