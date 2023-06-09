// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multiaddress.Transcoder
import org.erwinkok.multiformat.multibase.bases.Base32
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.errorMessage
import org.erwinkok.result.getOrElse
import org.erwinkok.util.Hex

class Garlic32Component private constructor(addressBytes: ByteArray) : Component(Protocol.GARLIC32, addressBytes) {
    override val value: String
        get() {
            var encode = Base32.encodeStdLowerPad(addressBytes)
            while (encode.endsWith("=")) {
                encode = encode.substring(0, encode.length - 1)
            }
            return encode
        }

    companion object : Transcoder {
        override fun bytesToComponent(protocol: Protocol, bytes: ByteArray): Result<Garlic32Component> {
            if (bytes.size < 35 && bytes.size != 32) {
                return Err("Invalid garlic address: ${Hex.encode(bytes)} not a i2p base32 address. len: ${bytes.size}")
            }
            return Ok(Garlic32Component(bytes))
        }

        override fun stringToComponent(protocol: Protocol, string: String): Result<Garlic32Component> {
            // an i2p base32 address with a length of greater than 55 characters is
            // using an Encrypted Leaseset v2. all other base32 addresses will always be
            // exactly 52 characters
            if (string.length < 55 && string.length != 52) {
                return Err("Invalid garlic addr: $string not a i2p base32 address. len: ${string.length}")
            }
            var vaddr = string
            while (vaddr.length % 8 != 0) {
                vaddr += "="
            }
            val bytes = Base32.decodeStdPad(vaddr)
                .getOrElse { return Err("invalid value $string for protocol Garlic32: ${errorMessage(it)}") }
            if (bytes.size < 35 && bytes.size != 32) {
                return Err("Invalid garlic address: ${Hex.encode(bytes)} not a i2p base32 address. len: ${bytes.size}")
            }
            return Ok(Garlic32Component(bytes))
        }
    }
}
