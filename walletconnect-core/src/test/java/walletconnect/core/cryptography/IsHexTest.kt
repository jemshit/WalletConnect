/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.cryptography

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsHexTest {

    @Test
    fun validNumbers() {
        assertTrue("0123456789".isHex())
    }

    @Test
    fun validChars() {
        assertTrue("abcdef".isHex())
    }

    @Test
    fun validCharsUppercase() {
        assertTrue("ABCDEF".isHex())
    }

    @Test
    fun validCharsMixedcase() {
        assertTrue("abCDef".isHex())
    }

    @Test
    fun validMixed() {
        assertTrue("012ab3435C57D7e76f".isHex())
    }

    @Test
    fun invalidChars() {
        assertFalse("gHij".isHex())
    }

    @Test
    fun invalidAndValidMixedChars() {
        assertFalse("abCghI340j".isHex())
    }

    @Test
    fun invalidCharsValidNumbers() {
        assertFalse("20984ijH".isHex())
    }

    @Test
    fun oddLength() {
        assertFalse("abc".isHex(evenLength = true))
    }

}