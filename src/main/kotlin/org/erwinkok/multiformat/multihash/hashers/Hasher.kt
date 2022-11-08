// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multihash.hashers

import org.erwinkok.result.Result

interface Hasher {
    fun write(p: ByteArray): Result<Int>
    fun sum(): ByteArray
    fun reset()
    fun size(): Int
    fun blockSize(): Int
}
