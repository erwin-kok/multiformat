// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multihash.hashers

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.errorMessage
import java.security.MessageDigest

class DoubleSha256 : Hasher {
    private var sha256: MessageDigest = MessageDigest.getInstance("SHA-256")

    override fun write(p: ByteArray): Result<Int> {
        return try {
            sha256.update(p)
            Ok(p.size)
        } catch (e: Exception) {
            Err("Could not update for MessageDigest: ${errorMessage(e)}")
        }
    }

    override fun sum(): ByteArray {
        val digest = sha256.digest()
        sha256.reset()
        return sha256.digest(digest)
    }

    override fun reset() {
        sha256.reset()
    }

    override fun size(): Int {
        return sha256.digestLength
    }

    override fun blockSize(): Int {
        return 32
    }
}
