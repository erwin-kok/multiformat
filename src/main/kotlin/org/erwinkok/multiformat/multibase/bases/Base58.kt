// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

@OptIn(ExperimentalUnsignedTypes::class)
class Base58(private val encode: String) {
    val decode = ByteArray(128) { -1 }

    init {
        for ((i, v) in encode.withIndex()) {
            decode[v.code] = i.toByte()
        }
    }

    fun encode(data: ByteArray): String {
        var zcount = 0
        while (zcount < data.size && data[zcount] == 0.toByte()) {
            zcount++
        }

        var size = zcount + (data.size - zcount) * 555 / 406 + 1
        val encoded = ByteArray(size)

        var high = size - 1
        for (b in data) {
            var i = size - 1
            var carry = b.toUByte().toUInt()
            while (i > high || carry != 0u) {
                carry += 256u * encoded[i].toUInt()
                encoded[i] = (carry % 58u).toByte()
                carry /= 58u
                i--
            }
            high = i
        }

        var i = zcount
        while (i < size && encoded[i] == 0.toByte()) {
            i++
        }

        size = encoded.size - i + zcount
        for (j in 0 until size) {
            val index = encoded[i - zcount + j].toInt()
            encoded[j] = encode[index].code.toByte()
        }
        return String(encoded, 0, size)
    }

    fun decode(input: String): Result<ByteArray> {
        if (input.isEmpty()) {
            return Err("can not decode zero-length string")
        }
        val zero = encode[0]
        val b58sz = input.length

        var zcount = 0
        var i = 0
        while (i < b58sz && input[i] == zero) {
            zcount++
            i++
        }

        var t: ULong
        var c: ULong

        // the 32bit algo stretches the result up to 2 times
        val binu = ByteArray(2 * ((b58sz * 406 / 555) + 1))
        val outi = UIntArray((b58sz + 3) / 4)
        for (r in input) {
            if (r.code > 127) {
                return Err("high-bit set on invalid digit")
            }
            if (decode[r.code] == (-1).toByte()) {
                return Err("invalid base58 digit ($r)")
            }
            c = decode[r.code].toULong()
            var j = outi.size - 1
            while (j >= 0) {
                t = (outi[j]).toULong() * 58u + c
                c = t shr 32
                outi[j] = (t and 0xffffffffu).toUInt()
                j--
            }
        }
        var mask = ((b58sz % 4).toUInt() * 8u)
        if (mask == 0u) {
            mask = 32u
        }
        mask -= 8u

        var outLen = 0
        for (element in outi) {
            while (mask < 32u) { // loop relies on uint overflow
                binu[outLen] = (element shr mask.toInt()).toByte()
                mask -= 8u
                outLen++
            }
            mask = 24u
        }

        for (msb in zcount until binu.size) {
            if (binu[msb].toUByte() > 0u) {
                return Ok(binu.copyOfRange(msb - zcount, outLen))
            }
        }
        return Ok(binu.copyOfRange(0, outLen))
    }

    companion object {
        private val BtcAlphabet = Base58("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")
        private val FlickrAlphabet = Base58("123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ")

        fun encodeToStringBtc(data: ByteArray): String {
            return BtcAlphabet.encode(data)
        }

        fun decodeStringBtc(data: String): Result<ByteArray> {
            return BtcAlphabet.decode(data)
        }

        fun encodeToStringFlickr(data: ByteArray): String {
            return FlickrAlphabet.encode(data)
        }

        fun decodeStringFlickr(data: String): Result<ByteArray> {
            return FlickrAlphabet.decode(data)
        }
    }
}
