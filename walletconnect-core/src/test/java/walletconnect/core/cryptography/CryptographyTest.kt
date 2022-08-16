/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.cryptography

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import walletconnect.core.session.model.EncryptedPayload

class CryptographyTest {

    @Test
    fun validDecrypt() {
        val data = "1b3db3674de082d65455eba0ae61cfe7e681c8ef1132e60c8dbd8e52daf18f4fea42cc76366c833" +
                   "51dab6dca52682ff81f828753f89a21e1cc46587ca51ccd353914ffdd3b0394acfee392be6c22b3db" +
                   "9237d3f717a3777e3577dd70408c089a4c9c85130a68c43b0a8aadb00f1b8a8558798104e67aa4ff02" +
                   "7b35d4b989e7fd3988d5dcdd563105767670be735b21c4"
        val hmac = "a33f868e793ca4fcca964bcb64430f65e2f1ca7a779febeaf94c5373d6df48b3"
        val iv = "89ef1d6728bac2f1dcde2ef9330d2bb8"
        val key = "5caa3a74154cee16bd1b570a1330be46e086474ac2f4720530662ef1a469662c".hexToByteArray()
        val payload = EncryptedPayload(data = data,
                                       iv = iv,
                                       hmac = hmac)
        val decryptedBytes = Cryptography.decrypt(payload, key)
        val decrypted = String(decryptedBytes, Charsets.UTF_8)
        val expected =
            "{\"id\":1554098597199736,\"jsonrpc\":\"2.0\",\"method\":\"wc_sessionUpdate\",\"params\":[{\"approved\":false,\"chainId\":null,\"accounts\":null}]}"

        assertEquals(expected, decrypted)
    }

    @Test
    fun validEncrypt() {
        val expected =
            "{\"id\":1554098597199736,\"jsonrpc\":\"2.0\",\"method\":\"wc_sessionUpdate\",\"params\":[{\"approved\":false,\"chainId\":null,\"accounts\":null}]}"
                    .toByteArray(Charsets.UTF_8)
        val key = "5caa3a74154cee16bd1b570a1330be46e086474ac2f4720530662ef1a469662c".hexToByteArray()
        val payload = Cryptography.encrypt(data = expected,
                                           symmetricKey = key)
        val decrypted = Cryptography.decrypt(payload,
                                             key)

        assertArrayEquals(expected, decrypted)
    }

    @Test
    fun randomBytes_size() {
        val expected = 7
        assertEquals(expected, Cryptography.randomBytes(expected).size)

        val expected2 = 16
        assertEquals(expected2, Cryptography.randomBytes(expected2).size)

        val expected3 = 32
        assertEquals(expected3, Cryptography.randomBytes(expected3).size)

        val expected4 = 64
        assertEquals(expected4, Cryptography.randomBytes(expected4).size)
    }

    @Test
    fun randomBytes_shouldWorkWithEncryptDecrypt() {
        for (counter in 1 until 1_000) {
            val expected =
                "{\"id\":1554098597199736,\"jsonrpc\":\"2.0\",\"method\":\"wc_sessionUpdate\",\"params\":[{\"approved\":false,\"chainId\":null,\"accounts\":null}]}"
                        .toByteArray(Charsets.UTF_8)
            val key = Cryptography.randomBytes(size = 32)
            val payload = Cryptography.encrypt(data = expected,
                                               symmetricKey = key)
            val decrypted = Cryptography.decrypt(payload,
                                                 key)

            assertArrayEquals(expected, decrypted)
        }
    }

    @Test
    fun generateSymmetricKey_size() {
        assertEquals(32, Cryptography.generateSymmetricKey().size)
    }

    @Test
    fun generateSymmetricKey_shouldntThrow() {
        for (counter in 1 until 10_000) {
            assertEquals(32, Cryptography.generateSymmetricKey().size)
        }
    }

    @Test
    fun generateSymmetricKey_shouldWorkWithEncryptDecrypt() {
        for (counter in 1 until 1_000) {
            val expected =
                "{\"id\":1554098597199736,\"jsonrpc\":\"2.0\",\"method\":\"wc_sessionUpdate\",\"params\":[{\"approved\":false,\"chainId\":null,\"accounts\":null}]}"
                        .toByteArray(Charsets.UTF_8)
            val key = Cryptography.generateSymmetricKey()
            val payload = Cryptography.encrypt(data = expected,
                                               symmetricKey = key)
            val decrypted = Cryptography.decrypt(payload,
                                                 key)

            assertArrayEquals(expected, decrypted)
        }
    }

}