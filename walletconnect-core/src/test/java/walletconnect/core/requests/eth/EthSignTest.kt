/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.requests.eth

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import walletconnect.core.Failure

class EthSignTest {

    @Test
    fun validate_validSign() {
        assertEquals(FakeEthSign.ValidSign, FakeEthSign.ValidSign.validate())
    }

    @Test
    fun validate_validPersonalSign() {
        assertEquals(FakeEthSign.ValidPersonalSign, FakeEthSign.ValidPersonalSign.validate())
    }

    @Test
    fun validate_invalidAddress() {
        Assert.assertThrows(Failure::class.java) {
            FakeEthSign.InvalidAddress.validate()
        }
    }

    @Test
    fun validate_invalidPersonalSign() {
        Assert.assertThrows(Failure::class.java) {
            FakeEthSign.InvalidPersonalSign.validate()
        }
    }

    @Test
    fun getRawMessage_validSign() {
        assertEquals("Raw String", FakeEthSign.ValidSign.getRawMessage())
    }

    @Test
    fun getRawMessage_validPersonalSign() {
        assertEquals("Raw String", FakeEthSign.ValidPersonalSign.getRawMessage())
    }

    @Test
    fun toList_validSign() {
        assertEquals(FakeEthSign.ValidSign.address, FakeEthSign.ValidSign.toList()[0])
        assertEquals(FakeEthSign.ValidSign.message, FakeEthSign.ValidSign.toList()[1])
    }

    @Test
    fun toList_validPersonalSign() {
        assertEquals(FakeEthSign.ValidPersonalSign.address, FakeEthSign.ValidPersonalSign.toList()[1])
        assertEquals(FakeEthSign.ValidPersonalSign.message, FakeEthSign.ValidPersonalSign.toList()[0])
    }

}