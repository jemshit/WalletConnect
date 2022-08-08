/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.cryptography

import walletconnect.core.Failure
import walletconnect.core.FailureType
import walletconnect.core.session.model.EncryptedPayload
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// PKCS5Padding is PKCS7Padding in Java, just wrong naming: <https://stackoverflow.com/a/10194082/3736955>
private val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
private val MAC_ALGORITHM = "HmacSHA256"
private val KeySize = 32 // bytes
private val IvSize = 16 // bytes

object Cryptography {

    /** @throws [Failure] */
    fun encrypt(data: ByteArray,
                symmetricKey: ByteArray)
            : EncryptedPayload {

        if (symmetricKey.isEmpty() || symmetricKey.size != KeySize) {
            throw Failure(type = FailureType.InvalidSymmetricKey)
        }

        try {
            val iv: ByteArray = randomBytes(IvSize)
            val keySpec = SecretKeySpec(symmetricKey, "AES")
            val encryptedData = with(Cipher.getInstance(CIPHER_ALGORITHM)) {
                init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
                doFinal(data)
            }
            val hmac: String = computeHMAC(data = encryptedData,
                                           iv = iv,
                                           key = symmetricKey)

            return EncryptedPayload(data = encryptedData.toHex(),
                                    iv = iv.toHex(),
                                    hmac = hmac)
                    .validate()
        } catch (error: Exception) {
            throw Failure(type = FailureType.Encryption,
                          message = error.message,
                          cause = error)
        }
    }

    /** @throws [Failure] */
    fun decrypt(payload: EncryptedPayload,
                symmetricKey: ByteArray)
            : ByteArray {

        if (symmetricKey.isEmpty() || symmetricKey.size != KeySize) {
            throw Failure(type = FailureType.InvalidSymmetricKey)
        }

        // verify
        val data = payload.data.hexToByteArray()
        val iv = payload.iv.hexToByteArray()
        val computedHMAC = computeHMAC(data = data,
                                       iv = iv,
                                       key = symmetricKey)
        if (computedHMAC.lowercase(Locale.ROOT) != payload.hmac.lowercase(Locale.ROOT)) {
            throw Failure(type = FailureType.InvalidHMAC)
        }

        // decode
        try {
            val keySpec = SecretKeySpec(symmetricKey, "AES")
            return with(Cipher.getInstance(CIPHER_ALGORITHM)) {
                init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
                doFinal(data)
            }
        } catch (error: Exception) {
            throw Failure(type = FailureType.Decryption,
                          message = error.message,
                          cause = error)
        }

    }

    /**
     * [Check](https://stackoverflow.com/a/48089539/3736955)
     * @throws [Failure] with [FailureType.SigningHMAC]
     */
    fun computeHMAC(data: ByteArray,
                    iv: ByteArray,
                    key: ByteArray)
            : String {
        try {
            val payload = data + iv
            return with(Mac.getInstance(MAC_ALGORITHM)) {
                init(SecretKeySpec(key, MAC_ALGORITHM))
                doFinal(payload).toHex()
            }
        } catch (error: Exception) {
            throw Failure(type = FailureType.SigningHMAC,
                          message = error.message,
                          cause = error)
        }
    }

    private fun randomBytes(size: Int)
            : ByteArray {
        return ByteArray(size).also { bytes ->
            SecureRandom().nextBytes(bytes)
        }
    }

}