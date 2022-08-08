/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.model.json_rpc

data class JsonRpcError(val jsonrpc: String = "2.0",
                        val id: Long,
                        val error: JsonRpcErrorData)

// can't do sealed class because serialization/deserialization gets crazy complex
/**
 * @param[code] Check [RpcErrorCode]
 */
data class JsonRpcErrorData(val code: Int,
                            val message: String)

/**
 * - [Source](https://www.jsonrpc.org/specification)
 * - Custom server code between [-32000, -32099] can be used
 */
object RpcErrorCode {
    /** General server error */
    const val Server = -32000
    /** The JSON sent is not a valid Request object. */
    const val InvalidRequestObject = -32600
    /** The method does not exist or is not available. */
    const val RequestMethodNotAvailable = -32601
    /** Invalid method parameter(s). */
    const val InvalidRequestParams = -32602
    /**
     * Invalid JSON was received by the server.
     * An error occurred on the server while parsing the JSON text.
     */
    const val InvalidJson = -32700
    /** Internal JSON-RPC error. */
    const val InternalError = -32603
}
