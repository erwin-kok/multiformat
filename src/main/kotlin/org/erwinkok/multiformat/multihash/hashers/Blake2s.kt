@file:OptIn(ExperimentalUnsignedTypes::class)

package org.erwinkok.multiformat.multihash.hashers

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class Blake2s private constructor(key: ByteArray?) : Hasher {
    private var h = UIntArray(8)
    private var c = UIntArray(2)
    private var block = ByteArray(BlockSize)
    private var offset: Int = 0
    private var key = ByteArray(BlockSize)
    private var keyLen: Int = 0

    init {
        if (key != null) {
            this.keyLen = key.size
            System.arraycopy(key, 0, this.key, 0, key.size)
        }
        reset()
    }

    override fun write(p: ByteArray): Result<Int> {
        val n = p.size
        var index = 0
        if (offset > 0) {
            val remaining = BlockSize - offset
            if (n <= remaining) {
                System.arraycopy(p, 0, block, offset, p.size)
                offset += p.size
                return Ok(n)
            }
            System.arraycopy(p, 0, block, offset, remaining)
            hashBlocks(h, c, 0u, block, 0, block.size)
            offset = 0
            index = remaining
        }
        val length = p.size - index
        if (length > BlockSize) {
            var nn = length and (BlockSize - 1).inv()
            if (length == nn) {
                nn -= BlockSize
            }
            hashBlocks(h, c, 0u, p, index, nn)
            index += nn
        }
        System.arraycopy(p, index, block, 0, p.size - index)
        offset += (p.size - index)
        return Ok(n)
    }

    override fun sum(): ByteArray {
        val hash = ByteArray(Size)
        finalize(hash)
        return hash
    }

    override fun reset() {
        for (i in iv.indices) {
            h[i] = iv[i]
        }
        h[0] = h[0] xor (Size.toUInt() or (keyLen.toUInt() shl 8) or (1u shl 16) or (1u shl 24))
        offset = 0
        c[0] = 0u
        c[1] = 0u
        if (keyLen > 0) {
            System.arraycopy(key, 0, block, 0, key.size)
            offset = BlockSize
        }
    }

    override fun size(): Int {
        return Size
    }

    override fun blockSize(): Int {
        return BlockSize
    }

    private fun finalize(hash: ByteArray) {
        val blk = ByteArray(BlockSize)
        System.arraycopy(block, 0, blk, 0, offset)
        val remaining = (BlockSize - offset).toUInt()
        val vc = c.copyOf()
        if (vc[0] < remaining) {
            vc[1]--
        }
        vc[0] -= remaining
        val vh = h.copyOf()
        hashBlocks(vh, vc, 0xFFFFFFFFu, blk, 0, blk.size)
        for ((i, v) in vh.withIndex()) {
            hash[4 * i + 0] = (v and 0xffu).toByte()
            hash[4 * i + 1] = ((v shr 8) and 0xffu).toByte()
            hash[4 * i + 2] = ((v shr 16) and 0xffu).toByte()
            hash[4 * i + 3] = ((v shr 24) and 0xffu).toByte()
        }
    }

    private fun hashBlocks(h: UIntArray, c: UIntArray, flag: UInt, blocks: ByteArray, index: Int, size: Int) {
        val m = UIntArray(16)
        var c0 = c[0]
        var c1 = c[1]
        var i = index
        while (i < size) {
            c0 += BlockSize.toUInt()
            if (c0 < BlockSize.toUInt()) {
                c1++
            }
            var v0 = h[0]
            var v1 = h[1]
            var v2 = h[2]
            var v3 = h[3]
            var v4 = h[4]
            var v5 = h[5]
            var v6 = h[6]
            var v7 = h[7]
            var v8 = iv[0]
            var v9 = iv[1]
            var v10 = iv[2]
            var v11 = iv[3]
            var v12 = iv[4] xor c0
            var v13 = iv[5] xor c1
            var v14 = iv[6] xor flag
            var v15 = iv[7]
            for (j in m.indices) {
                m[j] = blocks[i].toUByte().toUInt() or (blocks[i + 1].toUByte().toUInt() shl 8) or (blocks[i + 2].toUByte().toUInt() shl 16) or (blocks[i + 3].toUByte().toUInt() shl 24)
                i += 4
            }
            for (s in precomputed) {
                v0 += m[s[0].toInt()]
                v0 += v4
                v12 = v12 xor v0
                v12 = rotateLeft32(v12, -16)
                v8 += v12
                v4 = v4 xor v8
                v4 = rotateLeft32(v4, -12)
                v1 += m[s[1].toInt()]
                v1 += v5
                v13 = v13 xor v1
                v13 = rotateLeft32(v13, -16)
                v9 += v13
                v5 = v5 xor v9
                v5 = rotateLeft32(v5, -12)
                v2 += m[s[2].toInt()]
                v2 += v6
                v14 = v14 xor v2
                v14 = rotateLeft32(v14, -16)
                v10 += v14
                v6 = v6 xor v10
                v6 = rotateLeft32(v6, -12)
                v3 += m[s[3].toInt()]
                v3 += v7
                v15 = v15 xor v3
                v15 = rotateLeft32(v15, -16)
                v11 += v15
                v7 = v7 xor v11
                v7 = rotateLeft32(v7, -12)

                v0 += m[s[4].toInt()]
                v0 += v4
                v12 = v12 xor v0
                v12 = rotateLeft32(v12, -8)
                v8 += v12
                v4 = v4 xor v8
                v4 = rotateLeft32(v4, -7)
                v1 += m[s[5].toInt()]
                v1 += v5
                v13 = v13 xor v1
                v13 = rotateLeft32(v13, -8)
                v9 += v13
                v5 = v5 xor v9
                v5 = rotateLeft32(v5, -7)
                v2 += m[s[6].toInt()]
                v2 += v6
                v14 = v14 xor v2
                v14 = rotateLeft32(v14, -8)
                v10 += v14
                v6 = v6 xor v10
                v6 = rotateLeft32(v6, -7)
                v3 += m[s[7].toInt()]
                v3 += v7
                v15 = v15 xor v3
                v15 = rotateLeft32(v15, -8)
                v11 += v15
                v7 = v7 xor v11
                v7 = rotateLeft32(v7, -7)

                v0 += m[s[8].toInt()]
                v0 += v5
                v15 = v15 xor v0
                v15 = rotateLeft32(v15, -16)
                v10 += v15
                v5 = v5 xor v10
                v5 = rotateLeft32(v5, -12)
                v1 += m[s[9].toInt()]
                v1 += v6
                v12 = v12 xor v1
                v12 = rotateLeft32(v12, -16)
                v11 += v12
                v6 = v6 xor v11
                v6 = rotateLeft32(v6, -12)
                v2 += m[s[10].toInt()]
                v2 += v7
                v13 = v13 xor v2
                v13 = rotateLeft32(v13, -16)
                v8 += v13
                v7 = v7 xor v8
                v7 = rotateLeft32(v7, -12)
                v3 += m[s[11].toInt()]
                v3 += v4
                v14 = v14 xor v3
                v14 = rotateLeft32(v14, -16)
                v9 += v14
                v4 = v4 xor v9
                v4 = rotateLeft32(v4, -12)

                v0 += m[s[12].toInt()]
                v0 += v5
                v15 = v15 xor v0
                v15 = rotateLeft32(v15, -8)
                v10 += v15
                v5 = v5 xor v10
                v5 = rotateLeft32(v5, -7)
                v1 += m[s[13].toInt()]
                v1 += v6
                v12 = v12 xor v1
                v12 = rotateLeft32(v12, -8)
                v11 += v12
                v6 = v6 xor v11
                v6 = rotateLeft32(v6, -7)
                v2 += m[s[14].toInt()]
                v2 += v7
                v13 = v13 xor v2
                v13 = rotateLeft32(v13, -8)
                v8 += v13
                v7 = v7 xor v8
                v7 = rotateLeft32(v7, -7)
                v3 += m[s[15].toInt()]
                v3 += v4
                v14 = v14 xor v3
                v14 = rotateLeft32(v14, -8)
                v9 += v14
                v4 = v4 xor v9
                v4 = rotateLeft32(v4, -7)
            }
            h[0] = h[0] xor (v0 xor v8)
            h[1] = h[1] xor (v1 xor v9)
            h[2] = h[2] xor (v2 xor v10)
            h[3] = h[3] xor (v3 xor v11)
            h[4] = h[4] xor (v4 xor v12)
            h[5] = h[5] xor (v5 xor v13)
            h[6] = h[6] xor (v6 xor v14)
            h[7] = h[7] xor (v7 xor v15)
        }
        c[0] = c0
        c[1] = c1
    }

    private fun rotateLeft32(x: UInt, k: Int): UInt {
        val s = (k.toUInt() and (32u - 1u)).toInt()
        return (x shl s) or (x shr (32 - s))
    }

    companion object {
        // The blocksize of BLAKE2s in bytes.
        private const val BlockSize = 64

        // The hash size of BLAKE2s-256 in bytes.
        private const val Size = 32

        fun fromKey(key: ByteArray?): Result<Blake2s> {
            if ((key != null) && (key.size > Size)) {
                return Err("Unsupported keySize ${key.size} in Blake2s")
            }
            return Ok(Blake2s(key))
        }

        private var iv = uintArrayOf(
            0x6a09e667u,
            0xbb67ae85u,
            0x3c6ef372u,
            0xa54ff53au,
            0x510e527fu,
            0x9b05688cu,
            0x1f83d9abu,
            0x5be0cd19u
        )

        private var precomputed = arrayOf(
            byteArrayOf(0, 2, 4, 6, 1, 3, 5, 7, 8, 10, 12, 14, 9, 11, 13, 15),
            byteArrayOf(14, 4, 9, 13, 10, 8, 15, 6, 1, 0, 11, 5, 12, 2, 7, 3),
            byteArrayOf(11, 12, 5, 15, 8, 0, 2, 13, 10, 3, 7, 9, 14, 6, 1, 4),
            byteArrayOf(7, 3, 13, 11, 9, 1, 12, 14, 2, 5, 4, 15, 6, 10, 0, 8),
            byteArrayOf(9, 5, 2, 10, 0, 7, 4, 15, 14, 11, 6, 3, 1, 12, 8, 13),
            byteArrayOf(2, 6, 0, 8, 12, 10, 11, 3, 4, 7, 15, 1, 13, 5, 14, 9),
            byteArrayOf(12, 1, 14, 4, 5, 15, 13, 10, 0, 6, 9, 8, 7, 3, 2, 11),
            byteArrayOf(13, 7, 12, 3, 11, 14, 1, 9, 5, 15, 8, 2, 0, 4, 6, 10),
            byteArrayOf(6, 14, 11, 0, 15, 9, 3, 8, 12, 13, 1, 10, 2, 7, 4, 5),
            byteArrayOf(10, 8, 7, 1, 2, 4, 6, 5, 15, 9, 3, 13, 11, 14, 12, 0)
        )
    }
}
