// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import inet.ipaddr.AddressValueException
import inet.ipaddr.IPAddressString
import inet.ipaddr.ipv6.IPv6Address
import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multiaddress.Transcoder
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class Ip6Component private constructor(private val ipAddress: IPv6Address) : Component(Protocol.IP6, ipAddress.bytes) {
    override val value: String
        get() = ipAddress.toString()

    companion object : Transcoder {
        override fun bytesToComponent(protocol: Protocol, bytes: ByteArray): Result<Ip6Component> {
            return try {
                Ok(Ip6Component(IPv6Address(bytes)))
            } catch (e: AddressValueException) {
                Err("Invalid IPv6 address")
            }
        }

        override fun stringToComponent(protocol: Protocol, string: String): Result<Ip6Component> {
            val ipAddress = IPAddressString(string).address?.toIPv6() ?: return Err("Invalid IPv6 address: $string")
            return Ok(Ip6Component(ipAddress))
        }

        fun fromIPv6Address(address: IPv6Address): Result<Ip6Component> {
            return Ok(Ip6Component(address))
        }
    }
}
