// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multiaddress.Transcoder
import org.erwinkok.multiformat.multibase.bases.Base64
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.getOrElse

class Garlic64Component private constructor(addressBytes: ByteArray) : Component(Protocol.GARLIC64, addressBytes) {
    override val value: String
        get() = Base64.encodeToStringStd(addressBytes).replace("\\+".toRegex(), "-").replace("/".toRegex(), "~")

    companion object : Transcoder {
        override fun bytesToComponent(protocol: Protocol, bytes: ByteArray): Result<Garlic64Component> {
            return Ok(Garlic64Component(bytes))
        }

        override fun stringToComponent(protocol: Protocol, string: String): Result<Garlic64Component> {
            // i2p base64 address will be between 516 and 616 characters long, depending on certificate type
            if (string.length < 516 || string.length > 616) {
                return Err("Invalid garlic addr: $string not a i2p base64 address. len: ${string.length}")
            }
            val replace = string.replace("-".toRegex(), "+").replace("~".toRegex(), "/")
            val bytes = Base64.decodeStringStd(replace)
                .getOrElse { return Err("Invalid garlic addr: ${string.take(16)}... Could not decode Multibase") }
            if (bytes.size < 386) {
                return Err("Invalid garlic64 address length: ${bytes.size}")
            }
            return Ok(Garlic64Component(bytes))
        }
    }
}
