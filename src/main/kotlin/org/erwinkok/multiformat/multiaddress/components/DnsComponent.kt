// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class DnsComponent private constructor(protocol: Protocol, val address: String) : Component(protocol, address.toByteArray()) {
    override val value: String
        get() = address

    companion object {
        fun fromBytes(protocol: Protocol, bytes: ByteArray): Result<DnsComponent> {
            return Ok(DnsComponent(protocol, String(bytes)))
        }

        fun fromString(protocol: Protocol, string: String): Result<DnsComponent> {
            return Ok(DnsComponent(protocol, string))
        }
    }
}
