// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
@file:OptIn(ExperimentalUnsignedTypes::class)

package org.erwinkok.multiformat.multihash.hashers

import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import kotlin.math.min

// From: https://github.com/BLAKE3-team/BLAKE3/blob/master/reference_impl/reference_impl.rs
class Blake3(private val key: ChainValue, private val size: Int, private val flags: Int) : Hasher {
    private var chunkState = ChunkState(key, 0, flags)
    private var cvStack = arrayOfNulls<ChainValue>(54)
    private var cvStackLen = 0

    // Add input to the hash state. This can be called any number of times.
    override fun write(p: ByteArray): Result<Int> {
        var currPos = 0
        while (currPos < p.size) {
            // If the current chunk is complete, finalize it and reset the
            // chunk state. More input is coming, so this chunk is not ROOT.
            if (chunkState.length == ChunkSize) {
                val chunkCV = chunkState.output().chainingValue()
                val totalChunks = chunkState.chunkCounter + 1
                addChunkChainingValue(chunkCV, totalChunks)
                chunkState = ChunkState(key, totalChunks, flags)
            }
            // Compress input bytes into the current chunk state.
            val want = ChunkSize - chunkState.length
            val take = min(want, p.size - currPos)
            chunkState.update(p.copyOfRange(currPos, currPos + take))
            currPos += take
        }
        return Ok(p.size)
    }

    // Finalize the hash and write any number of output bytes.
    override fun sum(): ByteArray {
        // Starting with the Output from the current chunk, compute all the
        // parent chaining values along the right edge of the tree, until we
        // have the root Output.
        var node = chunkState.output()
        var cv = popStack()
        while (cv != null) {
            node = parentOutput(cv, node.chainingValue(), key, flags)
            cv = popStack()
        }
        return node.rootOutputBytes(size)
    }

    override fun reset() {
        cvStackLen = 0
        chunkState = ChunkState(key, 0, flags)
    }

    override fun size(): Int {
        return size
    }

    override fun blockSize(): Int {
        return BlockSize
    }

    private fun parentOutput(left: ChainValue, right: ChainValue, key: ChainValue, flags: Int): Output {
        val blockWords = IntArray(16)
        System.arraycopy(left.v.toIntArray(), 0, blockWords, 0, 8)
        System.arraycopy(right.v.toIntArray(), 0, blockWords, 8, 8)
        return Output(key, blockWords.toUIntArray(), 0, BlockSize, flags or FlagParent)
    }

    private fun parentCV(left: ChainValue, right: ChainValue, key: ChainValue, flags: Int): ChainValue {
        return parentOutput(left, right, key, flags).chainingValue()
    }

    // Section 5.1.2 of the BLAKE3 spec explains this algorithm in more detail.
    private fun addChunkChainingValue(newCV: ChainValue, totalChunks: Long) {
        // This chunk might complete some subtrees. For each completed subtree,
        // its left child will be the current top entry in the CV stack, and
        // its right child will be the current value of `new_cv`. Pop each left
        // child off the stack, merge it with `new_cv`, and overwrite `new_cv`
        // with the result. After all these merges, push the final value of
        // `new_cv` onto the stack. The number of completed subtrees is given
        // by the number of trailing 0-bits in the new total number of chunks.
        var ccv = newCV
        var ctotalChunks = totalChunks
        while ((ctotalChunks and 1L) == 0L) {
            ccv = parentCV(popStack()!!, ccv, key, flags)
            ctotalChunks = ctotalChunks shr 1
        }
        pushStack(ccv)
    }

    private fun pushStack(cv: ChainValue) {
        cvStack[cvStackLen] = cv
        cvStackLen++
    }

    private fun popStack(): ChainValue? {
        if (cvStackLen == 0) {
            return null
        }
        cvStackLen--
        return cvStack[cvStackLen]
    }

    data class ChainValue(
        val cv0: UInt,
        val cv1: UInt,
        val cv2: UInt,
        val cv3: UInt,
        val cv4: UInt,
        val cv5: UInt,
        val cv6: UInt,
        val cv7: UInt
    ) {
        constructor(a: UIntArray) : this(
            a[0],
            a[1],
            a[2],
            a[3],
            a[4],
            a[5],
            a[6],
            a[7]
        )

        operator fun get(index: Int): UInt {
            when (index) {
                0 -> return cv0
                1 -> return cv1
                2 -> return cv2
                3 -> return cv3
                4 -> return cv4
                5 -> return cv5
                6 -> return cv6
                7 -> return cv7
            }
            return 0u
        }

        val v: UIntArray
            get() = uintArrayOf(cv0, cv1, cv2, cv3, cv4, cv5, cv6, cv7)
    }

    // Each chunk or parent node can produce either an 8-word chaining value or, by
    // setting the ROOT flag, any number of final output bytes. The Output struct
    // captures the state just prior to choosing between those two possibilities.
    private class Output(private var inputChainingValue: ChainValue, private var blockWords: UIntArray, private var counter: Long, private var blockLen: Int, private var flags: Int) {
        private val buf = ByteArray(BlockSize)

        // Return the 8 int CV
        fun chainingValue(): ChainValue {
            return ChainValue(compress(inputChainingValue, blockWords, counter, blockLen, flags))
        }

        fun rootOutputBytes(outLen: Int): ByteArray {
            var counter = 0L
            val hash = ByteArray(outLen)
            var offset = 0
            while (offset < outLen) {
                val words = compress(inputChainingValue, blockWords, counter, blockLen, flags or FlagRoot)
                wordsToBytes(words, buf)
                System.arraycopy(buf, 0, hash, offset, min(BlockSize, outLen - offset))
                offset += BlockSize
                counter += 1
            }
            return hash
        }
    }

    private class ChunkState(private var chainingValue: ChainValue, var chunkCounter: Long, private var flags: Int) {
        private var block = ByteArray(BlockSize)
        private var blockLen = 0
        private var blocksCompressed = 0

        val length: Int
            get() = BlockSize * blocksCompressed + blockLen

        private fun startFlag(): Int {
            return if (blocksCompressed == 0) FlagChunkStart else 0
        }

        fun update(input: ByteArray) {
            var index = 0
            while (index < input.size) {
                // If the block buffer is full, compress it and clear it. More
                // input_bytes is coming, so this compression is not CHUNK_END.
                if (blockLen == BlockSize) {
                    val blockWords = bytesToWords(block)
                    chainingValue = ChainValue(compress(chainingValue, blockWords, chunkCounter, BlockSize, flags or startFlag()))
                    blocksCompressed++
                    block = ByteArray(BlockSize)
                    blockLen = 0
                }
                // Copy input bytes into the block buffer.
                val want = BlockSize - blockLen
                val canTake = min(want, input.size - index)
                System.arraycopy(input, index, block, blockLen, canTake)
                blockLen += canTake
                index += canTake
            }
        }

        fun output(): Output {
            return Output(chainingValue, bytesToWords(block), chunkCounter, blockLen, flags or startFlag() or FlagChunkEnd)
        }
    }

    companion object {
        private const val BlockSize = 64
        private const val ChunkSize = 1024

        private const val FlagChunkStart = 0b0000001
        private const val FlagChunkEnd = 0b0000010
        private const val FlagParent = 0b0000100
        private const val FlagRoot = 0b0001000
        private const val FlagKeyedHash = 0b0010000
        private const val FlagDeriveKeyContext = 0b0100000
        private const val FlagDeriveKeyMaterial = 0b1000000

        private var iv = ChainValue(
            0x6A09E667u,
            0xBB67AE85u,
            0x3C6EF372u,
            0xA54FF53Au,
            0x510E527Fu,
            0x9B05688Cu,
            0x1F83D9ABu,
            0x5BE0CD19u
        )

        private val MessagePermutation = intArrayOf(
            2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8
        )

        fun fromKey(key: ByteArray?, size: Int): Hasher {
            return if (key == null) {
                Blake3(iv, size, 0)
            } else {
                Blake3(ChainValue(bytesToWords(key)), size, FlagKeyedHash)
            }
        }

        fun deriveKey(context: String, input: ByteArray, size: Int): ByteArray {
            val h1 = Blake3(iv, size, FlagDeriveKeyContext)
            h1.write(context.toByteArray())
            val contextKey = bytesToWords(h1.sum())
            val h2 = Blake3(ChainValue(contextKey), size, FlagDeriveKeyMaterial)
            h2.write(input)
            return h2.sum()
        }

        // The mixing function, G, which mixes either a column or a diagonal.
        private fun g(state: UIntArray, a: Int, b: Int, c: Int, d: Int, mx: UInt, my: UInt) {
            state[a] = wrappingAdd(wrappingAdd(state[a], state[b]), mx)
            state[d] = rotateRight(state[d] xor state[a], 16)
            state[c] = wrappingAdd(state[c], state[d])
            state[b] = rotateRight(state[b] xor state[c], 12)
            state[a] = wrappingAdd(wrappingAdd(state[a], state[b]), my)
            state[d] = rotateRight(state[d] xor state[a], 8)
            state[c] = wrappingAdd(state[c], state[d])
            state[b] = rotateRight(state[b] xor state[c], 7)
        }

        private fun round(state: UIntArray, m: UIntArray) {
            // Mix columns
            g(state, 0, 4, 8, 12, m[0], m[1])
            g(state, 1, 5, 9, 13, m[2], m[3])
            g(state, 2, 6, 10, 14, m[4], m[5])
            g(state, 3, 7, 11, 15, m[6], m[7])

            // Mix diagonals
            g(state, 0, 5, 10, 15, m[8], m[9])
            g(state, 1, 6, 11, 12, m[10], m[11])
            g(state, 2, 7, 8, 13, m[12], m[13])
            g(state, 3, 4, 9, 14, m[14], m[15])
        }

        private fun permute(m: UIntArray): UIntArray {
            val permuted = UIntArray(16)
            for (i in 0..15) {
                permuted[i] = m[MessagePermutation[i]]
            }
            return permuted
        }

        private fun wrappingAdd(a: UInt, b: UInt): UInt {
            return a + b
        }

        private fun rotateRight(x: UInt, len: Int): UInt {
            return (x shr len) or (x shl (32 - len))
        }

        private fun compress(chainingValue: ChainValue, blockWords: UIntArray, counter: Long, blockLen: Int, flags: Int): UIntArray {
            val counterInt = (counter and 0xffffffffL).toUInt()
            val counterShift = ((counter shr 32) and 0xffffffffL).toUInt()
            val state = uintArrayOf(
                chainingValue[0],
                chainingValue[1],
                chainingValue[2],
                chainingValue[3],
                chainingValue[4],
                chainingValue[5],
                chainingValue[6],
                chainingValue[7],
                iv[0],
                iv[1],
                iv[2],
                iv[3],
                counterInt,
                counterShift,
                blockLen.toUInt(),
                flags.toUInt()
            )
            var bw = blockWords
            round(state, bw) // Round 1
            bw = permute(bw)
            round(state, bw) // Round 2
            bw = permute(bw)
            round(state, bw) // Round 3
            bw = permute(bw)
            round(state, bw) // Round 4
            bw = permute(bw)
            round(state, bw) // Round 5
            bw = permute(bw)
            round(state, bw) // Round 6
            bw = permute(bw)
            round(state, bw) // Round 7
            for (i in 0..7) {
                state[i] = state[i] xor state[i + 8]
                state[i + 8] = state[i + 8] xor chainingValue[i]
            }
            return state
        }

        private fun bytesToWords(b: ByteArray): UIntArray {
            val words = UIntArray(b.size / 4)
            for (i in words.indices) {
                words[i] = b[i * 4 + 0].toUByte().toUInt() or (b[i * 4 + 1].toUByte().toUInt() shl 8) or (b[i * 4 + 2].toUByte().toUInt() shl 16) or (b[i * 4 + 3].toUByte().toUInt() shl 24)
            }
            return words
        }

        private fun wordsToBytes(words: UIntArray, block: ByteArray) {
            for ((i, v) in words.withIndex()) {
                block[i * 4 + 0] = v.toByte()
                block[i * 4 + 1] = (v shr 8).toByte()
                block[i * 4 + 2] = (v shr 16).toByte()
                block[i * 4 + 3] = (v shr 24).toByte()
            }
        }
    }
}
