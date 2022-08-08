/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.cryptography

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import walletconnect.core.Failure
import walletconnect.core.FailureType

class HexByteExtensionsTest {

    @Test
    fun valid() {
        val input = "0123456789abcdef"
        assertEquals(input, input.hexToByteArray().toHex())
    }

    @Test
    fun validNumbers() {
        val input = "0123456789"
        assertEquals(input, input.hexToByteArray().toHex())
    }

    @Test
    fun validChars() {
        val input = "abcdef"
        assertEquals(input, input.hexToByteArray().toHex())
    }

    @Test
    fun validCharsUppercase() {
        val input = "ABCDEF"
        assertEquals(input.lowercase(), input.hexToByteArray().toHex())
    }

    @Test
    fun validCharsMixedcase() {
        val input = "AbCdEF"
        assertEquals(input.lowercase(), input.hexToByteArray().toHex())
    }

    @Test
    fun validNumbersCharsMixedcase() {
        val input = "01Ab23C039dEEF"
        assertEquals(input.lowercase(), input.hexToByteArray().toHex())
    }

    @Test
    fun invalidHex() {
        val input = "GHLERPOK"
        val error = Assert.assertThrows(Failure::class.java) {
            input.hexToByteArray()
        }
        assertEquals(FailureType.InvalidHexString, error.type)
    }

    @Test
    fun invalidLength() {
        val input = "123"
        val error = Assert.assertThrows(Failure::class.java) {
            input.hexToByteArray()
        }
        assertEquals(FailureType.InvalidHexString, error.type)
    }

    @Test
    fun nonHexConversion() {
        assertEquals("walletconnect", String("walletconnect".toHex().hexToByteArray()))
    }

    @Test
    fun longToHex() {
        // <https://www.rapidtables.com/convert/number/decimal-to-hex.html>

        val input = 0L
        val expected = "0"
        assertEquals(expected, input.toHex().lowercase())

        val input2 = 15L
        val expected2 = "f"
        assertEquals(expected2, input2.toHex().lowercase())

        val input3 = 16L
        val expected3 = "10"
        assertEquals(expected3, input3.toHex().lowercase())

        val input4 = 100L
        val expected4 = "64"
        assertEquals(expected4, input4.toHex().lowercase())

        val input5 = 2835L
        val expected5 = "b13"
        assertEquals(expected5, input5.toHex().lowercase())

        val input6 = 3958243533L
        val expected6 = "ebee00cd"
        assertEquals(expected6, input6.toHex().lowercase())
    }

    @Test
    fun intToHex() {
        // <https://www.rapidtables.com/convert/number/decimal-to-hex.html>

        val input = 0
        val expected = "0"
        assertEquals(expected, input.toHex().lowercase())

        val input2 = 15
        val expected2 = "f"
        assertEquals(expected2, input2.toHex().lowercase())

        val input3 = 16
        val expected3 = "10"
        assertEquals(expected3, input3.toHex().lowercase())

        val input4 = 100
        val expected4 = "64"
        assertEquals(expected4, input4.toHex().lowercase())

        val input5 = 2835
        val expected5 = "b13"
        assertEquals(expected5, input5.toHex().lowercase())

        val input6 = 3958243533
        val expected6 = "ebee00cd"
        assertEquals(expected6, input6.toHex().lowercase())
    }

}