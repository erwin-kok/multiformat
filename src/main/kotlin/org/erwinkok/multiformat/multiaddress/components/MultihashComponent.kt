// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.cid.Cid
import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.multihash.Multihash
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.errorMessage
import org.erwinkok.result.getOrElse
import org.erwinkok.result.map

class MultihashComponent private constructor(val multihash: Multihash) : Component(Protocol.P2P, multihash.bytes()) {
    override val value: String
        get() = multihash.base58()

    companion object {
        fun fromBytes(bytes: ByteArray): Result<MultihashComponent> {
            return Multihash.fromBytes(bytes)
                .map { MultihashComponent(it) }
        }

        fun fromString(address: String): Result<MultihashComponent> {
            val multihash = if (address.startsWith("Qm") || address.startsWith("1")) {
                Multihash.fromBase58(address)
                    .getOrElse { return Err(it) }
            } else {
                val cid = Cid.fromString(address)
                    .getOrElse { return Err("failed to parse p2p address $address: ${errorMessage(it)}") }
                if (cid.multicodec !== Multicodec.LIBP2P_KEY) {
                    return Err("failed to parse p2p address: $address has an invalid codec ${cid.multicodec.typeName}")
                }
                cid.multihash
            }
            return Ok(MultihashComponent(multihash))
        }
    }
}
