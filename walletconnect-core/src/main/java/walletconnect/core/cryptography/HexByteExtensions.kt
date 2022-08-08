/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.cryptography

import walletconnect.core.Failure
import walletconnect.core.FailureType
import java.util.*

private const val HEX_CHARS = "0123456789abcdef"

/**
 * Input must be even length and Hex String
 * [Check](https://stackoverflow.com/a/140861/3736955)
 */
fun String.hexToByteArray()
        : ByteArray {
    if (this.length % 2 != 0) {
        throw Failure(type = FailureType.InvalidHexString,
                      message = "String length must be even, 1-byte = 2-hex-chars")
    }
    if (!this.isHex()) {
        throw Failure(type = FailureType.InvalidHexString)
    }

    val hex = lowercase(Locale.ROOT)
    val result = ByteArray(length / 2)

    for (index in hex.indices step 2) {
        val firstChar = HEX_CHARS.indexOf(hex[index])
        val secondChar = HEX_CHARS.indexOf(hex[index + 1])

        val octet = firstChar.shl(4) + secondChar
        result[index / 2] = octet.toByte()
    }

    return result
}

/** [Check](https://stackoverflow.com/a/9855338/3736955) */
fun ByteArray.toHex()
        : String {

    val result = StringBuffer(size * 2)

    this.forEach { byte: Byte ->
        val octet = byte.toInt() and 0xFF
        result.append(HEX_CHARS[octet.ushr(4)])
        result.append(HEX_CHARS[octet and 0x0F])
    }

    return result.toString()
}

/** Hex string can be in odd length, e.g: numbers in hex. They must be even only if they are converted to/from ByteArray */
fun String.isHex(evenLength: Boolean = false)
        : Boolean {
    if (isBlank()) {
        return false
    }
    if (evenLength && length % 2 != 0) {
        // must be even, 1-byte = 2-hex-chars
        return false
    }

    return this.all { char -> HEX_CHARS.contains(char.lowercaseChar()) }
}

/** without 0x in the beginning */
fun String.toHex()
        : String {
    return toByteArray().toHex()
}

/** without 0x in the beginning */
fun Long.toHex()
        : String {
    //return java.lang.Long.toHexString(this)
    return String.format("%#x", this).substring(2)
}

/** without 0x in the beginning */
fun Int.toHex()
        : String {
    //return Integer.toHexString(this)
    return String.format("%#x", this).substring(2)
}