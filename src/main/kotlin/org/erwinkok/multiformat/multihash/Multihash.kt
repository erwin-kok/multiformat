// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multihash

import org.erwinkok.multiformat.multibase.bases.Base16
import org.erwinkok.multiformat.multibase.bases.Base58
import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.util.readUnsignedVarInt
import org.erwinkok.multiformat.util.writeUnsignedVarInt
import org.erwinkok.result.Err
import org.erwinkok.result.Error
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.flatMap
import org.erwinkok.result.getOrElse
import org.erwinkok.result.mapBoth
import org.erwinkok.util.Hex
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class Multihash private constructor(val type: Multicodec, val digest: ByteArray) {
    val name: String
        get() = type.typeName

    val code: Int
        get() = type.code

    val length: Int
        get() = digest.size

    fun hex(): String {
        return Hex.encode(bytes())
    }

    fun base58(): String {
        return Base58.encodeToStringBtc(bytes())
    }

    fun bytes(): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.writeUnsignedVarInt(code)
        stream.writeUnsignedVarInt(digest.size)
        stream.writeBytes(digest)
        return stream.toByteArray()
    }

    fun serialize(s: OutputStream) {
        s.write(bytes())
    }

    override fun toString(): String {
        return base58()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is Multihash) {
            return super.equals(other)
        }
        return type == other.type &&
            digest.contentEquals(other.digest)
    }

    override fun hashCode(): Int {
        return digest.contentHashCode() xor type.hashCode()
    }

    companion object {
        private val ErrTooShort = Error("multihash too short. must be >= 2 bytes")
        private val ErrInvalidMultihash = Error("input isn't valid multihash")

        private const val MAX_IDENTITY_HASH_LENGTH = 1024 * 1024

        fun fromHexString(s: String): Result<Multihash> {
            return Base16.decode(s)
                .flatMap { fromBytes(it) }
        }

        fun fromBase58(s: String): Result<Multihash> {
            return Base58.decodeStringBtc(s)
                .mapBoth({ fromBytes(it) }, { Err(ErrInvalidMultihash) })
        }

        fun fromTypeAndDigest(type: Multicodec, digest: ByteArray): Result<Multihash> {
            if (digest.size > Int.MAX_VALUE) {
                return Err("digest too long, supporting only <= 2^31-1")
            }
            return Ok(Multihash(type, digest))
        }

        fun fromBytes(bytes: ByteArray): Result<Multihash> {
            return deserialize(bytes)
        }

        fun fromStream(stream: ByteArrayInputStream): Result<Multihash> {
            return deserialize(stream)
        }

        fun sum(type: Multicodec, data: ByteArray, length: Int = -1): Result<Multihash> {
            val hasher = MultihashRegistry.getHasher(type, length)
                .getOrElse { return Err(it) }
            hasher.write(data)
            var sum = hasher.sum()
            val vlength = if (length < 0) {
                hasher.size()
            } else {
                length
            }
            if (sum.size < vlength) {
                return Err("requested length was too large for digest")
            }
            if (vlength >= 0) {
                if (type == Multicodec.IDENTITY && vlength != sum.size) {
                    return Err("the length of the identity hash must be equal to the length of the data")
                }
                sum = sum.copyOfRange(0, vlength)
            }
            return fromTypeAndDigest(type, sum)
        }

        fun encode(digest: ByteArray, type: Multicodec): Result<Multihash> {
            return fromTypeAndDigest(type, digest)
        }

        fun encodeName(digest: ByteArray, name: String): Result<Multihash> {
            return Multicodec.nameToType(name)
                .flatMap { encode(digest, it) }
        }

        @Deprecated("Have one consistent interface", ReplaceWith("fromBytes(bytes)", "org.erwinkok.libp2p.multiformat.multihash.Multihash.fromBytes"))
        fun cast(buf: ByteArray): Result<Multihash> {
            return fromBytes(buf)
        }

        @Deprecated("Have one consistent interface", ReplaceWith("fromBytes(bytes)", "org.erwinkok.libp2p.multiformat.multihash.Multihash.fromBytes"))
        fun decode(bytes: ByteArray): Result<Multihash> {
            return fromBytes(bytes)
        }

        private fun deserialize(buf: ByteArray): Result<Multihash> {
            return deserialize(ByteArrayInputStream(buf))
        }

        private fun deserialize(din: InputStream): Result<Multihash> {
            if (din.available() < 2) {
                return Err(ErrTooShort)
            }
            val code = din.readUnsignedVarInt()
                .getOrElse { return Err(it) }
                .toInt()
            val type = Multicodec.codeToType(code)
                .getOrElse { return Err(it) }
            val length = din.readUnsignedVarInt()
                .getOrElse { return Err(it) }
                .toInt()
            if (length < 0) {
                return Err("Multihash invalid length: $length")
            }
            if (length > Int.MAX_VALUE) {
                return Err("digest too long, supporting only <= 2^31-1")
            }
            if (din.available() != length) {
                return Err("length greater than remaining number of bytes in buffer")
            }
            val digest = ByteArray(length)
            if (length > 0 && din.read(digest, 0, length) != length) {
                return Err("Error reading Multihash from buffer")
            }
            return fromTypeAndDigest(type, digest)
        }
    }
}
