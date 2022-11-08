// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class Base64(private val encode: String, private val padChar: Char = StdPadding) {
    private val decodeMap = ByteArray(256) { 0xff.toByte() }

    init {
        for ((i, v) in encode.withIndex()) {
            this.decodeMap[v.code] = i.toByte()
        }
    }

    fun encodeToString(src: ByteArray): String {
        val dst = CharArray(encodedLen(src.size))
        if (src.isEmpty()) {
            return String(dst)
        }
        var di = 0
        var si = 0
        val n = (src.size / 3) * 3
        while (si < n) {
            // Convert 3x 8bit source bytes into 4 bytes
            val v = ((src[si + 0]).toUByte().toUInt() shl 16) or ((src[si + 1]).toUByte().toUInt() shl 8) or (src[si + 2]).toUByte().toUInt()

            dst[di + 0] = encode[((v shr 18) and 0x3Fu).toInt()]
            dst[di + 1] = encode[((v shr 12) and 0x3Fu).toInt()]
            dst[di + 2] = encode[((v shr 6) and 0x3Fu).toInt()]
            dst[di + 3] = encode[(v and 0x3Fu).toInt()]

            si += 3
            di += 4
        }
        val remain = src.size - si
        if (remain == 0) {
            return String(dst)
        }
        // Add the remaining small block
        var v = (src[si + 0]).toUByte().toUInt() shl 16
        if (remain == 2) {
            v = v or ((src[si + 1]).toUByte().toUInt() shl 8)
        }
        dst[di + 0] = encode[((v shr 18) and 0x3Fu).toInt()]
        dst[di + 1] = encode[((v shr 12) and 0x3Fu).toInt()]
        when (remain) {
            2 -> {
                dst[di + 2] = encode[((v shr 6) and 0x3Fu).toInt()]
                if (padChar != NoPadding) {
                    dst[di + 3] = padChar
                }
            }

            1 -> {
                if (padChar != NoPadding) {
                    dst[di + 2] = padChar
                    dst[di + 3] = padChar
                }
            }
        }
        return String(dst)
    }

    private fun encodedLen(size: Int): Int {
        if (padChar == NoPadding) {
            return (size * 8 + 5) / 6 // minimum # chars at 5 bits per char
        }
        return (size + 2) / 3 * 4
    }

    fun decodeString(data: String): Result<ByteArray> {
        var end = data.length
        while (data[end - 1] == '=') {
            --end
        }
        val out = ByteArray(end * 6 / 8)
        var bits = 0
        var buffer = 0
        var written = 0
        for (i in 0 until end) {
            val r = data[i]
            if (r.code > 127) {
                return Err("high-bit set on invalid digit")
            }
            if (decodeMap[r.code] == (-1).toByte()) {
                return Err("invalid alphabet digit ($r)")
            }
            val value = decodeMap[r.code].toInt()
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[written++] = (0xff and (buffer shr bits)).toByte()
            }
        }
        if (bits >= 6 || 0xff and (buffer shl (8 - bits)) != 0) {
            return Err("Unexpected end of data")
        }
        return Ok(out)
    }

    companion object {
        private const val StdPadding = '='
        private const val NoPadding = (-1).toChar()

        private const val alphabetStd = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        private const val alphabetUrl = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

        private var StdEncoding = Base64(alphabetStd)
        private var URLEncoding = Base64(alphabetUrl)
        private var RawStdEncoding = Base64(alphabetStd, NoPadding)
        private var RawURLEncoding = Base64(alphabetUrl, NoPadding)

        fun encodeToStringStd(data: ByteArray): String {
            return RawStdEncoding.encodeToString(data)
        }

        fun encodeToStringUrl(data: ByteArray): String {
            return RawURLEncoding.encodeToString(data)
        }

        fun encodeToStringPad(data: ByteArray): String {
            return StdEncoding.encodeToString(data)
        }

        fun encodeToStringUrlPad(data: ByteArray): String {
            return URLEncoding.encodeToString(data)
        }

        fun decodeStringStd(data: String): Result<ByteArray> {
            return RawStdEncoding.decodeString(data)
        }

        fun decodeStringUrl(data: String): Result<ByteArray> {
            return RawURLEncoding.decodeString(data)
        }

        fun decodeStringPad(data: String): Result<ByteArray> {
            return StdEncoding.decodeString(data)
        }

        fun decodeStringUrlPad(data: String): Result<ByteArray> {
            return URLEncoding.decodeString(data)
        }
    }
}
