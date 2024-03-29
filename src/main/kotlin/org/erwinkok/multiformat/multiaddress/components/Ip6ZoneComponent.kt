// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multiaddress.Transcoder
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.util.Hex

class Ip6ZoneComponent private constructor(val address: String) : Component(Protocol.IP6ZONE, address.toByteArray()) {
    override val value: String
        get() = address

    companion object : Transcoder {
        override fun bytesToComponent(protocol: Protocol, bytes: ByteArray): Result<Ip6ZoneComponent> {
            if (bytes.isEmpty()) {
                return Err("invalid length (should be > 0)")
            }
            if (bytes.indexOf('/'.code.toByte()) >= 0) {
                return Err("IPv6 zone ID contains '/': " + Hex.encode(bytes))
            }
            return Ok(Ip6ZoneComponent(String(bytes)))
        }

        override fun stringToComponent(protocol: Protocol, string: String): Result<Ip6ZoneComponent> {
            if (string.isEmpty()) {
                return Err("Empty IPv6Zone")
            }
            if (string.contains("/")) {
                return Err("IPv6Zone ID contains '/': $string")
            }
            return Ok(Ip6ZoneComponent(string))
        }
    }
}
