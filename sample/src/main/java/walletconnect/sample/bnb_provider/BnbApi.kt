/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample.bnb_provider

import retrofit2.http.Body
import retrofit2.http.POST
import walletconnect.core.requests.eth.EthTransaction
import walletconnect.core.session.model.json_rpc.JsonRpcRequest
import walletconnect.core.session.model.json_rpc.JsonRpcResponse

interface BnbApi {

    @POST(".")
    suspend fun getNextNonce(@Body jsonRpc: JsonRpcRequest<String>)
            : JsonRpcResponse<String>

    @POST(".")
    suspend fun estimateGas(@Body jsonRpc: JsonRpcRequest<EthTransaction>)
            : JsonRpcResponse<String>

    @POST(".")
    suspend fun getGasPrice(@Body jsonRpc: JsonRpcRequest<String>)
            : JsonRpcResponse<String>

}