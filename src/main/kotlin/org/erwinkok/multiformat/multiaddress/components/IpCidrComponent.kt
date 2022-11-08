// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.errorMessage

class IpCidrComponent private constructor(addressByte: Byte) : Component(Protocol.IPCIDR, byteArrayOf(addressByte)) {
    override val value: String
        get() = "${addressBytes[0].toUByte()}"

    companion object {
        fun fromBytes(bytes: ByteArray): Result<IpCidrComponent> {
            if (bytes.size != 1) {
                return Err("invalid length (should be == 1)")
            }
            return Ok(IpCidrComponent(bytes[0]))
        }

        fun fromString(string: String): Result<IpCidrComponent> {
            return try {
                val i = Integer.parseUnsignedInt(string, 10)
                if (i > 255) {
                    Err("Invalid cpidr, must be <256")
                } else {
                    Ok(IpCidrComponent(i.toByte()))
                }
            } catch (e: NumberFormatException) {
                Err("Could not parse integer: ${errorMessage(e)}")
            }
        }
    }
}
