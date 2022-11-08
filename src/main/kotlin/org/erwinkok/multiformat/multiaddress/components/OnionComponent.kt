package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multibase.bases.Base32
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.getOrElse

class OnionComponent private constructor(protocol: Protocol, val hostBytes: ByteArray, val port: Int) : Component(protocol, hostBytes + byteArrayOf((port shr 8).toByte(), port.toByte())) {
    override val value: String
        get() {
            val addressString = Base32.encodeStdLowerNoPad(hostBytes)
            return "$addressString:$port"
        }

    companion object {
        fun fromBytes(protocol: Protocol, bytes: ByteArray): Result<OnionComponent> {
            val hostBytes = bytes.copyOfRange(0, bytes.size - 2)
            val port = ((bytes[bytes.size - 2].toInt() shl 8) or (bytes[bytes.size - 1].toInt() and 255)) and 0xffff
            return Ok(OnionComponent(protocol, hostBytes, port))
        }

        fun fromString(protocol: Protocol, address: String): Result<OnionComponent> {
            val len = if (protocol == Protocol.ONION) {
                16
            } else {
                56
            }
            val dlen = if (protocol == Protocol.ONION) {
                10
            } else {
                35
            }
            val split = address.split(":")
            if (split.size != 2) {
                return Err("failed to parse onion address: $address does not contain a port number")
            }
            // onion address without the ".onion" substring
            if (split[0].length != len) {
                return Err("failed to parse onion address: $address not a Tor onion address.")
            }
            // onion addresses do not include the multibase prefix, add it before decoding
            val onionHostBytes = Base32.decodeStdNoPad(split[0])
                .getOrElse { return Err("Invalid onion address host: ${split[0]}") }
            if (onionHostBytes.size != dlen) {
                return Err("Invalid onion address host: ${split[0]}")
            }
            val port = split[1].toInt()
            if (port < 1 || port > 65535) {
                return Err("Port number is not in range(1, 65536): $port")
            }
            return Ok(OnionComponent(protocol, onionHostBytes, port))
        }
    }
}
