/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class UriParserTest {

    @Test
    fun splitQuery_nullValue() {
        val url = "https://stackoverflow.com?param1=value1&param2="

        val result = URL(url).splitQuery()

        // {param1=["value1"], param2=[null]}
        assertTrue(result["param1"]!!.size == 1 && result["param1"]!![0] == "value1")
        assertTrue(result["param2"]!!.size == 1 && result["param2"]!![0] == null)
    }

    @Test
    fun splitQuery_nullValue2() {
        val url = "https://stackoverflow.com?param1=value1&param2"

        val result = URL(url).splitQuery()

        // {param1=["value1"], param2=[null]}
        assertTrue(result["param1"]!!.size == 1 && result["param1"]!![0] == "value1")
        assertTrue(result["param2"]!!.size == 1 && result["param2"]!![0] == null)
    }

    @Test
    fun splitQuery_multipleValuesWithNull() {
        val url = "https://stackoverflow.com?param1=value1&param3=value3&param3"

        val result = URL(url).splitQuery()

        // {param1=["value1"], param3=["value3", null]}
        assertTrue(result["param1"]!!.size == 1 && result["param1"]!![0] == "value1")
        assertTrue(result["param3"]!!.size == 2 && result["param3"]!![0] == "value3")
        assertTrue(result["param3"]!!.size == 2 && result["param3"]!![1] == null)
    }

    @Test
    fun splitQuery_multipleValuesWithNull2() {
        val url = "https://stackoverflow.com?a=&b=&c="

        val result = URL(url).splitQuery()

        // {a=[null], b=[null], c=[null]}
        assertTrue(result["a"]!!.size == 1 && result["a"]!![0] == null)
        assertTrue(result["b"]!!.size == 1 && result["b"]!![0] == null)
        assertTrue(result["c"]!!.size == 1 && result["c"]!![0] == null)
    }

    @Test
    fun splitQuery_5params() {
        val url = "https://google.com.ua/oauth/authorize" +
                  "?client_id=SS" +
                  "&response_type=code" +
                  "&scope=N_FULL" +
                  "&access_type=offline" +
                  "&redirect_uri=http://localhost/Callback"

        val result = URL(url).splitQuery()

        assertTrue(result["client_id"]!!.size == 1 && result["client_id"]!![0] == "SS")
        assertTrue(result["response_type"]!!.size == 1 && result["response_type"]!![0] == "code")
        assertTrue(result["scope"]!!.size == 1 && result["scope"]!![0] == "N_FULL")
        assertTrue(result["access_type"]!!.size == 1 && result["access_type"]!![0] == "offline")
        assertTrue(result["redirect_uri"]!!.size == 1 && result["redirect_uri"]!![0] == "http://localhost/Callback")
    }

    @Test
    fun isValidURL_valid() {
        val url = "https://google.com"

        val result = url.isValidURL()

        assertTrue(result)
    }

    @Test
    fun isValidURL_valid2() {
        val url = "https://www.google.com"

        val result = url.isValidURL()

        assertTrue(result)
    }

    @Test
    fun isValidURL_invalid() {
        val url = "http://ProductDetail/2Thumbz, Inc.?source=Admin"

        val result = url.isValidURL()

        assertFalse(result)
    }

    @Test
    fun isValidURL_nullInput() {
        val url: String? = null

        val result = url.isValidURL()

        assertFalse(result)
    }

    @Test
    fun isValidURL_emptyInput() {
        val url = ""

        val result = url.isValidURL()

        assertFalse(result)
    }

    @Test
    fun isValidURL_noScheme() {
        val url = "www.google.com"

        val result = url.isValidURL()

        assertFalse(result)
    }

    @Test
    fun isValidURL_noScheme2() {
        val url = "google.com"

        val result = url.isValidURL()

        assertFalse(result)
    }

    @Test
    fun isValidURL_ftp() {
        val url = "ftp://ftp.is.co.za/rfc/rfc1808.txt"

        val result = url.isValidURL()

        assertTrue(result)
    }

    @Test
    fun isValidURL_mailTo() {
        val url = "mailto:mduerst@ifi.unizh.ch"

        val result = url.isValidURL()

        assertTrue(result)
    }

    @Test
    fun isValidURL_unknownScheme() {
        val url = "news:comp.infosystems.www.servers.unix"

        val result = url.isValidURL()

        assertFalse(result)
    }

    @Test
    fun isValidURL_unknownScheme2() {
        val url = "telnet://melvyl.ucop.edu/"

        val result = url.isValidURL()

        assertFalse(result)
    }

    @Test
    fun isValidURL_unknownScheme3() {
        val url = "gopher://spinaltap.micro.umn.edu/00/Weather/California/Los%20Angeles"

        val result = url.isValidURL()

        assertFalse(result)
    }


}