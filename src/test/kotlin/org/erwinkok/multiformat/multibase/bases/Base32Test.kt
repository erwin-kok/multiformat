package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.erwinkok.util.Tuple2
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.Random
import java.util.stream.Stream

internal class Base32Test {
    private val pairsUpper = listOf(
        // Tuple2("", ""),
        Tuple2("f", "MY======"),
        Tuple2("fo", "MZXQ===="),
        Tuple2("foo", "MZXW6==="),
        Tuple2("foob", "MZXW6YQ="),
        Tuple2("fooba", "MZXW6YTB"),
        Tuple2("foobar", "MZXW6YTBOI======"),

        // Wikipedia examples, converted to base32
        Tuple2("sure.", "ON2XEZJO"),
        Tuple2("sure", "ON2XEZI="),
        Tuple2("sur", "ON2XE==="),
        Tuple2("su", "ON2Q===="),
        Tuple2("leasure.", "NRSWC43VOJSS4==="),
        Tuple2("easure.", "MVQXG5LSMUXA===="),
        Tuple2("asure.", "MFZXK4TFFY======"),
        Tuple2("sure.", "ON2XEZJO")
    )

    @TestFactory
    fun encode(): Stream<DynamicTest> {
        return pairsUpper.map { (decoded, encoded) ->
            DynamicTest.dynamicTest("Test $decoded") {
                val got = Base32.encodeStdUpperPad(decoded.toByteArray())
                assertEquals(encoded, got)
            }
        }.stream()
    }

    @TestFactory
    fun decode(): Stream<DynamicTest> {
        return pairsUpper.map { (decoded, encoded) ->
            DynamicTest.dynamicTest("Test $decoded") {
                val got = Base32.decodeStdPad(encoded).expectNoErrors()
                assertEquals(decoded, String(got))
            }
        }.stream()
    }

    @TestFactory
    fun decodeCorrupt(): Stream<DynamicTest> {
        return listOf(
            Tuple2("", -1),
            Tuple2("!!!!", 0),
            Tuple2("!===", 0),
            Tuple2("AA=A====", 2),
            Tuple2("AAA=AAAA", 3),
            Tuple2("MMMMMMMMM", 8),
            Tuple2("MMMMMM", 0),
            Tuple2("A=", 1),
            Tuple2("AA=", 3),
            Tuple2("AA==", 4),
            Tuple2("AA===", 5),
            Tuple2("AAAA=", 5),
            Tuple2("AAAA==", 6),
            Tuple2("AAAAA=", 6),
            Tuple2("AAAAA==", 7),
            Tuple2("A=======", 1),
            Tuple2("AA======", -1),
            Tuple2("AAA=====", 3),
            Tuple2("AAAA====", -1),
            Tuple2("AAAAA===", -1),
            Tuple2("AAAAAA==", 6),
            Tuple2("AAAAAAA=", -1),
            Tuple2("AAAAAAAA", -1)
        ).map { (inp, offset) ->
            DynamicTest.dynamicTest("Test $inp") {
                val result = Base32.decodeStdPad(inp)
                if (offset > -1) {
                    assertErrorResult("illegal base32 data at input byte $offset") { result }
                } else {
                    result.expectNoErrors()
                }
            }
        }.stream()
    }

    @TestFactory
    fun newLine(): Stream<DynamicTest> {
        return listOf(
            Tuple2("ON2XEZI=", "sure"),
            Tuple2("ON2XEZI=\r", "sure"),
            Tuple2("ON2XEZI=\n", "sure"),
            Tuple2("ON2XEZI=\r\n", "sure"),
            Tuple2("ON2XEZ\r\nI=", "sure"),
            Tuple2("ON2X\rEZ\nI=", "sure"),
            Tuple2("ON2X\nEZ\rI=", "sure"),
            Tuple2("ON2XEZ\nI=", "sure"),
            Tuple2("ON2XEZI\n=", "sure"),
            Tuple2("MZXW6YTBOI======", "foobar"),
            Tuple2("MZXW6YTBOI=\r\n=====", "foobar")
        ).map { (inp, expected) ->
            DynamicTest.dynamicTest("Test $inp") {
                val result = Base32.decodeStdPad(inp).expectNoErrors()
                assertEquals(expected, String(result))
            }
        }.stream()
    }

    @TestFactory
    fun noPadding(): Stream<DynamicTest> {
        return listOf(
            "a",
            "ab",
            "abc",
            "abcd",
            "abcde",
            "",
            String(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        ).map { inp ->
            DynamicTest.dynamicTest("Test $inp") {
                val enc = Base32.encodeStdUpperNoPad(inp.toByteArray())
                assertFalse(enc.contains("="))
                val out = Base32.decodeStdNoPad(enc).expectNoErrors()
                assertEquals(inp, String(out))
            }
        }.stream()
    }

    @Test
    fun noPaddingRand() {
        val rand = Random()
        for (i in 0 until 1000) {
            val l = rand.nextInt(1024)
            val buf = ByteArray(l)
            rand.nextBytes(buf)
            val enc = Base32.encodeStdUpperNoPad(buf)
            assertFalse(enc.contains("="))
            val out = Base32.decodeStdNoPad(enc).expectNoErrors()
            assertArrayEquals(buf, out)
        }
    }
}
