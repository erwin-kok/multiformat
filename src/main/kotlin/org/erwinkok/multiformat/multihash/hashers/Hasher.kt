package org.erwinkok.multiformat.multihash.hashers

import org.erwinkok.result.Result

interface Hasher {
    fun write(p: ByteArray): Result<Int>
    fun sum(): ByteArray
    fun reset()
    fun size(): Int
    fun blockSize(): Int
}
