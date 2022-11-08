// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
@file:OptIn(ExperimentalUnsignedTypes::class)

package org.erwinkok.multiformat.multihash.hashers

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import java.util.Arrays
import kotlin.experimental.xor
import kotlin.math.min

open class Sha3(
    val rate: Int, // the number of bytes of state to use
    val outputLen: Int, // the default output size in bytes

    // dsbyte contains the "domain separation" bits and the first bit of
    // the padding. Sections 6.1 and 6.2 of [1] separate the outputs of the
    // SHA-3 and SHAKE functions by appending bitstrings to the message.
    // Using a little-endian bit-ordering convention, these are "01" for SHA-3
    // and "1111" for SHAKE, or 00000010b and 00001111b, respectively. Then the
    // padding rule from section 5.1 is applied to pad the message to a multiple
    // of the rate, which involves adding a "1" bit, zero or more "0" bits, and
    // a final "1" bit. We merge the first "1" bit from the padding into dsbyte,
    // giving 00000110b (0x06) and 00011111b (0x1f).
    // [1] http://csrc.nist.gov/publications/drafts/fips-202/fips_202_draft.pdf
    //     "Draft FIPS 202: SHA-3 Standard: Permutation-Based Hash and
    //      Extendable-Output Functions (May 2014)"
    val dsbyte: Byte
) : Hasher {
    enum class SpongeDirection {
        SpongeAbsorbing,
        SpongeSqueezing
    }

    private val a = LongArray(25) // main state of the hash
    private val buf = ByteArray(maxRate)
    private var bufIndex = 0
    private var state = SpongeDirection.SpongeAbsorbing // whether the sponge is absorbing or squeezing

    override fun write(p: ByteArray): Result<Int> {
        if (state != SpongeDirection.SpongeAbsorbing) {
            return Err("sha3: write to sponge after read")
        }
        var index = 0
        while ((p.size - index) > 0) {
            if (bufIndex == 0 && (p.size - index) >= rate) {
                // The fast path; absorb a full "rate" bytes of input and apply the permutation.
                xorIn(p, index, rate, a)
                index += rate
                keccakF1600(a)
            } else {
                // The slow path; buffer the input until we can fill the sponge, and then xor it in.
                var todo = rate - bufIndex
                if (todo > (p.size - index)) {
                    todo = p.size - index
                }
                System.arraycopy(p, index, buf, bufIndex, todo)
                bufIndex += todo
                index += todo

                // If the sponge is full, apply the permutation.
                if (bufIndex == rate) {
                    permute()
                }
            }
        }
        return Ok(p.size)
    }

    override fun sum(): ByteArray {
        val dup = clone()
        val hash = ByteArray(dup.outputLen)
        dup.read(hash)
        return hash
    }

    override fun reset() {
        // Zero the permutation's state.
        Arrays.fill(a, 0)
        Arrays.fill(buf, 0)
        state = SpongeDirection.SpongeAbsorbing
        bufIndex = 0
    }

    override fun size(): Int {
        return outputLen
    }

    override fun blockSize(): Int {
        return rate
    }

    fun read(out: ByteArray): Result<Int> {
        // If we're still absorbing, pad and apply the permutation.
        if (state == SpongeDirection.SpongeAbsorbing) {
            padAndPermute(dsbyte)
        }
        // Now, do the squeezing.
        var outIndex = 0
        while ((out.size - outIndex) > 0) {
            val length = min(out.size - outIndex, buf.size - bufIndex)
            System.arraycopy(buf, bufIndex, out, outIndex, length)
            bufIndex += length
            outIndex += length
            // Apply the permutation if we've squeezed the sponge dry.
            if ((buf.size - bufIndex) == 0) {
                permute()
            }
        }
        return Ok(out.size)
    }

    private fun permute() {
        when (state) {
            SpongeDirection.SpongeAbsorbing -> {
                // If we're absorbing, we need to xor the input into the state
                // before applying the permutation.
                xorIn(buf, 0, rate, a)
                bufIndex = 0
                keccakF1600(a)
            }

            SpongeDirection.SpongeSqueezing -> {
                // If we're squeezing, we need to apply the permutation before
                // copying more output.
                keccakF1600(a)
                bufIndex = 0
                copyOut(buf, 0, rate, a)
            }
        }
    }

    private fun padAndPermute(dsbyte: Byte) {
        // Pad with this instance's domain-separator bits. We know that there's
        // at least one byte of space in d.buf because, if it were full,
        // permute would have been called to empty it. dsbyte also contains the
        // first one bit for the padding. See the comment in the state struct.
        buf[bufIndex] = dsbyte
        bufIndex++

        for (i in bufIndex until rate) {
            buf[i] = 0
        }
        // This adds the final one bit for the padding. Because of the way that
        // bits are numbered from the LSB upwards, the final bit is the MSB of
        // the last byte.
        buf[rate - 1] = buf[rate - 1] xor 0x80.toByte()
        // Apply the permutation
        permute()
        state = SpongeDirection.SpongeSqueezing
        bufIndex = 0
        copyOut(buf, 0, rate, a)
    }

    fun clone(): Sha3 {
        val out = Sha3(rate, outputLen, dsbyte)
        System.arraycopy(a, 0, out.a, 0, a.size)
        System.arraycopy(buf, 0, out.buf, 0, buf.size)
        out.state = state
        out.bufIndex = bufIndex
        return out
    }

    companion object {
        // maxRate is the maximum size of the internal buffer. SHAKE-256
        // currently needs the largest buffer.
        private const val maxRate = 168

        fun sha224(): Sha3 {
            return Sha3(144, 28, 0x06)
        }

        fun sha256(): Sha3 {
            return Sha3(136, 32, 0x06)
        }

        fun sha384(): Sha3 {
            return Sha3(104, 48, 0x06)
        }

        fun sha512(): Sha3 {
            return Sha3(72, 64, 0x06)
        }

        fun keccak256(): Sha3 {
            return Sha3(136, 32, 0x01)
        }

        fun keccak512(): Sha3 {
            return Sha3(72, 64, 0x01)
        }

        private fun xorIn(b: ByteArray, idx: Int, size: Int, a: LongArray) {
            var index = idx
            val n = size / 8
            for (i in 0 until n) {
                val v = b[index + 0].toUByte().toULong() or
                    (b[index + 1].toUByte().toULong() shl 8) or
                    (b[index + 2].toUByte().toULong() shl 16) or
                    (b[index + 3].toUByte().toULong() shl 24) or
                    (b[index + 4].toUByte().toULong() shl 32) or
                    (b[index + 5].toUByte().toULong() shl 40) or
                    (b[index + 6].toUByte().toULong() shl 48) or
                    (b[index + 7].toUByte().toULong() shl 56)
                a[i] = a[i] xor v.toLong()
                index += 8
            }
        }

        private fun copyOut(b: ByteArray, idx: Int, size: Int, a: LongArray) {
            var index = idx
            var i = 0
            while ((size - index) >= 8) {
                val v = a[i]
                b[index + 0] = (v).toByte()
                b[index + 1] = (v ushr 8).toByte()
                b[index + 2] = (v ushr 16).toByte()
                b[index + 3] = (v ushr 24).toByte()
                b[index + 4] = (v ushr 32).toByte()
                b[index + 5] = (v ushr 40).toByte()
                b[index + 6] = (v ushr 48).toByte()
                b[index + 7] = (v ushr 56).toByte()
                index += 8
                i++
            }
        }

        private fun keccakF1600(a: LongArray) {
            var bc0: Long
            var bc1: Long
            var bc2: Long
            var bc3: Long
            var bc4: Long
            var d0: Long
            var d1: Long
            var d2: Long
            var d3: Long
            var d4: Long
            var t: Long

            for (i in 0 until 24 step 4) {
                // Combines the 5 steps in each round into 2 steps.
                // Unrolls 4 rounds per loop and spreads some steps across rounds.

                // Round 1
                bc0 = a[0] xor a[5] xor a[10] xor a[15] xor a[20]
                bc1 = a[1] xor a[6] xor a[11] xor a[16] xor a[21]
                bc2 = a[2] xor a[7] xor a[12] xor a[17] xor a[22]
                bc3 = a[3] xor a[8] xor a[13] xor a[18] xor a[23]
                bc4 = a[4] xor a[9] xor a[14] xor a[19] xor a[24]
                d0 = bc4 xor ((bc1 shl 1) or (bc1 ushr 63))
                d1 = bc0 xor ((bc2 shl 1) or (bc2 ushr 63))
                d2 = bc1 xor ((bc3 shl 1) or (bc3 ushr 63))
                d3 = bc2 xor ((bc4 shl 1) or (bc4 ushr 63))
                d4 = bc3 xor ((bc0 shl 1) or (bc0 ushr 63))

                bc0 = a[0] xor d0
                t = a[6] xor d1
                bc1 = (t shl 44) or (t ushr (64 - 44))
                t = a[12] xor d2
                bc2 = (t shl 43) or (t ushr (64 - 43))
                t = a[18] xor d3
                bc3 = (t shl 21) or (t ushr (64 - 21))
                t = a[24] xor d4
                bc4 = (t shl 14) or (t ushr (64 - 14))
                a[0] = bc0 xor (bc2 and bc1.inv()) xor rc[i].toLong()
                a[6] = bc1 xor (bc3 and bc2.inv())
                a[12] = bc2 xor (bc4 and bc3.inv())
                a[18] = bc3 xor (bc0 and bc4.inv())
                a[24] = bc4 xor (bc1 and bc0.inv())

                t = a[10] xor d0
                bc2 = (t shl 3) or (t ushr (64 - 3))
                t = a[16] xor d1
                bc3 = (t shl 45) or (t ushr (64 - 45))
                t = a[22] xor d2
                bc4 = (t shl 61) or (t ushr (64 - 61))
                t = a[3] xor d3
                bc0 = (t shl 28) or (t ushr (64 - 28))
                t = a[9] xor d4
                bc1 = (t shl 20) or (t ushr (64 - 20))
                a[10] = bc0 xor (bc2 and bc1.inv())
                a[16] = bc1 xor (bc3 and bc2.inv())
                a[22] = bc2 xor (bc4 and bc3.inv())
                a[3] = bc3 xor (bc0 and bc4.inv())
                a[9] = bc4 xor (bc1 and bc0.inv())

                t = a[20] xor d0
                bc4 = (t shl 18) or (t ushr (64 - 18))
                t = a[1] xor d1
                bc0 = (t shl 1) or (t ushr (64 - 1))
                t = a[7] xor d2
                bc1 = (t shl 6) or (t ushr (64 - 6))
                t = a[13] xor d3
                bc2 = (t shl 25) or (t ushr (64 - 25))
                t = a[19] xor d4
                bc3 = (t shl 8) or (t ushr (64 - 8))
                a[20] = bc0 xor (bc2 and bc1.inv())
                a[1] = bc1 xor (bc3 and bc2.inv())
                a[7] = bc2 xor (bc4 and bc3.inv())
                a[13] = bc3 xor (bc0 and bc4.inv())
                a[19] = bc4 xor (bc1 and bc0.inv())

                t = a[5] xor d0
                bc1 = (t shl 36) or (t ushr (64 - 36))
                t = a[11] xor d1
                bc2 = (t shl 10) or (t ushr (64 - 10))
                t = a[17] xor d2
                bc3 = (t shl 15) or (t ushr (64 - 15))
                t = a[23] xor d3
                bc4 = (t shl 56) or (t ushr (64 - 56))
                t = a[4] xor d4
                bc0 = (t shl 27) or (t ushr (64 - 27))
                a[5] = bc0 xor (bc2 and bc1.inv())
                a[11] = bc1 xor (bc3 and bc2.inv())
                a[17] = bc2 xor (bc4 and bc3.inv())
                a[23] = bc3 xor (bc0 and bc4.inv())
                a[4] = bc4 xor (bc1 and bc0.inv())

                t = a[15] xor d0
                bc3 = (t shl 41) or (t ushr (64 - 41))
                t = a[21] xor d1
                bc4 = (t shl 2) or (t ushr (64 - 2))
                t = a[2] xor d2
                bc0 = (t shl 62) or (t ushr (64 - 62))
                t = a[8] xor d3
                bc1 = (t shl 55) or (t ushr (64 - 55))
                t = a[14] xor d4
                bc2 = (t shl 39) or (t ushr (64 - 39))
                a[15] = bc0 xor (bc2 and bc1.inv())
                a[21] = bc1 xor (bc3 and bc2.inv())
                a[2] = bc2 xor (bc4 and bc3.inv())
                a[8] = bc3 xor (bc0 and bc4.inv())
                a[14] = bc4 xor (bc1 and bc0.inv())

                // Round 2
                bc0 = a[0] xor a[5] xor a[10] xor a[15] xor a[20]
                bc1 = a[1] xor a[6] xor a[11] xor a[16] xor a[21]
                bc2 = a[2] xor a[7] xor a[12] xor a[17] xor a[22]
                bc3 = a[3] xor a[8] xor a[13] xor a[18] xor a[23]
                bc4 = a[4] xor a[9] xor a[14] xor a[19] xor a[24]
                d0 = bc4 xor ((bc1 shl 1) or (bc1 ushr 63))
                d1 = bc0 xor ((bc2 shl 1) or (bc2 ushr 63))
                d2 = bc1 xor ((bc3 shl 1) or (bc3 ushr 63))
                d3 = bc2 xor ((bc4 shl 1) or (bc4 ushr 63))
                d4 = bc3 xor ((bc0 shl 1) or (bc0 ushr 63))

                bc0 = a[0] xor d0
                t = a[16] xor d1
                bc1 = (t shl 44) or (t ushr (64 - 44))
                t = a[7] xor d2
                bc2 = (t shl 43) or (t ushr (64 - 43))
                t = a[23] xor d3
                bc3 = (t shl 21) or (t ushr (64 - 21))
                t = a[14] xor d4
                bc4 = (t shl 14) or (t ushr (64 - 14))
                a[0] = bc0 xor (bc2 and bc1.inv()) xor rc[i + 1].toLong()
                a[16] = bc1 xor (bc3 and bc2.inv())
                a[7] = bc2 xor (bc4 and bc3.inv())
                a[23] = bc3 xor (bc0 and bc4.inv())
                a[14] = bc4 xor (bc1 and bc0.inv())

                t = a[20] xor d0
                bc2 = (t shl 3) or (t ushr (64 - 3))
                t = a[11] xor d1
                bc3 = (t shl 45) or (t ushr (64 - 45))
                t = a[2] xor d2
                bc4 = (t shl 61) or (t ushr (64 - 61))
                t = a[18] xor d3
                bc0 = (t shl 28) or (t ushr (64 - 28))
                t = a[9] xor d4
                bc1 = (t shl 20) or (t ushr (64 - 20))
                a[20] = bc0 xor (bc2 and bc1.inv())
                a[11] = bc1 xor (bc3 and bc2.inv())
                a[2] = bc2 xor (bc4 and bc3.inv())
                a[18] = bc3 xor (bc0 and bc4.inv())
                a[9] = bc4 xor (bc1 and bc0.inv())

                t = a[15] xor d0
                bc4 = (t shl 18) or (t ushr (64 - 18))
                t = a[6] xor d1
                bc0 = (t shl 1) or (t ushr (64 - 1))
                t = a[22] xor d2
                bc1 = (t shl 6) or (t ushr (64 - 6))
                t = a[13] xor d3
                bc2 = (t shl 25) or (t ushr (64 - 25))
                t = a[4] xor d4
                bc3 = (t shl 8) or (t ushr (64 - 8))
                a[15] = bc0 xor (bc2 and bc1.inv())
                a[6] = bc1 xor (bc3 and bc2.inv())
                a[22] = bc2 xor (bc4 and bc3.inv())
                a[13] = bc3 xor (bc0 and bc4.inv())
                a[4] = bc4 xor (bc1 and bc0.inv())

                t = a[10] xor d0
                bc1 = (t shl 36) or (t ushr (64 - 36))
                t = a[1] xor d1
                bc2 = (t shl 10) or (t ushr (64 - 10))
                t = a[17] xor d2
                bc3 = (t shl 15) or (t ushr (64 - 15))
                t = a[8] xor d3
                bc4 = (t shl 56) or (t ushr (64 - 56))
                t = a[24] xor d4
                bc0 = (t shl 27) or (t ushr (64 - 27))
                a[10] = bc0 xor (bc2 and bc1.inv())
                a[1] = bc1 xor (bc3 and bc2.inv())
                a[17] = bc2 xor (bc4 and bc3.inv())
                a[8] = bc3 xor (bc0 and bc4.inv())
                a[24] = bc4 xor (bc1 and bc0.inv())

                t = a[5] xor d0
                bc3 = (t shl 41) or (t ushr (64 - 41))
                t = a[21] xor d1
                bc4 = (t shl 2) or (t ushr (64 - 2))
                t = a[12] xor d2
                bc0 = (t shl 62) or (t ushr (64 - 62))
                t = a[3] xor d3
                bc1 = (t shl 55) or (t ushr (64 - 55))
                t = a[19] xor d4
                bc2 = (t shl 39) or (t ushr (64 - 39))
                a[5] = bc0 xor (bc2 and bc1.inv())
                a[21] = bc1 xor (bc3 and bc2.inv())
                a[12] = bc2 xor (bc4 and bc3.inv())
                a[3] = bc3 xor (bc0 and bc4.inv())
                a[19] = bc4 xor (bc1 and bc0.inv())

                // Round 3
                bc0 = a[0] xor a[5] xor a[10] xor a[15] xor a[20]
                bc1 = a[1] xor a[6] xor a[11] xor a[16] xor a[21]
                bc2 = a[2] xor a[7] xor a[12] xor a[17] xor a[22]
                bc3 = a[3] xor a[8] xor a[13] xor a[18] xor a[23]
                bc4 = a[4] xor a[9] xor a[14] xor a[19] xor a[24]
                d0 = bc4 xor ((bc1 shl 1) or (bc1 ushr 63))
                d1 = bc0 xor ((bc2 shl 1) or (bc2 ushr 63))
                d2 = bc1 xor ((bc3 shl 1) or (bc3 ushr 63))
                d3 = bc2 xor ((bc4 shl 1) or (bc4 ushr 63))
                d4 = bc3 xor ((bc0 shl 1) or (bc0 ushr 63))

                bc0 = a[0] xor d0
                t = a[11] xor d1
                bc1 = (t shl 44) or (t ushr (64 - 44))
                t = a[22] xor d2
                bc2 = (t shl 43) or (t ushr (64 - 43))
                t = a[8] xor d3
                bc3 = (t shl 21) or (t ushr (64 - 21))
                t = a[19] xor d4
                bc4 = (t shl 14) or (t ushr (64 - 14))
                a[0] = bc0 xor (bc2 and bc1.inv()) xor rc[i + 2].toLong()
                a[11] = bc1 xor (bc3 and bc2.inv())
                a[22] = bc2 xor (bc4 and bc3.inv())
                a[8] = bc3 xor (bc0 and bc4.inv())
                a[19] = bc4 xor (bc1 and bc0.inv())

                t = a[15] xor d0
                bc2 = (t shl 3) or (t ushr (64 - 3))
                t = a[1] xor d1
                bc3 = (t shl 45) or (t ushr (64 - 45))
                t = a[12] xor d2
                bc4 = (t shl 61) or (t ushr (64 - 61))
                t = a[23] xor d3
                bc0 = (t shl 28) or (t ushr (64 - 28))
                t = a[9] xor d4
                bc1 = (t shl 20) or (t ushr (64 - 20))
                a[15] = bc0 xor (bc2 and bc1.inv())
                a[1] = bc1 xor (bc3 and bc2.inv())
                a[12] = bc2 xor (bc4 and bc3.inv())
                a[23] = bc3 xor (bc0 and bc4.inv())
                a[9] = bc4 xor (bc1 and bc0.inv())

                t = a[5] xor d0
                bc4 = (t shl 18) or (t ushr (64 - 18))
                t = a[16] xor d1
                bc0 = (t shl 1) or (t ushr (64 - 1))
                t = a[2] xor d2
                bc1 = (t shl 6) or (t ushr (64 - 6))
                t = a[13] xor d3
                bc2 = (t shl 25) or (t ushr (64 - 25))
                t = a[24] xor d4
                bc3 = (t shl 8) or (t ushr (64 - 8))
                a[5] = bc0 xor (bc2 and bc1.inv())
                a[16] = bc1 xor (bc3 and bc2.inv())
                a[2] = bc2 xor (bc4 and bc3.inv())
                a[13] = bc3 xor (bc0 and bc4.inv())
                a[24] = bc4 xor (bc1 and bc0.inv())

                t = a[20] xor d0
                bc1 = (t shl 36) or (t ushr (64 - 36))
                t = a[6] xor d1
                bc2 = (t shl 10) or (t ushr (64 - 10))
                t = a[17] xor d2
                bc3 = (t shl 15) or (t ushr (64 - 15))
                t = a[3] xor d3
                bc4 = (t shl 56) or (t ushr (64 - 56))
                t = a[14] xor d4
                bc0 = (t shl 27) or (t ushr (64 - 27))
                a[20] = bc0 xor (bc2 and bc1.inv())
                a[6] = bc1 xor (bc3 and bc2.inv())
                a[17] = bc2 xor (bc4 and bc3.inv())
                a[3] = bc3 xor (bc0 and bc4.inv())
                a[14] = bc4 xor (bc1 and bc0.inv())

                t = a[10] xor d0
                bc3 = (t shl 41) or (t ushr (64 - 41))
                t = a[21] xor d1
                bc4 = (t shl 2) or (t ushr (64 - 2))
                t = a[7] xor d2
                bc0 = (t shl 62) or (t ushr (64 - 62))
                t = a[18] xor d3
                bc1 = (t shl 55) or (t ushr (64 - 55))
                t = a[4] xor d4
                bc2 = (t shl 39) or (t ushr (64 - 39))
                a[10] = bc0 xor (bc2 and bc1.inv())
                a[21] = bc1 xor (bc3 and bc2.inv())
                a[7] = bc2 xor (bc4 and bc3.inv())
                a[18] = bc3 xor (bc0 and bc4.inv())
                a[4] = bc4 xor (bc1 and bc0.inv())

                // Round 4
                bc0 = a[0] xor a[5] xor a[10] xor a[15] xor a[20]
                bc1 = a[1] xor a[6] xor a[11] xor a[16] xor a[21]
                bc2 = a[2] xor a[7] xor a[12] xor a[17] xor a[22]
                bc3 = a[3] xor a[8] xor a[13] xor a[18] xor a[23]
                bc4 = a[4] xor a[9] xor a[14] xor a[19] xor a[24]
                d0 = bc4 xor ((bc1 shl 1) or (bc1 ushr 63))
                d1 = bc0 xor ((bc2 shl 1) or (bc2 ushr 63))
                d2 = bc1 xor ((bc3 shl 1) or (bc3 ushr 63))
                d3 = bc2 xor ((bc4 shl 1) or (bc4 ushr 63))
                d4 = bc3 xor ((bc0 shl 1) or (bc0 ushr 63))

                bc0 = a[0] xor d0
                t = a[1] xor d1
                bc1 = (t shl 44) or (t ushr (64 - 44))
                t = a[2] xor d2
                bc2 = (t shl 43) or (t ushr (64 - 43))
                t = a[3] xor d3
                bc3 = (t shl 21) or (t ushr (64 - 21))
                t = a[4] xor d4
                bc4 = (t shl 14) or (t ushr (64 - 14))
                a[0] = bc0 xor (bc2 and bc1.inv()) xor rc[i + 3].toLong()
                a[1] = bc1 xor (bc3 and bc2.inv())
                a[2] = bc2 xor (bc4 and bc3.inv())
                a[3] = bc3 xor (bc0 and bc4.inv())
                a[4] = bc4 xor (bc1 and bc0.inv())

                t = a[5] xor d0
                bc2 = (t shl 3) or (t ushr (64 - 3))
                t = a[6] xor d1
                bc3 = (t shl 45) or (t ushr (64 - 45))
                t = a[7] xor d2
                bc4 = (t shl 61) or (t ushr (64 - 61))
                t = a[8] xor d3
                bc0 = (t shl 28) or (t ushr (64 - 28))
                t = a[9] xor d4
                bc1 = (t shl 20) or (t ushr (64 - 20))
                a[5] = bc0 xor (bc2 and bc1.inv())
                a[6] = bc1 xor (bc3 and bc2.inv())
                a[7] = bc2 xor (bc4 and bc3.inv())
                a[8] = bc3 xor (bc0 and bc4.inv())
                a[9] = bc4 xor (bc1 and bc0.inv())

                t = a[10] xor d0
                bc4 = (t shl 18) or (t ushr (64 - 18))
                t = a[11] xor d1
                bc0 = (t shl 1) or (t ushr (64 - 1))
                t = a[12] xor d2
                bc1 = (t shl 6) or (t ushr (64 - 6))
                t = a[13] xor d3
                bc2 = (t shl 25) or (t ushr (64 - 25))
                t = a[14] xor d4
                bc3 = (t shl 8) or (t ushr (64 - 8))
                a[10] = bc0 xor (bc2 and bc1.inv())
                a[11] = bc1 xor (bc3 and bc2.inv())
                a[12] = bc2 xor (bc4 and bc3.inv())
                a[13] = bc3 xor (bc0 and bc4.inv())
                a[14] = bc4 xor (bc1 and bc0.inv())

                t = a[15] xor d0
                bc1 = (t shl 36) or (t ushr (64 - 36))
                t = a[16] xor d1
                bc2 = (t shl 10) or (t ushr (64 - 10))
                t = a[17] xor d2
                bc3 = (t shl 15) or (t ushr (64 - 15))
                t = a[18] xor d3
                bc4 = (t shl 56) or (t ushr (64 - 56))
                t = a[19] xor d4
                bc0 = (t shl 27) or (t ushr (64 - 27))
                a[15] = bc0 xor (bc2 and bc1.inv())
                a[16] = bc1 xor (bc3 and bc2.inv())
                a[17] = bc2 xor (bc4 and bc3.inv())
                a[18] = bc3 xor (bc0 and bc4.inv())
                a[19] = bc4 xor (bc1 and bc0.inv())

                t = a[20] xor d0
                bc3 = (t shl 41) or (t ushr (64 - 41))
                t = a[21] xor d1
                bc4 = (t shl 2) or (t ushr (64 - 2))
                t = a[22] xor d2
                bc0 = (t shl 62) or (t ushr (64 - 62))
                t = a[23] xor d3
                bc1 = (t shl 55) or (t ushr (64 - 55))
                t = a[24] xor d4
                bc2 = (t shl 39) or (t ushr (64 - 39))
                a[20] = bc0 xor (bc2 and bc1.inv())
                a[21] = bc1 xor (bc3 and bc2.inv())
                a[22] = bc2 xor (bc4 and bc3.inv())
                a[23] = bc3 xor (bc0 and bc4.inv())
                a[24] = bc4 xor (bc1 and bc0.inv())
            }
        }

        private val rc = ulongArrayOf(
            0x0000000000000001uL,
            0x0000000000008082uL,
            0x800000000000808AuL,
            0x8000000080008000uL,
            0x000000000000808BuL,
            0x0000000080000001uL,
            0x8000000080008081uL,
            0x8000000000008009uL,
            0x000000000000008AuL,
            0x0000000000000088uL,
            0x0000000080008009uL,
            0x000000008000000AuL,
            0x000000008000808BuL,
            0x800000000000008BuL,
            0x8000000000008089uL,
            0x8000000000008003uL,
            0x8000000000008002uL,
            0x8000000000000080uL,
            0x000000000000800AuL,
            0x800000008000000AuL,
            0x8000000080008081uL,
            0x8000000000008080uL,
            0x0000000080000001uL,
            0x8000000080008008uL
        )
    }
}
