/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core

import walletconnect.core.socket.model.SocketMessage

data class Failure(val type: FailureType,
                   override val message: String? = null,
                   override val cause: Throwable? = null)
    : Exception()

sealed class FailureType {

    object InvalidBridgeUrl
        : FailureType()

    object InvalidUri
        : FailureType()

    object InvalidTopic
        : FailureType()

    object InvalidVersion
        : FailureType()

    object InvalidSymmetricKey
        : FailureType()

    object InvalidHexString
        : FailureType()

    object InvalidHMAC
        : FailureType()

    object Encryption
        : FailureType()

    object Decryption
        : FailureType()

    object SigningHMAC
        : FailureType()

    object SocketConnectionFlow
        : FailureType()

    object SocketMessageFlow
        : FailureType()

    object DeserializeSocketMessage
        : FailureType()

    data class SocketPublishMessage(val message: SocketMessage)
        : FailureType()

    object SocketPublishFlow
        : FailureType()

    object SerializeSessionMessage
        : FailureType()

    object DeserializeSessionMessage
        : FailureType()

    object SessionError
        : FailureType()

    object SessionIncomeFlow
        : FailureType()

    object SessionOutgoingFlow
        : FailureType()

    object InvalidEthAddress
        : FailureType()

    object InvalidRequest
        : FailureType()

    object InvalidResponse
        : FailureType()

    override fun toString(): String {
        return when (this) {
            Decryption -> "Decryption"
            DeserializeSessionMessage -> "DeserializeSessionMessage"
            DeserializeSocketMessage -> "DeserializeSocketMessage"
            Encryption -> "Encryption"
            InvalidBridgeUrl -> "InvalidBridgeUrl"
            InvalidEthAddress -> "InvalidEthAddress"
            InvalidHMAC -> "InvalidHMAC"
            InvalidHexString -> "InvalidHexString"
            InvalidRequest -> "InvalidRequest"
            InvalidResponse -> "InvalidResponse"
            InvalidSymmetricKey -> "InvalidSymmetricKey"
            InvalidTopic -> "InvalidTopic"
            InvalidUri -> "InvalidUri"
            InvalidVersion -> "InvalidVersion"
            SerializeSessionMessage -> "SerializeSessionMessage"
            SessionError -> "SessionError"
            SessionIncomeFlow -> "SessionIncomeFlow"
            SessionOutgoingFlow -> "SessionOutgoingFlow"
            SigningHMAC -> "SigningHMAC"
            SocketConnectionFlow -> "SocketConnectionFlow"
            SocketMessageFlow -> "SocketMessageFlow"
            SocketPublishFlow -> "SocketPublishFlow"
            is SocketPublishMessage -> "SocketPublishMessage(${this.message})"
        }
    }

}

fun Exception.toFailure(type: FailureType,
                        message: String? = null)
        : Failure {
    if (this is Failure) {
        return this
    }

    return Failure(type, message, cause = this)
}