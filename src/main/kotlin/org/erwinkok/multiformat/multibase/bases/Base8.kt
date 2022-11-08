package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

object Base8 {
    private const val encode = "01234567"
    private val decode = ByteArray(128) { -1 }

    init {
        for (i in encode.indices) {
            decode[encode[i].code] = i.toByte()
        }
    }

    fun encode(data: ByteArray): String {
        val out = StringBuilder()
        var bits = 0
        var buffer = 0
        for (datum in data) {
            // Slurp data into the buffer:
            buffer = (buffer shl 8) or datum.toUByte().toInt()
            bits += 8

            // Write out as much as we can:
            while (bits > 3) {
                bits -= 3
                val index = (buffer shr bits) and 7
                out.append(encode[index])
            }
        }

        // Partial character:
        if (bits > 0) {
            val index = (buffer shl (3 - bits)) and 7
            out.append(encode[index])
        }
        return out.toString()
    }

    fun decode(data: String): Result<ByteArray> {
        if (data.isEmpty()) {
            return Err("can not decode zero-length string")
        }

        // Count the padding bytes:
        val end = data.length

        // Allocate the output:
        val out = ByteArray(end * 3 / 8)

        // Parse the data:
        var bits = 0 // Number of bits currently in the buffer
        var buffer = 0 // Bits waiting to be written out, MSB first
        var written = 0 // Next byte to write
        for (i in 0 until end) {
            val r = data[i]
            if (r.code > 127) {
                return Err("high-bit set on invalid digit")
            }
            if (decode[r.code] == (-1).toByte()) {
                return Err("invalid alphabet digit ($r)")
            }

            // Read one character from the string:
            val value = decode[r.code].toInt()
            // Append the bits to the buffer:
            buffer = buffer shl 3 or value
            bits += 3
            // Write out some bits if the buffer has a byte's worth:
            if (bits >= 8) {
                bits -= 8
                out[written++] = (0xff and (buffer shr bits)).toByte()
            }
        }
        // Verify that we have received just enough bits:
        if (bits >= 3 || 0xff and (buffer shl 8 - bits) != 0) {
            return Err("Unexpected end of data")
        }
        return Ok(out)
    }
}
