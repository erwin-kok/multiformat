// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multiaddress.Transcoder
import org.erwinkok.multiformat.multibase.bases.Base16
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.errorMessage
import org.erwinkok.result.getOrElse
import org.erwinkok.util.Hex

class GenericComponent private constructor(protocol: Protocol, addressBytes: ByteArray) : Component(protocol, addressBytes) {
    override val value: String
        get() = Hex.encode(addressBytes)

    companion object : Transcoder {
        override fun bytesToComponent(protocol: Protocol, bytes: ByteArray): Result<GenericComponent> {
            return Ok(GenericComponent(protocol, bytes))
        }

        override fun stringToComponent(protocol: Protocol, string: String): Result<GenericComponent> {
            val bytes = Base16.decode(string)
                .getOrElse { return Err("invalid value $string for protocol ${protocol.codec.typeName}: ${errorMessage(it)}") }
            return Ok(GenericComponent(protocol, bytes))
        }
    }
}
