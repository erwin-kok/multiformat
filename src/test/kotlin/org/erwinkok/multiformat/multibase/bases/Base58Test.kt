package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.Random
import java.util.stream.Stream

internal class Base58Test {
    @TestFactory
    fun test2(): Stream<DynamicTest> {
        return listOf(
            "1QCaxc8hutpdZ62iKZsn1TCG3nh7uPZojq",
            "1DhRmSGnhPjUaVPAj48zgPV9e2oRhAQFUb",
            "17LN2oPYRYsXS9TdYdXCCDvF2FegshLDU2",
            "14h2bDLZSuvRFhUL45VjPHJcW667mmRAAn"
        ).map { str ->
            DynamicTest.dynamicTest("Test: $str") {
                val num = Base58.decodeStringBtc(str).expectNoErrors()
                val res = Base58.encodeToStringBtc(num)
                assertEquals(str, res)
            }
        }.stream()
    }

    @Test
    fun random() {
        val random = Random()
        for (j in 1 until 256) {
            val b = ByteArray(j)
            for (i in 0 until 100) {
                random.nextBytes(b)
                val inBtc = Base58.encodeToStringBtc(b)
                val outBtc = Base58.decodeStringBtc(inBtc).expectNoErrors()
                assertArrayEquals(b, outBtc)
                val inFlickr = Base58.encodeToStringFlickr(b)
                val outFlickr = Base58.decodeStringFlickr(inFlickr).expectNoErrors()
                assertArrayEquals(b, outFlickr)
            }
        }
    }

    @Test
    fun empty() {
        assertErrorResult("can not decode zero-length string") { Base58.decodeStringBtc("") }
    }

    @Test
    fun `invalid character`() {
        assertErrorResult("invalid base58 digit (*)") { Base58.decodeStringBtc("*&%^") }
    }

    @Test
    fun `high bit`() {
        assertErrorResult("high-bit set on invalid digit") { Base58.decodeStringBtc("😂") }
    }
}
