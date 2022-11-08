// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.cid

import org.erwinkok.multiformat.multibase.Multibase
import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.multihash.Multihash
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class CidBuilder {
    private var version: Int = 1
    private var multicodec: Multicodec? = null
    private var multihash: Multihash? = null
    private var multibase: Multibase? = null
    fun withVersion(version: Int): CidBuilder {
        this.version = version
        return this
    }

    fun withMulticodec(multicodec: Multicodec): CidBuilder {
        this.multicodec = multicodec
        return this
    }

    fun withMultihash(multihash: Multihash): CidBuilder {
        this.multihash = multihash
        return this
    }

    fun withMultibase(multibase: Multibase): CidBuilder {
        this.multibase = multibase
        return this
    }

    fun build(): Result<Cid> {
        if (version != 0 && version != 1) {
            return Err("Invalid version, must be a number equal to 1 or 0")
        }
        if (version == 0) {
            if (multicodec != null && multicodec != Multicodec.DAG_PB) {
                return Err("codec must be 'dag-pb' for CIDv0")
            }
            if (multibase != null && multibase != Multibase.BASE58_BTC) {
                return Err("multibase must be 'base58btc' for CIDv0")
            }
            val h = multihash ?: return Err("hash must be non-null")
            return Ok(CidV0(h))
        }
        val c = multicodec ?: return Err("codec must be non-null")
        val h = multihash ?: return Err("hash must be non-null")
        return Ok(CidV1(h, c, multibase ?: Multibase.BASE32))
    }
}
