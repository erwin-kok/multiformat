// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.cid

import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.multihash.Multihash
import org.erwinkok.multiformat.util.readUnsignedVarInt
import org.erwinkok.multiformat.util.writeUnsignedVarInt
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.flatMap
import org.erwinkok.result.getOrElse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

data class Prefix(val version: Int, val codec: Multicodec, val multihashType: Multicodec, val multiHashLength: Int) {
    fun bytes(): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.writeUnsignedVarInt(version)
        stream.writeUnsignedVarInt(codec.code)
        stream.writeUnsignedVarInt(multihashType.code)
        stream.writeUnsignedVarInt(multiHashLength)
        return stream.toByteArray()
    }

    fun sum(data: ByteArray): Result<Cid> {
        var length = multiHashLength
        if (multihashType == Multicodec.IDENTITY) {
            length = -1
        }
        if (version == 0 && (multihashType != Multicodec.SHA2_256 || (multiHashLength != 32 && multiHashLength != -1))) {
            return Err("invalid v0 prefix")
        }
        val hash = Multihash.sum(multihashType, data, length)
            .getOrElse { return Err(it) }
        if (version == 0) {
            return Ok(CidV0(hash))
        } else if (version == 1) {
            return Ok(CidV1(hash, codec))
        }
        return Err("invalid cid version")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Prefix) {
            return false
        }
        return version == other.version &&
            codec == other.codec &&
            multihashType == other.multihashType &&
            multiHashLength == other.multiHashLength
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 32 * result + codec.hashCode()
        result = 32 * result + multihashType.hashCode()
        result = 32 * result + multiHashLength.hashCode()
        return result
    }

    companion object {
        fun fromBytes(buf: ByteArray): Result<Prefix> {
            val stream = ByteArrayInputStream(buf)
            val version = stream.readUnsignedVarInt()
                .getOrElse { return Err(it) }
            val codec = stream.readUnsignedVarInt()
                .flatMap { Multicodec.codeToType(it.toInt()) }
                .getOrElse { return Err(it) }
            val multihashType = stream.readUnsignedVarInt()
                .flatMap { Multicodec.codeToType(it.toInt()) }
                .getOrElse { return Err(it) }
            val multihashLength = stream.readUnsignedVarInt()
                .getOrElse { return Err(it) }
            return Ok(Prefix(version.toInt(), codec, multihashType, multihashLength.toInt()))
        }
    }
}
