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

fun CallbackData.simplifiedName(withContent: Boolean = false)
        : String {
    return when (this) {
        is FailureCallback -> {
            if (withContent) {
                "Failure(${this.failure.type})"
            } else {
                "Failure"
            }
        }
        is SessionCallback -> {
            when (this) {
                is SessionCallback.LocalSessionStateUpdated ->
                    ""
                SessionCallback.SubscribedToMessages ->
                    "SubscribedToMessages"
                SessionCallback.SessionRestoredLocally ->
                    "SessionRestoredLocally"
                is SessionCallback.SessionRequested -> if (withContent) {
                    "$this"
                } else {
                    "SessionRequested"
                }
                is SessionCallback.SessionApproved -> if (withContent) {
                    "$this"
                } else {
                    "SessionApproved"
                }
                is SessionCallback.SessionUpdated -> if (withContent) {
                    "$this"
                } else {
                    "SessionUpdated"
                }
                is SessionCallback.SessionRejected -> if (withContent) {
                    "$this"
                } else {
                    "SessionRejected"
                }
                is SessionCallback.SessionDeleted ->
                    if (byMe) "SessionDeletedByMe" else "SessionDeletedByPeer"
                SessionCallback.SessionClosedLocally ->
                    "SessionClosedLocally"
            }
        }
        is SocketCallback -> {
            when (this) {
                SocketCallback.SocketConnecting -> "Connecting"
                SocketCallback.SocketConnected -> "Connected"
                is SocketCallback.SocketMessage -> if (withContent) {
                    "onMessage($this)"
                } else {
                    "onMessage"
                }
                SocketCallback.SocketDisconnected -> "Disconnected"
                SocketCallback.SocketClosed -> "Closed"
            }
        }
        is RequestCallback -> {
            when (this) {
                is RequestCallback.CustomRequested -> if (withContent) {
                    "$this"
                } else {
                    "CustomRequested(${this.method})"
                }
                is RequestCallback.CustomResponse -> if (withContent) {
                    "$this"
                } else {
                    "CustomResponse"
                }
                is RequestCallback.EthSignRequested -> if (withContent) {
                    "$this"
                } else {
                    "EthSignRequested"
                }
                is RequestCallback.EthSignTypedDataRequested -> if (withContent) {
                    "$this"
                } else {
                    "EthSignTypedRequested"
                }
                is RequestCallback.EthSignResponse -> if (withContent) {
                    "$this"
                } else {
                    "EthSignResponse"
                }
                is RequestCallback.EthSignTxRequested -> if (withContent) {
                    "$this"
                } else {
                    "EthSignTxRequested"
                }
                is RequestCallback.EthSignTxResponse -> if (withContent) {
                    "$this"
                } else {
                    "EthSignTxResponse"
                }
                is RequestCallback.EthSendRawTxRequested -> if (withContent) {
                    "$this"
                } else {
                    "EthSendRawTxRequested"
                }
                is RequestCallback.EthSendRawTxResponse -> if (withContent) {
                    "$this"
                } else {
                    "EthSendRawTxResponse"
                }
                is RequestCallback.EthSendTxRequested -> if (withContent) {
                    "$this"
                } else {
                    "EthSendTxRequested"
                }
                is RequestCallback.EthSendTxResponse -> if (withContent) {
                    "$this"
                } else {
                    "EthSendTxResponse"
                }
                is RequestCallback.RequestRejected -> if (withContent) {
                    "$this"
                } else {
                    "RequestRejected"
                }
            }
        }
    }
}

