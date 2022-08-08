/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core

import walletconnect.core.session.SessionLifecycle
import walletconnect.core.session.model.SessionRequest
import walletconnect.core.session.model.SessionResponse
import walletconnect.core.session.model.json_rpc.JsonRpcError
import walletconnect.core.session.model.json_rpc.JsonRpcErrorData
import walletconnect.core.session.model.json_rpc.RpcErrorCode
import java.lang.reflect.Type

interface Wallet : SessionLifecycle {

    /**
     * Sends [SessionResponse] to DApp with same messageId of [SessionRequest]
     * - Not idempotent
     * - After this, session is agreed and persisted by both peers
     */
    fun approveSession(chainId: Int,
                       accounts: List<String>)

    /**
     * Sends [JsonRpcError] to DApp with same messageId of [SessionRequest]
     *   (as seen [here](https://docs.walletconnect.com/client-api))
     * - Not idempotent
     * - After this, session is deleted by both peers
     */
    fun rejectSession(errorMessage: String? = null)

    /** This is either used to update active session by wallet, or to delete session from both peers */
    fun updateSession(chainId: Int?,
                      accounts: List<String>?,
                      approved: Boolean = true)

    /**
     * @param[messageId] You must reply with same messageId of received request
     * @param[result]
     * @param[resultType] Make sure Type is correct.
     *                   E.g: (for Lists, it should be `jsonAdapter.getParametrizedType(List::class.java,String::class.java)`
     * @return [messageId] or null if error
     */
    suspend fun approveRequest(messageId: Long,
                               result: Any,
                               resultType: Type)
            : Long?

    /**
     * @param[messageId] You must reply with same messageId of received request
     * @param[errorType] example: [JsonRpcErrorData] ([RpcErrorCode.Server],"Request is Rejected!")
     *
     * @return [messageId] or null if error
     */
    suspend fun rejectRequest(messageId: Long,
                              errorType: JsonRpcErrorData?)
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