package org.erwinkok.multiformat.multiaddress.components

import inet.ipaddr.AddressValueException
import inet.ipaddr.IPAddressString
import inet.ipaddr.ipv6.IPv6Address
import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class Ip6Component private constructor(private val ipAddress: IPv6Address) : Component(Protocol.IP6, ipAddress.bytes) {
    override val value: String
        get() = ipAddress.toString()

    companion object {
        fun fromBytes(bytes: ByteArray): Result<Ip6Component> {
            return try {
                Ok(Ip6Component(IPv6Address(bytes)))
            } catch (e: AddressValueException) {
                Err("Invalid IPv6 address")
            }
        }

        fun fromString(string: String): Result<Ip6Component> {
            val ipAddress = IPAddressString(string).address?.toIPv6() ?: return Err("Invalid IPv6 address: $string")
            return Ok(Ip6Component(ipAddress))
        }

        fun fromIPv6Address(address: IPv6Address): Result<Ip6Component> {
            return Ok(Ip6Component(address))
        }
    }
}
