// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.getOrElse
import kotlin.experimental.and

class Base32(private val encode: String, private val padChar: Char = StdPadding) {
    private val decodeMap = ByteArray(256) { 0xff.toByte() }

    init {
        for ((i, v) in encode.withIndex()) {
            this.decodeMap[asciiToLower(v)] = i.toByte()
            this.decodeMap[asciiToUpper(v)] = i.toByte()
        }
    }

    fun encodeToString(src: ByteArray): String {
        val dst = CharArray(encodedLen(src.size))
        if (src.isEmpty()) {
            return String(dst)
        }

        // Unpack 8x 5-bit source blocks into a 5 byte
        // destination quantum
        var srcIndex = 0
        var dstIndex = 0
        while (srcIndex < (src.size - 4)) {
            dst[dstIndex + 7] = encode[(src[srcIndex + 4] and 0x1F).toInt()]
            dst[dstIndex + 6] = encode[(src[srcIndex + 4].toUByte().toInt() ushr 5) or (src[srcIndex + 3].toUByte().toInt() shl 3) and 0x1F]
            dst[dstIndex + 5] = encode[(src[srcIndex + 3].toUByte().toInt() ushr 2) and 0x1F]
            dst[dstIndex + 4] = encode[(src[srcIndex + 3].toUByte().toInt() ushr 7) or (src[srcIndex + 2].toUByte().toInt() shl 1) and 0x1F]
            dst[dstIndex + 3] = encode[((src[srcIndex + 2].toUByte().toInt() ushr 4) or (src[srcIndex + 1].toUByte().toInt() shl 4)) and 0x1F]
            dst[dstIndex + 2] = encode[(src[srcIndex + 1].toUByte().toInt() ushr 1) and 0x1F]
            dst[dstIndex + 1] = encode[((src[srcIndex + 1].toUByte().toInt() ushr 6) or (src[srcIndex + 0].toUByte().toInt() shl 2)) and 0x1F]
            dst[dstIndex + 0] = encode[src[srcIndex + 0].toUByte().toInt() ushr 3]
            srcIndex += 5
            dstIndex += 8
        }
        var carry = 0
        val len = src.size - srcIndex
        if (len == 4) {
            dst[dstIndex + 6] = encode[(src[3 + srcIndex].toUByte().toInt() shl 3) and 0x1F]
            dst[dstIndex + 5] = encode[(src[3 + srcIndex].toUByte().toInt() ushr 2) and 0x1F]
            carry = src[3 + srcIndex].toUByte().toInt() ushr 7
        }
        if (len >= 3) {
            dst[dstIndex + 4] = encode[carry or (src[2 + srcIndex].toUByte().toInt() shl 1) and 0x1F]
            carry = (src[2 + srcIndex].toUByte().toInt() ushr 4) and 0x1F
        }
        if (len >= 2) {
            dst[dstIndex + 3] = encode[carry or (src[1 + srcIndex].toUByte().toInt() shl 4) and 0x1F]
            dst[dstIndex + 2] = encode[(src[1 + srcIndex].toUByte().toInt() ushr 1) and 0x1F]
            carry = (src[1 + srcIndex].toUByte().toInt() ushr 6) and 0x1F
        }
        if (len >= 1) {
            dst[dstIndex + 1] = encode[carry or (src[0 + srcIndex].toUByte().toInt() shl 2) and 0x1F]
            dst[dstIndex + 0] = encode[src[0 + srcIndex].toUByte().toInt() ushr 3]
        }
        if (len > 0 && padChar != NoPadding) {
            dst[dstIndex + 7] = padChar
            if (len < 4) {
                dst[dstIndex + 6] = padChar
                dst[dstIndex + 5] = padChar
                if (len < 3) {
                    dst[dstIndex + 4] = padChar
                    if (len < 2) {
                        dst[dstIndex + 3] = padChar
                        dst[dstIndex + 2] = padChar
                    }
                }
            }
        }
        return String(dst)
    }

    private fun encodedLen(size: Int): Int {
        if (padChar == NoPadding) {
            return (size * 8 + 4) / 5 // minimum # chars at 5 bits per char
        }
        return (size + 4) / 5 * 8
    }

    private fun decodeInPlace(strb: ByteArray): Result<Int> {
        var off = 0
        for (b in strb) {
            if (b != '\n'.code.toByte() && b != '\r'.code.toByte()) {
                strb[off] = b
                off++
            }
        }
        return decode(strb, strb.copyOfRange(0, off))
    }

    private fun decode(dst: ByteArray, src: ByteArray): Result<Int> {
        val olen = src.size
        var end = false
        var srcIndex = 0
        var dstIndex = 0
        var n = 0
        var left = src.size
        while (left > 0 && !end) {
            val dbuf = ByteArray(8)
            var dlen = 8
            var j = 0
            while (j < 8) {
                if (left == 0) {
                    if (padChar != NoPadding) {
                        return Err("illegal base32 data at input byte ${olen - left - j}")
                    }
                    dlen = j
                    break
                }
                val inp = src[srcIndex]
                srcIndex++
                left--
                if (inp == padChar.code.toByte() && j >= 2 && left < 8) {
                    if (padChar == NoPadding) {
                        return Err("illegal base32 data at input byte $olen")
                    }
                    // We've reached the end and there's padding
                    if (left + j < 8 - 1) {
                        // not enough padding
                        return Err("illegal base32 data at input byte $olen")
                    }
                    for (k in 0 until 8 - 1 - j) {
                        if (left > k && src[srcIndex + k] != padChar.code.toByte()) {
                            // incorrect padding
                            return Err("illegal base32 data at input byte ${olen - left + k - 1}")
                        }
                    }
                    dlen = j
                    end = true
                    // 7, 5 and 2 are not valid padding lengths, and so 1, 3 and 6 are not
                    // valid dlen values. See RFC 4648 Section 6 "Base 32 Encoding" listing
                    // the five valid padding lengths, and Section 9 "Illustrations and
                    // Examples" for an illustration for how the 1st, 3rd and 6th base32
                    // src bytes do not yield enough information to decode a dst byte.
                    if (dlen == 1 || dlen == 3 || dlen == 6) {
                        return Err("illegal base32 data at input byte ${olen - left - 1}")
                    }
                    break
                }
                dbuf[j] = decodeMap[inp.toInt()]
                if (dbuf[j] == 0xFF.toByte()) {
                    return Err("illegal base32 data at input byte ${olen - left - 1}")
                }
                j++
            }
            // Pack 8x 5-bit source blocks into 5 byte destination
            // quantum
            if (dlen == 8) {
                dst[dstIndex + 4] = ((dbuf[6].toUByte().toInt() shl 5) or (dbuf[7].toInt())).toByte()
            }
            if (dlen >= 7) {
                dst[dstIndex + 3] = ((dbuf[4].toUByte().toInt() shl 7) or (dbuf[5].toUByte().toInt() shl 2) or (dbuf[6].toUByte().toInt() ushr 3)).toByte()
            }
            if (dlen >= 5) {
                dst[dstIndex + 2] = ((dbuf[3].toUByte().toInt() shl 4) or (dbuf[4].toUByte().toInt() ushr 1)).toByte()
            }
            if (dlen >= 4) {
                dst[dstIndex + 1] = ((dbuf[1].toUByte().toInt() shl 6) or (dbuf[2].toUByte().toInt() shl 1) or (dbuf[3].toUByte().toInt() ushr 4)).toByte()
            }
            if (dlen >= 2) {
                dst[dstIndex + 0] = ((dbuf[0].toUByte().toInt() shl 3) or (dbuf[1].toUByte().toInt() ushr 2)).toByte()
            }
            if ((dst.size - dstIndex) > 5) {
                dstIndex += 5
            }
            when (dlen) {
                2 -> n += 1
                4 -> n += 2
                5 -> n += 3
                7 -> n += 4
                8 -> n += 5
            }
        }
        return Ok(n)
    }

    fun decodeString(s: String): Result<ByteArray> {
        val strb = s.toByteArray()
        val n = decodeInPlace(strb)
            .getOrElse { return Err(it) }
        return Ok(strb.copyOfRange(0, n))
    }

    private fun asciiToLower(c: Char): Int {
        if (c in 'A'..'Z') {
            return c.code + 32
        }
        return c.code
    }

    private fun asciiToUpper(c: Char): Int {
        if (c in 'a'..'z') {
            return c.code - 32
        }
        return c.code
    }

    companion object {
        private const val StdPadding = '='
        private const val NoPadding = (-1).toChar()

        private const val alphabetStdLower = "abcdefghijklmnopqrstuvwxyz234567"
        private const val alphabetStdUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        private const val alphabetHexLower = "0123456789abcdefghijklmnopqrstuv"
        private const val alphabetHexUpper = "0123456789ABCDEFGHIJKLMNOPQRSTUV"
        private const val alphabetZLower = "ybndrfg8ejkmcpqxot1uwisza345h769"
        private const val alphabetZUpper = "YBNDRFG8EJKMCPQXOT1UWISZA345H769"

        private val base32StdLowerPad = Base32(alphabetStdLower)
        private val base32StdLowerNoPad = Base32(alphabetStdLower, NoPadding)
        private val base32StdUpperPad = Base32(alphabetStdUpper)
        private val base32StdUpperNoPad = Base32(alphabetStdUpper, NoPadding)
        private val base32HexLowerPad = Base32(alphabetHexLower)
        private val base32HexLowerNoPad = Base32(alphabetHexLower, NoPadding)
        private val base32HexUpperPad = Base32(alphabetHexUpper)
        private val base32HexUpperNoPad = Base32(alphabetHexUpper, NoPadding)
        private val base32ZLowerNoPad = Base32(alphabetZLower, NoPadding)
        private val base32ZUpperNoPad = Base32(alphabetZUpper, NoPadding)

        fun encodeStdLowerPad(data: ByteArray) = base32StdLowerPad.encodeToString(data)
        fun encodeStdLowerNoPad(data: ByteArray) = base32StdLowerNoPad.encodeToString(data)
        fun encodeStdUpperPad(data: ByteArray) = base32StdUpperPad.encodeToString(data)
        fun encodeStdUpperNoPad(data: ByteArray) = base32StdUpperNoPad.encodeToString(data)
        fun encodeHexLowerPad(data: ByteArray) = base32HexLowerPad.encodeToString(data)
        fun encodeHexLowerNoPad(data: ByteArray) = base32HexLowerNoPad.encodeToString(data)
        fun encodeHexUpperPad(data: ByteArray) = base32HexUpperPad.encodeToString(data)
        fun encodeHexUpperNoPad(data: ByteArray) = base32HexUpperNoPad.encodeToString(data)
        fun encodeZ(data: ByteArray) = base32ZLowerNoPad.encodeToString(data)
        fun decodeStdPad(data: String) = base32StdUpperPad.decodeString(data)
        fun decodeStdNoPad(data: String) = base32StdUpperNoPad.decodeString(data)
        fun decodeHexPad(data: String) = base32HexUpperPad.decodeString(data)
        fun decodeHexNoPad(data: String) = base32HexUpperNoPad.decodeString(data)
        fun decodeZ(data: String) = base32ZUpperNoPad.decodeString(data)
    }
}
