// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

object Base10 {
    fun encode(data: ByteArray): String {
        if (data.isEmpty()) {
            return ""
        }
        var zeros = 0
        while (zeros < data.size && data[zeros].toUInt() == 0u) {
            zeros++
        }
        val sb = StringBuilder()
        var index = zeros
        val cdata = data.copyOf()
        while (index < cdata.size) {
            sb.append('0' + encode(cdata, index))
            if (cdata[index].toInt() == 0) {
                index++
            }
        }
        return "0".repeat(zeros) + sb.toString().reversed()
    }

    fun decode(input: String): Result<ByteArray> {
        if (input.isEmpty()) {
            return Err("can not decode zero-length string")
        }
        var zeros = 0
        while (zeros < input.length && input[zeros] == '0') {
            zeros++
        }
        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            if (c < '0' || c > '9') {
                return Err("InvalidCharacter in base 10")
            }
            input58[i] = (c.code - '0'.code).toByte()
        }
        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var index = zeros
        while (index < input.length) {
            decoded[--outputStart] = decode(input58, index)
            if (input58[index].toInt() == 0) {
                index++
            }
        }
        // Ignore extra leading zeroes that were added during the calculation.
        while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) {
            ++outputStart
        }
        // Return decoded data (including original number of leading zeros).
        return Ok(decoded.copyOfRange(outputStart - zeros, decoded.size))
    }

    private fun encode(data: ByteArray, index: Int): Int {
        var remainder = 0
        for (i in index until data.size) {
            val temp = remainder * 256 + data[i].toUByte().toInt()
            data[i] = (temp / 10).toByte()
            remainder = temp % 10
        }
        return remainder
    }

    private fun decode(data: ByteArray, index: Int): Byte {
        var remainder = 0
        for (i in index until data.size) {
            val temp = remainder * 10 + data[i].toUByte().toInt()
            data[i] = (temp / 256).toByte()
            remainder = temp % 256
        }
        return remainder.toByte()
    }
}
