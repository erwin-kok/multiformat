// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
@file:OptIn(ExperimentalUnsignedTypes::class)

package org.erwinkok.multiformat.varint

import org.erwinkok.multiformat.util.UVarInt
import org.erwinkok.multiformat.util.readUnsignedVarInt
import org.erwinkok.multiformat.util.writeUnsignedVarInt
import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal class UVarIntTest {
    @Test
    fun check16() {
        val stream = ByteArrayOutputStream()
        val n = stream.writeUnsignedVarInt((1u shl (16u - 1u).toInt()).toULong()).expectNoErrors()
        assertEquals(UVarInt.MaxVarintLen16, n)
    }

    @Test
    fun check32() {
        val stream = ByteArrayOutputStream()
        val n = stream.writeUnsignedVarInt((1u shl (32u - 1u).toInt()).toULong()).expectNoErrors()
        assertEquals(UVarInt.MaxVarintLen32, n)
    }

    @Test
    fun len64() {
        val nlz = nlz()
        for (i in 0 until 256) {
            val len = 8 - nlz[i]
            for (k in 0 until (64 - 8)) {
                val x = i.toULong() shl k
                var want = 0
                if (x != 0uL) {
                    want = len + k
                }
                if (x <= (1uL shl (64 - 1))) {
                    val got = len64(x)
                    assertEquals(want, got, "Mismatch for: $i")
                }
            }
        }
    }

    @Test
    fun size() {
        for (i in 0uL..(1uL shl 16)) {
            val outputStream = ByteArrayOutputStream()
            val expected = outputStream.writeUnsignedVarInt(i).expectNoErrors()
            val size = uvarintSize(i)
            assertEquals(expected, size, "Mismatch for $i")
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val xi = inputStream.readUnsignedVarInt().expectNoErrors()
            assertEquals(i, xi)
        }
    }

    @Test
    fun overflow() {
        assertErrorResult("varints larger than uint63 not supported") {
            val stream = ByteArrayInputStream(
                byteArrayOf(
                    0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
                    0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
                    0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x00,
                ),
            )
            stream.readUnsignedVarInt()
        }
    }

    @Test
    fun `not minimal`() {
        assertErrorResult("varint not minimally encoded") {
            val stream = ByteArrayInputStream(byteArrayOf(0x81.toByte(), 0x00.toByte()))
            stream.readUnsignedVarInt()
        }
    }

    @Test
    fun `end of stream`() {
        assertErrorResult("EndOfStream") {
            val stream = ByteArrayInputStream(byteArrayOf())
            stream.readUnsignedVarInt()
        }
    }

    @Test
    fun `unexpected end of stream`() {
        assertErrorResult("UnexpectedEndOfStream") {
            val stream = ByteArrayInputStream(byteArrayOf(0x81.toByte(), 0x81.toByte()))
            stream.readUnsignedVarInt()
        }
    }

    private fun nlz(): IntArray {
        val nlz = IntArray(256)
        nlz[0] = 0
        for (i in 1 until 256) {
            var x = i
            var n = 0
            while ((x and 0x80) == 0) {
                n++
                x = x shl 1
            }
            nlz[i] = n
        }
        return nlz
    }

    private fun uvarintSize(num: ULong): Int {
        val bits = len64(num)
        val q = bits / 7
        val r = bits % 7
        var size = q
        if (r > 0 || size == 0) {
            size++
        }
        return size
    }

    private fun len64(x: ULong): Int {
        var n = 0
        var vx = x
        if (vx >= (1uL shl 32)) {
            vx = vx shr 32
            n = 32
        }
        if (vx >= (1uL shl 16)) {
            vx = vx shr 16
            n += 16
        }
        if (vx >= (1uL shl 8)) {
            vx = vx shr 8
            n += 8
        }
        return n + len8tab[vx.toInt()].toInt()
    }

    private val len8tab = ubyteArrayOf(
        0x00U, 0x01U, 0x02U, 0x02U, 0x03U, 0x03U, 0x03U, 0x03U, 0x04U, 0x04U, 0x04U, 0x04U, 0x04U, 0x04U, 0x04U, 0x04U,
        0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U, 0x05U,
        0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U,
        0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U, 0x06U,
        0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U,
        0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U,
        0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U,
        0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U, 0x07U,
        0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U,
        0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U,
        0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U,
        0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U,
        0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U,
        0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U,
        0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U,
        0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U, 0x08U,
    )
}
