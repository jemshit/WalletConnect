/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session.model

import walletconnect.core.Failure
import walletconnect.core.FailureType
import walletconnect.core.cryptography.isHex

/**
 * All payloads are consequently encrypted using the active symmetric key and also signed before
 * they are posted as messages to the Bridge server.
 *
 * The encryption and signing algorithms used are AES-256-CBC and HMAC-SHA256 respectively.
 *
 * All fields ([data], [hmac] and [iv]) are Hexadecimal strings. The receiving peer will consequently verify the
 * [hmac] before decrypting the [data] field using the active key and provided [iv].
 *
 * @param[data] Encrypted data
 * @param[hmac] verify this using symmetricKey and [iv]
 * @param[iv] use to verify [hmac]
 */
data class EncryptedPayload(val data: String,
                            val hmac: String,
                            val iv: String) {

    /** @throws [Failure] with [FailureType.InvalidHexString] */
    fun validate()
            : EncryptedPayload {

        if (!data.isHex(evenLength = true)
            || !hmac.isHex(evenLength = true)
            || !iv.isHex(evenLength = true)) {

            throw Failure(type = FailureType.InvalidHexString,
                          message = "data:$data, hmac:$hmac, iv:$iv")
        }

        return this
    }

}