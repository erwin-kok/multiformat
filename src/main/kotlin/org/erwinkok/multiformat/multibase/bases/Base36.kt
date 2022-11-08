// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

@OptIn(ExperimentalUnsignedTypes::class)
object Base36 {
    private const val UcAlphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LcAlphabet = "0123456789abcdefghijklmnopqrstuvwxyz"
    private const val maxDigitOrdinal = 'z'
    private const val maxDigitValueB36 = 35.toByte()
    private val revAlphabet = ByteArray(maxDigitOrdinal.code + 1)

    init {
        for (i in revAlphabet.indices) {
            revAlphabet[i] = (maxDigitValueB36 + 1).toByte()
        }
        for ((i, c) in UcAlphabet.withIndex()) {
            revAlphabet[c.code] = i.toByte()
            if (c > '9') {
                revAlphabet[c.code + 32] = i.toByte()
            }
        }
    }

    fun encodeToStringUc(b: ByteArray): String {
        return encode(b, UcAlphabet)
    }

    fun encodeToStringLc(b: ByteArray): String {
        return encode(b, LcAlphabet)
    }

    private fun encode(data: ByteArray, alphabet: String): String {
        var zcount = 0
        while (zcount < data.size && data[zcount] == 0u.toByte()) {
            zcount++
        }

        var bufsz = zcount + (data.size - zcount) * 277 / 179 + 1
        val out = ByteArray(bufsz)

        var stopIdx = bufsz - 1
        for (b in data) {
            var idx = bufsz - 1
            var carry = b.toUByte().toUInt()
            while (idx > stopIdx || carry != 0u) {
                carry += 256u * out[idx].toUInt()
                out[idx] = (carry % 36u).toByte()
                carry /= 36u
                idx--
            }
            stopIdx = idx
        }

        var i = zcount
        while (i < bufsz && out[i] == 0.toByte()) {
            i++
        }

        bufsz = out.size - i + zcount
        for (j in 0 until bufsz) {
            val index = out[i - zcount + j].toInt()
            out[j] = alphabet[index].code.toByte()
        }
        return String(out, 0, bufsz)
    }

    fun decodeString(input: String): Result<ByteArray> {
        if (input.isEmpty()) {
            return Err("can not decode zero-length string")
        }

        var zcount = 0
        while (zcount < input.length && input[zcount] == '0') {
            zcount++
        }

        // the 32bit algo stretches the result up to 2 times
        val binu = ByteArray(2 * ((input.length * 179 / 277) + 1))
        val outi = UIntArray((input.length + 3) / 4)
        for (r in input) {
            if ((r > maxDigitOrdinal) || revAlphabet[r.code] > maxDigitValueB36) {
                return Err("invalid base36 digit ($r)")
            }
            var c = revAlphabet[r.code].toULong()
            for (j in outi.size - 1 downTo 0) {
                val t = (outi[j]).toULong() * 36u + c
                c = t shr 32
                outi[j] = (t and 0xffffffffu).toUInt()
            }
        }
        var mask = ((input.length % 4).toUInt() * 8u)
        if (mask == 0u) {
            mask = 32u
        }
        mask -= 8u

        var outidx = 0
        for (element in outi) {
            while (mask < 32u) { // loop relies on uint overflow
                binu[outidx] = (element shr mask.toInt()).toByte()
                mask -= 8u
                outidx++
            }
            mask = 24u
        }

        for (msb in zcount until outidx) {
            if (binu[msb].toUByte() > 0u) {
                return Ok(binu.copyOfRange(msb - zcount, outidx))
            }
        }
        return Ok(binu.copyOfRange(0, outidx))
    }
}
