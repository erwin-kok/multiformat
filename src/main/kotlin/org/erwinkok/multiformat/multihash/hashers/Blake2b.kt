// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
@file:OptIn(ExperimentalUnsignedTypes::class)

package org.erwinkok.multiformat.multihash.hashers

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class Blake2b private constructor(key: ByteArray?, hashSize: Int = Size) : Hasher {
    private var h = ULongArray(8)
    private var c = ULongArray(2)
    private var size: Int = 0
    private var block = ByteArray(BlockSize)
    private var offset: Int = 0
    private var key = ByteArray(BlockSize)
    private var keyLen: Int = 0

    init {
        require(hashSize in 1..Size)
        this.size = hashSize
        if (key != null) {
            require(key.size <= Size)
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
        if (p.size - index > 0) {
            System.arraycopy(p, index, block, 0, p.size - index)
            offset += (p.size - index)
        }
        return Ok(n)
    }

    override fun sum(): ByteArray {
        val hash = ByteArray(Size)
        finalize(hash)
        return hash.copyOf(size)
    }

    override fun reset() {
        for (i in iv.indices) {
            h[i] = iv[i]
        }
        h[0] = h[0] xor (size.toULong() or (keyLen.toULong() shl 8) or (1uL shl 16) or (1uL shl 24))
        offset = 0
        c[0] = 0u
        c[1] = 0u
        if (keyLen > 0) {
            System.arraycopy(key, 0, block, 0, key.size)
            offset = BlockSize
        }
    }

    override fun size(): Int {
        return size
    }

    override fun blockSize(): Int {
        return BlockSize
    }

    private fun finalize(hash: ByteArray) {
        val blk = ByteArray(BlockSize)
        System.arraycopy(block, 0, blk, 0, offset)
        val remaining = (BlockSize - offset).toULong()
        val vc = c.copyOf()
        if (vc[0] < remaining) {
            vc[1]--
        }
        vc[0] -= remaining
        val vh = h.copyOf()
        hashBlocks(vh, vc, 0xFFFFFFFFFFFFFFFFuL, blk, 0, blk.size)
        for ((i, v) in vh.withIndex()) {
            hash[8 * i + 0] = (v and 0xffu).toByte()
            hash[8 * i + 1] = ((v shr 8) and 0xffu).toByte()
            hash[8 * i + 2] = ((v shr 16) and 0xffu).toByte()
            hash[8 * i + 3] = ((v shr 24) and 0xffu).toByte()
            hash[8 * i + 4] = ((v shr 32) and 0xffu).toByte()
            hash[8 * i + 5] = ((v shr 40) and 0xffu).toByte()
            hash[8 * i + 6] = ((v shr 48) and 0xffu).toByte()
            hash[8 * i + 7] = ((v shr 56) and 0xffu).toByte()
        }
    }

    private fun hashBlocks(h: ULongArray, c: ULongArray, flag: ULong, blocks: ByteArray, index: Int, size: Int) {
        val m = ULongArray(16)
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
                m[j] =
                    (blocks[i].toUByte().toULong()) or
                    (blocks[i + 1].toUByte().toULong() shl 8) or
                    (blocks[i + 2].toUByte().toULong() shl 16) or
                    (blocks[i + 3].toUByte().toULong() shl 24) or
                    (blocks[i + 4].toUByte().toULong() shl 32) or
                    (blocks[i + 5].toUByte().toULong() shl 40) or
                    (blocks[i + 6].toUByte().toULong() shl 48) or
                    (blocks[i + 7].toUByte().toULong() shl 56)
                i += 8
            }
            for (s in precomputed) {
                v0 += m[s[0].toInt()]
                v0 += v4
                v12 = v12 xor v0
                v12 = rotateLeft64(v12, -32)
                v8 += v12
                v4 = v4 xor v8
                v4 = rotateLeft64(v4, -24)
                v1 += m[s[1].toInt()]
                v1 += v5
                v13 = v13 xor v1
                v13 = rotateLeft64(v13, -32)
                v9 += v13
                v5 = v5 xor v9
                v5 = rotateLeft64(v5, -24)
                v2 += m[s[2].toInt()]
                v2 += v6
                v14 = v14 xor v2
                v14 = rotateLeft64(v14, -32)
                v10 += v14
                v6 = v6 xor v10
                v6 = rotateLeft64(v6, -24)
                v3 += m[s[3].toInt()]
                v3 += v7
                v15 = v15 xor v3
                v15 = rotateLeft64(v15, -32)
                v11 += v15
                v7 = v7 xor v11
                v7 = rotateLeft64(v7, -24)

                v0 += m[s[4].toInt()]
                v0 += v4
                v12 = v12 xor v0
                v12 = rotateLeft64(v12, -16)
                v8 += v12
                v4 = v4 xor v8
                v4 = rotateLeft64(v4, -63)
                v1 += m[s[5].toInt()]
                v1 += v5
                v13 = v13 xor v1
                v13 = rotateLeft64(v13, -16)
                v9 += v13
                v5 = v5 xor v9
                v5 = rotateLeft64(v5, -63)
                v2 += m[s[6].toInt()]
                v2 += v6
                v14 = v14 xor v2
                v14 = rotateLeft64(v14, -16)
                v10 += v14
                v6 = v6 xor v10
                v6 = rotateLeft64(v6, -63)
                v3 += m[s[7].toInt()]
                v3 += v7
                v15 = v15 xor v3
                v15 = rotateLeft64(v15, -16)
                v11 += v15
                v7 = v7 xor v11
                v7 = rotateLeft64(v7, -63)

                v0 += m[s[8].toInt()]
                v0 += v5
                v15 = v15 xor v0
                v15 = rotateLeft64(v15, -32)
                v10 += v15
                v5 = v5 xor v10
                v5 = rotateLeft64(v5, -24)
                v1 += m[s[9].toInt()]
                v1 += v6
                v12 = v12 xor v1
                v12 = rotateLeft64(v12, -32)
                v11 += v12
                v6 = v6 xor v11
                v6 = rotateLeft64(v6, -24)
                v2 += m[s[10].toInt()]
                v2 += v7
                v13 = v13 xor v2
                v13 = rotateLeft64(v13, -32)
                v8 += v13
                v7 = v7 xor v8
                v7 = rotateLeft64(v7, -24)
                v3 += m[s[11].toInt()]
                v3 += v4
                v14 = v14 xor v3
                v14 = rotateLeft64(v14, -32)
                v9 += v14
                v4 = v4 xor v9
                v4 = rotateLeft64(v4, -24)

                v0 += m[s[12].toInt()]
                v0 += v5
                v15 = v15 xor v0
                v15 = rotateLeft64(v15, -16)
                v10 += v15
                v5 = v5 xor v10
                v5 = rotateLeft64(v5, -63)
                v1 += m[s[13].toInt()]
                v1 += v6
                v12 = v12 xor v1
                v12 = rotateLeft64(v12, -16)
                v11 += v12
                v6 = v6 xor v11
                v6 = rotateLeft64(v6, -63)
                v2 += m[s[14].toInt()]
                v2 += v7
                v13 = v13 xor v2
                v13 = rotateLeft64(v13, -16)
                v8 += v13
                v7 = v7 xor v8
                v7 = rotateLeft64(v7, -63)
                v3 += m[s[15].toInt()]
                v3 += v4
                v14 = v14 xor v3
                v14 = rotateLeft64(v14, -16)
                v9 += v14
                v4 = v4 xor v9
                v4 = rotateLeft64(v4, -63)
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

    private fun rotateLeft64(x: ULong, k: Long): ULong {
        val s = (k.toUInt() and (64u - 1u)).toInt()
        return (x shl s) or (x shr (64 - s))
    }

    companion object {
        // The blocksize of BLAKE2b in bytes.
        private const val BlockSize = 128

        // The hash size of BLAKE2b-512 in bytes.
        private const val Size = 64

        fun fromKey(key: ByteArray?, hashSize: Int = Size): Result<Blake2b> {
            if (hashSize < 1 || hashSize > Size) {
                return Err("Unsupported hashSize $hashSize in Blake2b")
            }
            if ((key != null) && (key.size > Size)) {
                return Err("Unsupported keySize ${key.size} in Blake2b")
            }
            return Ok(Blake2b(key, hashSize))
        }

        private var iv = ulongArrayOf(
            0x6a09e667f3bcc908u,
            0xbb67ae8584caa73bu,
            0x3c6ef372fe94f82bu,
            0xa54ff53a5f1d36f1u,
            0x510e527fade682d1u,
            0x9b05688c2b3e6c1fu,
            0x1f83d9abfb41bd6bu,
            0x5be0cd19137e2179u,
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
            byteArrayOf(10, 8, 7, 1, 2, 4, 6, 5, 15, 9, 3, 13, 11, 14, 12, 0),
            byteArrayOf(0, 2, 4, 6, 1, 3, 5, 7, 8, 10, 12, 14, 9, 11, 13, 15), // equal to the first
            byteArrayOf(14, 4, 9, 13, 10, 8, 15, 6, 1, 0, 11, 5, 12, 2, 7, 3), // equal to the second
        )
    }
}
