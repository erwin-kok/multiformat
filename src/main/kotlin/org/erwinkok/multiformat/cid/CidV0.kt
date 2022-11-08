// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.cid

import org.erwinkok.multiformat.multibase.Multibase
import org.erwinkok.multiformat.multibase.bases.Base58
import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.multihash.Multihash
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class CidV0(multihash: Multihash) : Cid(multihash) {
    override val version: Int
        get() = 0

    override val multicodec: Multicodec
        get() = Multicodec.DAG_PB

    override val multibase: Multibase
        get() = Multibase.BASE58_BTC

    override fun bytes(): ByteArray {
        return multihash.bytes()
    }

    override fun prefix(): Prefix {
        return Prefix(version, Multicodec.DAG_PB, Multicodec.SHA2_256, 32)
    }

    override fun toV0(): Result<Cid> {
        return Ok(CidV0(multihash))
    }

    override fun toV1(): Result<Cid> {
        return Ok(CidV1(multihash, Multicodec.DAG_PB, Multibase.BASE58_BTC))
    }

    override fun copy(): Cid {
        return CidV0(multihash)
    }

    override fun toString(base: Multibase): Result<String> {
        if (base != Multibase.BASE58_BTC) {
            return Err("invalid base encoding")
        }
        return Ok(Base58.encodeToStringBtc(bytes()))
    }

    override fun toString(): String {
        return Base58.encodeToStringBtc(bytes())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CidV0) {
            return false
        }
        return multihash == other.multihash
    }

    override fun hashCode(): Int {
        return multihash.hashCode()
    }
}
