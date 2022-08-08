/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.model.json_rpc

data class JsonRpcResponse<T>(val jsonrpc: String = "2.0",
                              val id: Long,
                              val result: T)