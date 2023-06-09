// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multiaddress.Transcoder
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class PortComponent private constructor(protocol: Protocol, private val port: Int) : Component(protocol, byteArrayOf((port shr 8).toByte(), port.toByte())) {
    override val value: String
        get() = port.toString()

    companion object : Transcoder {
        override fun bytesToComponent(protocol: Protocol, bytes: ByteArray): Result<PortComponent> {
            val port = ((bytes[0].toInt() shl 8) or (bytes[1].toInt() and 255)) and 0xffff
            return Ok(PortComponent(protocol, port))
        }

        override fun stringToComponent(protocol: Protocol, string: String): Result<PortComponent> {
            val port = try {
                string.toInt()
            } catch (e: NumberFormatException) {
                return Err("Failed to parse address $string")
            }
            if (port > 65535) {
                return Err("Failed to parse address $string (> 65535)")
            }
            return Ok(PortComponent(protocol, port))
        }
    }
}
