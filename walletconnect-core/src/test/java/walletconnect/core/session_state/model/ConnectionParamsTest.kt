/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.session_state.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import walletconnect.core.Failure
import walletconnect.core.FailureType
import java.net.URLEncoder

class ConnectionParamsTest {

    @Test
    fun normalToUriAndFromUri() {
        val expected = FakeConnectionParams.Normal
        val expectedUri = "wc:${expected.topic}" +
                          "@${expected.version}" +
                          "?bridge=${URLEncoder.encode(expected.bridgeUrl, Charsets.UTF_8)}" +
                          "&key=${expected.symmetricKey}"

        assertEquals(expectedUri, expected.toUri())
        assertEquals(expected, ConnectionParams.fromUri(expected.toUri()))
    }

    @Test
    fun versionSemver() {
        val expected = FakeConnectionParams.VersionSemver
        val expectedUri = "wc:${expected.topic}" +
                          "@${expected.version}" +
                          "?bridge=${URLEncoder.encode(expected.bridgeUrl, Charsets.UTF_8)}" +
                          "&key=${expected.symmetricKey}"

        assertEquals(expectedUri, expected.toUri())
        assertEquals(expected, ConnectionParams.fromUri(expected.toUri()))
    }

    @Test
    fun emptyTopic() {
        val expected = FakeConnectionParams.EmptyTopic

        val error = assertThrows(Failure::class.java) {
            expected.toUri()
        }
        assertEquals(FailureType.InvalidTopic, error.type)
    }

    @Test
    fun emptyVersion() {
        val expected = FakeConnectionParams.EmptyVersion

        val error = assertThrows(Failure::class.java) {
            expected.toUri()
        }
        assertEquals(FailureType.InvalidVersion, error.type)
    }

    @Test
    fun invalidBridgeUrl() {
        val expected = FakeConnectionParams.InvalidBridgeUrl

        val error = assertThrows(Failure::class.java) {
            expected.toUri()
        }
        assertEquals(FailureType.InvalidBridgeUrl, error.type)
    }

    @Test
    fun emptyBridgeUrl() {
        val expected = FakeConnectionParams.EmptyBridgeUrl

        val error = assertThrows(Failure::class.java) {
            expected.toUri()
        }
        assertEquals(FailureType.InvalidBridgeUrl, error.type)
    }

    @Test
    fun emptySymmetricKey() {
        val expected = FakeConnectionParams.EmptySymmetricKey

        val error = assertThrows(Failure::class.java) {
            expected.toUri()
        }
        assertEquals(FailureType.InvalidSymmetricKey, error.type)
    }

    @Test
    fun invalidSymmetricKey() {
        val expected = FakeConnectionParams.NonHexSymmetricKey

        val error = assertThrows(Failure::class.java) {
            expected.toUri()
        }
        assertEquals(FailureType.InvalidSymmetricKey, error.type)
    }

    @Test
    fun fromUriEmpty() {
        val error = assertThrows(Failure::class.java) {
            ConnectionParams.fromUri("")
        }
        assertEquals(FailureType.InvalidUri, error.type)
    }

    @Test
    fun fromUriEmptyProtocol() {
        val error = assertThrows(Failure::class.java) {
            val expected = FakeConnectionParams.Normal
            ConnectionParams.fromUri("${expected.topic}" +
                                     "@${expected.version}" +
                                     "?bridge=${URLEncoder.encode(expected.bridgeUrl, Charsets.UTF_8)}" +
                                     "&key=${expected.symmetricKey}")
        }
        assertEquals(FailureType.InvalidUri, error.type)
    }

    @Test
    fun fromUriInvalidTopic() {
        val error = assertThrows(Failure::class.java) {
            val expected = FakeConnectionParams.Normal
            ConnectionParams.fromUri("wc:${expected.topic}" +
                                     "${expected.version}" +
                                     "?bridge=${URLEncoder.encode(expected.bridgeUrl, Charsets.UTF_8)}" +
                                     "&key=${expected.symmetricKey}")
        }
        assertEquals(FailureType.InvalidTopic, error.type)
    }

    @Test
    fun fromUriInvalidBridge() {
        val error = assertThrows(Failure::class.java) {
            val expected = FakeConnectionParams.Normal
            ConnectionParams.fromUri("wc:${expected.topic}" +
                                     "@${expected.version}" +
                                     "?key=${expected.symmetricKey}")
        }
        assertEquals(FailureType.InvalidBridgeUrl, error.type)
    }

    @Test
    fun fromUriInvalidKey() {
        val error = assertThrows(Failure::class.java) {
            val expected = FakeConnectionParams.Normal
            ConnectionParams.fromUri("wc:${expected.topic}" +
                                     "@${expected.version}" +
                                     "?bridge=${URLEncoder.encode(expected.bridgeUrl, Charsets.UTF_8)}")
        }
        assertEquals(FailureType.InvalidSymmetricKey, error.type)
    }

}