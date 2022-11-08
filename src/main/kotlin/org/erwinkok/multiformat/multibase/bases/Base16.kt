// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.Result
import org.erwinkok.util.Hex

object Base16 {
    private val hexTableUppers = "0123456789ABCDEF".map { it.code.toByte() }

    fun encodeToStringUc(src: ByteArray): String {
        val dst = ByteArray(src.size * 2)
        hexEncodeUpper(dst, src)
        return String(dst)
    }

    private fun hexEncodeUpper(dst: ByteArray, src: ByteArray): Int {
        for ((i, v) in src.withIndex()) {
            dst[i * 2] = hexTableUppers[v.toInt() shr 4]
            dst[i * 2 + 1] = hexTableUppers[v.toInt() and 0x0f]
        }
        return src.size * 2
    }

    fun encodeToStringLc(data: ByteArray): String {
        return Hex.encode(data)
    }

    fun decode(data: String): Result<ByteArray> {
        return Hex.decode(data)
    }
}
