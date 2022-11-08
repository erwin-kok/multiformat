// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multihash.hashers

class Shake(rate: Int, outputLen: Int, dsbyte: Byte) : Sha3(rate, outputLen, dsbyte) {

    override fun blockSize(): Int {
        return 32 // Shake does not define a preferred blockSize. Return a the common blockSize of 32
    }

    companion object {
        fun shake128(): Shake {
            return Shake(168, 32, 0x1f)
        }

        fun shake256(): Shake {
            return Shake(136, 64, 0x1f)
        }
    }
}
