/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.requests.eth

import walletconnect.core.Failure
import walletconnect.core.FailureType
import walletconnect.core.cryptography.hexToByteArray
import walletconnect.core.cryptography.isHex
import walletconnect.core.session.model.json_rpc.EthRpcMethod

enum class SignType {
    Sign,
    PersonalSign;

    companion object {
        fun SignType.toMethod()
                : EthRpcMethod {
            return when (this) {
                Sign -> EthRpcMethod.Sign
                PersonalSign -> EthRpcMethod.PersonalSign
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            Sign -> "Sign"
            PersonalSign -> "PersonalSign"
        }
    }
}

/**
 * Model for [EthRpcMethod.Sign], [EthRpcMethod.PersonalSign]
 *
 * If [SignType.Sign], Metamask returns `eth_sign requires 32 byte message hash`.
 *
 * @param[address] 20 Bytes - address in hex.
 * @param[message] N Bytes - message to sign. If [SignType.Sign] -> raw data, if [SignType.PersonalSign] -> hex string.
 *                       Message can be JSON string, some wallets parse it and show
 * @param[type] if [SignType.Sign], index 0 should contain [address];
 *              if [SignType.PersonalSign], index 1 should contain [address]
 */
data class EthSign(val address: String,
                   val message: String,
                   val type: SignType) {

    /**
     * @throws [Failure] with [FailureType.InvalidEthAddress], [FailureType.InvalidHexString]
     */
    fun validate()
            : EthSign {
        // don't validate case sensitive address checksum, protocol doesn't enforce it

        // address
        if (!address.startsWith("0x")) {
            throw Failure(type = FailureType.InvalidHexString,
                          message = "'address' doesn't start with 0x")
        }
        // must be 20 Byte = 40 chars
        val addressHex = address.substring(2)
        if (!addressHex.isHex() || addressHex.length != 40) {
            throw Failure(type = FailureType.InvalidEthAddress,
                          message = "'address' invalid Hex or length (must be 40 chars)")
        }

        // messageToSign (EthSign uses raw data, while personalSign uses hex)
        if (type == SignType.PersonalSign) {
            if (!message.startsWith("0x")) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'messageToSign' doesn't start with 0x")
            }
            val messageToSignHex = message.substring(2)
            if (!messageToSignHex.isHex()) {
                throw Failure(type = FailureType.InvalidHexString,
                              message = "'messageToSign' invalid Hex")
            }
        }

        return this
    }

    fun toList(): List<String> {
        return when (type) {
            SignType.Sign -> listOf(address, message)
            SignType.PersonalSign -> listOf(message, address)
        }
    }

    fun getRawMessage(): String {
        return when (type) {
            SignType.Sign -> {
                // raw string
                message
            }
            SignType.PersonalSign -> {
                // hex -> string
                String(message.substring(2).hexToByteArray())
            }
        }
    }

}
