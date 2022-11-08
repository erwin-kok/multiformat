package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.expectNoErrors
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Random

internal class Base36Test {
    @Test
    fun basicLc() {
        val enc = Base36.encodeToStringLc("Decentralize everything!!!".toByteArray())
        assertEquals("m552ng4dabi4neu1oo8l4i5mndwmpc3mkukwtxy9", enc)
        val res = Base36.decodeString(enc).expectNoErrors()
        assertArrayEquals("Decentralize everything!!!".toByteArray(), res)
    }

    @Test
    fun basicUc() {
        val enc = Base36.encodeToStringUc("Decentralize everything!!!".toByteArray())
        assertEquals("M552NG4DABI4NEU1OO8L4I5MNDWMPC3MKUKWTXY9", enc)
        val res = Base36.decodeString(enc).expectNoErrors()
        assertArrayEquals("Decentralize everything!!!".toByteArray(), res)
    }

    @Test
    fun permute() {
        val random = Random()
        val buf = ByteArray(137 + 16)
        for (i in 16 until buf.size) {
            buf[i] = random.nextInt().toByte()
        }
        for (i in buf.indices) {
            val newBuf = buf.copyOfRange(i, buf.size)
            val enc1 = Base36.encodeToStringLc(newBuf)
            val out1 = Base36.decodeString(enc1).expectNoErrors()
            assertArrayEquals(newBuf, out1)
            val enc2 = Base36.encodeToStringUc(newBuf)
            val out2 = Base36.decodeString(enc2).expectNoErrors()
            assertArrayEquals(newBuf, out2)
        }
    }
}
