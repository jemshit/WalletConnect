/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core

import walletconnect.core.session.SessionLifecycle
import walletconnect.core.session.model.SessionRequest
import walletconnect.core.session.model.json_rpc.CustomRpcMethod
import walletconnect.core.session.model.json_rpc.EthRpcMethod
import walletconnect.core.session.model.json_rpc.JsonRpcMethod
import java.lang.reflect.Type

interface DApp : SessionLifecycle {

    /**
     * Sends [SessionRequest] to Wallet.
     * - Not idempotent. Once approved by wallet, consecutive calls to this function is idempotent
     * - Wallet responds with same messageId
     */
    fun sendSessionRequest(chainId: Int?)

    /**
     * - Send any request such as ETH sendTransaction, sign, wallet specific requests ...
     * - You can store returned messageId mapped to Callback, (`Map<messageId, Callback>`), so when you
     *   receive response with same messageId, you can get corresponding Callback from Map and invoke it
     *
     * @param[method] one of [EthRpcMethod], [CustomRpcMethod]
     * @param[data] list of any model, usually single item
     * @param[itemType] Type of item in [data] list (not type of List!)
     *
     * @return messageId or null if error
     */
    suspend fun sendRequest(method: JsonRpcMethod,
                            data: List<Any>,
                            itemType: Type)
            : Long?

    /**
     * Serializes, encrypts and sends payload to the peer
     * - Only use after session is approved
     * - You can store returned [messageId] mapped to Callback, (`Map<messageId, Callback>`), so when you
     *   receive response with same [messageId], you can get corresponding Callback from Map and invoke it
     * - No callback is invoked
     *
     * @return messageId or null if error. Response will have same [messageId]
     */
    suspend fun encryptPayloadAndPublish(messageId: Long,
                                         payload: Any,
                                         type: Type,
                                         queueIfDisconnected: Boolean = true)
            : Long?

    fun generateMessageId(): Long

}