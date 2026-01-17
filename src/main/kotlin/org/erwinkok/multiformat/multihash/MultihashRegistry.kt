// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multihash

import io.github.oshai.kotlinlogging.KotlinLogging
import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.multihash.hashers.Blake2b
import org.erwinkok.multiformat.multihash.hashers.Blake2s
import org.erwinkok.multiformat.multihash.hashers.Blake3
import org.erwinkok.multiformat.multihash.hashers.DoubleSha256
import org.erwinkok.multiformat.multihash.hashers.Hasher
import org.erwinkok.multiformat.multihash.hashers.Identity
import org.erwinkok.multiformat.multihash.hashers.MessageDigestHasher
import org.erwinkok.multiformat.multihash.hashers.Sha3
import org.erwinkok.multiformat.multihash.hashers.Shake
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.errorMessage
import org.erwinkok.result.mapBoth
import org.erwinkok.result.onFailure
import org.erwinkok.result.onSuccess
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

object MultihashRegistry {
    private val hashers: MutableMap<Multicodec, (Int) -> Result<Hasher>> = ConcurrentHashMap()
    private val defaultLengths: MutableMap<Multicodec, Int> = ConcurrentHashMap()

    init {
        registerVariableSize(Multicodec.IDENTITY) { Ok(Identity()) }
        register(Multicodec.DBL_SHA2_256) { Ok(DoubleSha256()) }
        registerMessageDigest(Multicodec.MD5, "MD5")
        registerMessageDigest(Multicodec.SHA1, "SHA1")
        registerMessageDigest(Multicodec.SHA2_224, "SHA-224")
        registerMessageDigest(Multicodec.SHA2_256, "SHA-256")
        registerMessageDigest(Multicodec.SHA2_384, "SHA-384")
        registerMessageDigest(Multicodec.SHA2_512, "SHA-512")
        registerMessageDigest(Multicodec.SHA2_512_224, "SHA-512/224")
        registerMessageDigest(Multicodec.SHA2_512_256, "SHA-512/256")

        register(Multicodec.SHA3_224) { Ok(Sha3.sha224()) }
        register(Multicodec.SHA3_256) { Ok(Sha3.sha256()) }
        register(Multicodec.SHA3_384) { Ok(Sha3.sha384()) }
        register(Multicodec.SHA3_512) { Ok(Sha3.sha512()) }
        register(Multicodec.SHAKE_128) { Ok(Shake.shake128()) }
        register(Multicodec.SHAKE_256) { Ok(Shake.shake256()) }
        register(Multicodec.KECCAK_256) { Ok(Sha3.keccak256()) }
        register(Multicodec.KECCAK_512) { Ok(Sha3.keccak512()) }

        registerBlake2()
        registerBlake3()
    }

    fun register(type: Multicodec, hasherConstructor: () -> Result<Hasher>): Result<Unit> {
        return hasherConstructor()
            .mapBoth(
                { hasher ->
                    val maxSize = hasher.size()
                    hashers[type] = { size ->
                        if (size > maxSize) {
                            Err("requested length was too large for digest of type: $type")
                        } else {
                            hasherConstructor()
                        }
                    }
                    defaultLengths[type] = maxSize
                    Ok(Unit)
                },
                {
                    val message = "Could not register hasher for $type: ${errorMessage(it)}"
                    logger.warn { message }
                    Err(message)
                },
            )
    }

    fun registerVariableSize(type: Multicodec, hasher: (Int) -> Result<Hasher>): Result<Unit> {
        return hasher(-1)
            .mapBoth(
                {
                    hashers[type] = hasher
                    defaultLengths[type] = it.size()
                    Ok(Unit)
                },
                {
                    val message = "Could not register hasher for $type: ${errorMessage(it)}"
                    logger.warn { message }
                    Err(message)
                },
            )
    }

    fun registerMessageDigest(type: Multicodec, hasher: String) {
        register(type) {
            try {
                Ok(MessageDigestHasher(MessageDigest.getInstance(hasher)))
            } catch (e: NoSuchAlgorithmException) {
                Err("No MessageDigest found for $hasher")
            }
        }
    }

    fun getHasher(type: Multicodec, sizeHint: Int = -1): Result<Hasher> {
        val hasherFactory = hashers[type] ?: return Err("No hasher found for: $type")
        return hasherFactory(sizeHint)
    }

    fun defaultHashSize(codec: Multicodec): Result<Int> {
        val length = defaultLengths[codec] ?: return Err("No defaulth length for $codec")
        return Ok(length)
    }

    private fun registerBlake2() {
        register(Multicodec.BLAKE2S_256) { Blake2s.fromKey(null) }

        for (i in Multicodec.BLAKE2B_8.code..Multicodec.BLAKE2B_512.code) {
            val size = i - Multicodec.BLAKE2B_8.code + 1
            Multicodec.codeToType(i)
                .onSuccess { code -> register(code) { Blake2b.fromKey(null, size) } }
                .onFailure { logger.warn { "Could not register blake2b_${size * 8}: ${errorMessage(it)}" } }
        }
    }

    private fun registerBlake3() {
        registerVariableSize(Multicodec.BLAKE3) { size ->
            when (size) {
                -1 -> {
                    Ok(Blake3.fromKey(null, 32))
                }

                in 1..128 -> {
                    Ok(Blake3.fromKey(null, size))
                }

                else -> {
                    Err("Unsupported size for Blake3: $size")
                }
            }
        }
    }
}
