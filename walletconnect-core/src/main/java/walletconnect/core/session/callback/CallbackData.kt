/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.callback

import walletconnect.core.Failure
import walletconnect.core.requests.eth.EthSign
import walletconnect.core.requests.eth.EthTransaction
import walletconnect.core.session.model.json_rpc.JsonRpcErrorData
import walletconnect.core.session_state.model.PeerMeta
import walletconnect.core.session_state.model.SessionState

sealed interface CallbackData

data class FailureCallback(val failure: Failure)
    : CallbackData

sealed interface SocketCallback
    : CallbackData {

    object SocketConnecting
        : SocketCallback

    object SocketConnected
        : SocketCallback

    /** if [beforeHandling] is false, [messageId] is not null */
    data class SocketMessage(val beforeHandling: Boolean,
                             val decryptedPayloadJson: String,
                             val messageId: Long?)
        : SocketCallback

    object SocketDisconnected
        : SocketCallback

    object SocketClosed
        : SocketCallback

}

/** Both dApp and wallet get all callbacks */
sealed interface SessionCallback
    : CallbackData {

    /** peer data of dApp */
    data class SessionRequested(val messageId: Long,
                                val chainId: Int?,
                                val remotePeerId: String,
                                val remotePeerMeta: PeerMeta)
        : SessionCallback

    data class SessionApproved(val messageId: Long,
                               val chainId: Int?,
                               val accounts: List<String>?,
                               val remotePeerId: String,
                               val remotePeerMeta: PeerMeta)
        : SessionCallback

    data class SessionRejected(val errorMessage: String?)
        : SessionCallback

    data class SessionUpdated(val messageId: Long,
                              val chainId: Int?,
                              val accounts: List<String>?)
        : SessionCallback

    object SessionClosedLocally
        : SessionCallback

    data class SessionDeleted(val byMe: Boolean)
        : SessionCallback

    data class LocalSessionStateUpdated(val sessionState: SessionState?)
        : SessionCallback

    object SessionRestoredLocally
        : SessionCallback

    object SubscribedToMessages
        : SessionCallback

}

sealed interface RequestCallback
    : CallbackData {

    data class EthSignRequested(val messageId: Long,
                                val data: EthSign)
        : RequestCallback

    data class EthSignTypedDataRequested(val messageId: Long,
                                         val address: String,
                                         val data: Any)
        : RequestCallback

    data class EthSignResponse(val messageId: Long,
                               val signature: String)
        : RequestCallback

    data class EthSignTxRequested(val messageId: Long,
                                  val transaction: EthTransaction)
        : RequestCallback

    data class EthSignTxResponse(val messageId: Long,
                                 val signedTransaction: String)
        : RequestCallback

    data class EthSendRawTxRequested(val messageId: Long,
                                     val signedTransaction: String)
        : RequestCallback

    data class EthSendRawTxResponse(val messageId: Long,
                                    val transactionHash: String?)
        : RequestCallback

    data class EthSendTxRequested(val messageId: Long,
                                  val transaction: EthTransaction)
        : RequestCallback

    data class EthSendTxResponse(val messageId: Long,
                                 val transactionHash: String?)
        : RequestCallback

    data class CustomRequested(val messageId: Long,
                               val method: String,
                               val data: List<Any>)
        : RequestCallback

    /**
     * - called for custom JsonRpcResponse
     * - called in case we lose <messageId, JsonRpcMethod> data for activity/process death reasons
     * - called if payload is not JsonRpcRequest, JsonRpcResponse, JsonRpcError
     */
    data class CustomResponse(val messageId: Long,
                              val data: Any?)
        : RequestCallback

    /** Called for all types of JsonRpcMethods in case of rejected */
    data class RequestRejected(val messageId: Long,
                               val error: JsonRpcErrorData)
        : RequestCallback

}

fun CallbackData.simplifiedName()
        : String {
    return when (this) {
        is FailureCallback -> "Failure"
        is SessionCallback -> {
            when (this) {
                is SessionCallback.LocalSessionStateUpdated -> ""
                SessionCallback.SubscribedToMessages -> "SubscribedToMessages"
                SessionCallback.SessionRestoredLocally -> "SessionRestoredLocally"
                is SessionCallback.SessionRequested -> "SessionRequested"
                is SessionCallback.SessionApproved -> "SessionApproved"
                is SessionCallback.SessionUpdated -> "SessionUpdated"
                is SessionCallback.SessionRejected -> "SessionRejected"
                is SessionCallback.SessionDeleted -> if (byMe) "SessionDeletedByMe" else "SessionDeletedByPeer"
                SessionCallback.SessionClosedLocally -> "SessionClosedLocally"
            }
        }
        is SocketCallback -> {
            when (this) {
                SocketCallback.SocketConnecting -> "Connecting"
                SocketCallback.SocketConnected -> "Connected"
                is SocketCallback.SocketMessage -> "onMessage"
                SocketCallback.SocketDisconnected -> "Disconnected"
                SocketCallback.SocketClosed -> "Closed"
            }
        }
        is RequestCallback -> {
            when (this) {
                is RequestCallback.CustomRequested -> "CustomRequested(${this.method})"
                is RequestCallback.CustomResponse -> "CustomResponse"
                is RequestCallback.EthSignRequested -> "EthSignRequested"
                is RequestCallback.EthSignTypedDataRequested -> "EthSignTypedRequested"
                is RequestCallback.EthSignResponse -> "EthSignResponse"
                is RequestCallback.EthSignTxRequested -> "EthSignTxRequested"
                is RequestCallback.EthSignTxResponse -> "EthSignTxResponse"
                is RequestCallback.EthSendRawTxRequested -> "EthSendRawTxRequested"
                is RequestCallback.EthSendRawTxResponse -> "EthSendRawTxResponse"
                is RequestCallback.EthSendTxRequested -> "EthSendTxRequested"
                is RequestCallback.EthSendTxResponse -> "EthSendTxResponse"
                is RequestCallback.RequestRejected -> "RequestRejected"
            }
        }
    }
}

