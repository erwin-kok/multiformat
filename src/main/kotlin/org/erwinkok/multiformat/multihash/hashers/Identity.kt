// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multihash.hashers

import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class Identity : Hasher {
    private var buf = byteArrayOf()

    override fun write(p: ByteArray): Result<Int> {
        buf = concatenate(buf, p)
        return Ok(p.size)
    }

    override fun sum(): ByteArray {
        return buf
    }

    override fun reset() {
        buf = byteArrayOf()
    }

    override fun size(): Int {
        return buf.size
    }

    override fun blockSize(): Int {
        return 32
    }

    private fun concatenate(a: ByteArray, b: ByteArray): ByteArray {
        val r = ByteArray(a.size + b.size)
        System.arraycopy(a, 0, r, 0, a.size)
        System.arraycopy(b, 0, r, a.size, b.size)
        return r
    }
}
