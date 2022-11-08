package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class PortComponent private constructor(protocol: Protocol, val port: Int) : Component(protocol, byteArrayOf((port shr 8).toByte(), port.toByte())) {
    override val value: String
        get() = port.toString()

    companion object {
        fun fromBytes(protocol: Protocol, bytes: ByteArray): Result<PortComponent> {
            val port = ((bytes[0].toInt() shl 8) or (bytes[1].toInt() and 255)) and 0xffff
            return Ok(PortComponent(protocol, port))
        }

        fun fromString(protocol: Protocol, address: String): Result<PortComponent> {
            val port = try {
                address.toInt()
            } catch (e: NumberFormatException) {
                return Err("Failed to parse address $address")
            }
            if (port > 65535) {
                return Err("Failed to parse address $address (> 65535)")
            }
            return Ok(PortComponent(protocol, port))
        }
    }
}
