// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress

import mu.KotlinLogging
import org.erwinkok.multiformat.multiaddress.components.CertHashComponent
import org.erwinkok.multiformat.multiaddress.components.Component
import org.erwinkok.multiformat.multiaddress.components.DnsComponent
import org.erwinkok.multiformat.multiaddress.components.Garlic32Component
import org.erwinkok.multiformat.multiaddress.components.Garlic64Component
import org.erwinkok.multiformat.multiaddress.components.GenericComponent
import org.erwinkok.multiformat.multiaddress.components.Ip4Component
import org.erwinkok.multiformat.multiaddress.components.Ip6Component
import org.erwinkok.multiformat.multiaddress.components.Ip6ZoneComponent
import org.erwinkok.multiformat.multiaddress.components.IpCidrComponent
import org.erwinkok.multiformat.multiaddress.components.MultihashComponent
import org.erwinkok.multiformat.multiaddress.components.OnionComponent
import org.erwinkok.multiformat.multiaddress.components.PortComponent
import org.erwinkok.multiformat.multiaddress.components.UnixComponent
import org.erwinkok.multiformat.multibase.Multibase
import org.erwinkok.multiformat.multihash.Multihash
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.errorMessage
import org.erwinkok.result.flatMap
import org.erwinkok.result.getOrElse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private val logger = KotlinLogging.logger {}

class Multiaddress private constructor(val components: List<Component>) {
    private val _string: String by lazy { constructString() }
    val bytes: ByteArray by lazy { constructBytes() }

    fun encapsulate(address: String): Result<Multiaddress> {
        return fromString(address)
            .flatMap { encapsulate(it) }
    }

    fun encapsulate(multiAddress: Multiaddress): Result<Multiaddress> {
        val thisPath = this.toString()
        return if (thisPath == "/") {
            fromMultiaddress(multiAddress)
        } else {
            fromString(thisPath + multiAddress)
        }
    }

    fun decapsulate(multiAddress: Multiaddress): Result<Multiaddress> {
        return decapsulate(multiAddress.toString())
    }

    fun decapsulate(address: String): Result<Multiaddress> {
        val s = this.toString()
        val i = s.lastIndexOf(address)
        return if (i < 0) {
            fromMultiaddress(this)
        } else {
            fromString(s.substring(0, i))
        }
    }

    fun protocols(): List<Protocol> {
        return components.map { obj -> obj.protocol }
    }

    fun valueForProtocol(p: Protocol): String? {
        return components.filter { i -> i.protocol == p }.map { it.value }.firstOrNull()
    }

    fun hasProtocol(p: Protocol): Boolean {
        return components.any { i -> i.protocol == p }
    }

    override fun toString(): String {
        return _string
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is Multiaddress) {
            return super.equals(other)
        }
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    private fun constructBytes(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        components.forEach {
            Protocol.writeTo(outputStream, it.protocol, it.addressBytes)
        }
        return outputStream.toByteArray()
    }

    private fun constructString(): String {
        val stringBuilder = StringBuilder()
        components.forEach {
            Protocol.writeTo(stringBuilder, it.protocol, it.value)
        }
        return cleanPath(stringBuilder.toString())
    }

    private fun cleanPath(str: String): String {
        val split = str.trim { it <= ' ' }.split("/")
        return "/" + split.filter { cs -> cs.isNotBlank() }.joinToString("/")
    }

    companion object {
        private const val MaxBytes = 1024

        fun fromMultihash(hash: Multihash): Result<Multiaddress> {
            val encoded = Multibase.BASE58_BTC.encode(hash.digest)
            return fromString("/ipfs/" + encoded.substring(1))
        }

        fun fromString(address: String): Result<Multiaddress> {
            if (address.length > MaxBytes) {
                return Err("Multiaddress is too long, over $MaxBytes bytes. Rejecting.")
            }
            if (address.isNotEmpty() && address[0] != '/') {
                return Err("multiaddress $address must start with a '/'")
            }
            val parts = address.trimEnd('/').split("/").toMutableList()
            if (parts.removeFirst().isNotBlank()) {
                return Err("failed to parse Multiaddress ${address.trimEnd('/')}: must begin with /")
            }
            val components = mutableListOf<Component>()
            while (parts.isNotEmpty()) {
                val (protocol, addressString) = Protocol.readFrom(parts)
                    .getOrElse { return Err("failed to parse Multiaddress $address: ${errorMessage(it)}") }
                val component = fromProtocolAndString(protocol, addressString)
                    .getOrElse { return Err("failed to parse Multiaddress $address: ${errorMessage(it)}") }
                components.add(component)
            }
            return Ok(Multiaddress(components))
        }

        fun fromBytes(bytes: ByteArray): Result<Multiaddress> {
            if (bytes.size > MaxBytes) {
                return Err("Multiaddress is too long, over $MaxBytes bytes. Rejecting.")
            }
            val stream = ByteArrayInputStream(bytes)
            val components = mutableListOf<Component>()
            while (stream.available() > 0) {
                val (protocol, addressBytes) = Protocol.readFrom(stream)
                    .getOrElse { return Err(it) }
                val component = fromProtocolAndBytes(protocol, addressBytes)
                    .getOrElse { return Err(it) }
                components.add(component)
            }
            return Ok(Multiaddress(components))
        }

        private fun fromMultiaddress(other: Multiaddress): Result<Multiaddress> {
            return Ok(Multiaddress(other.components))
        }

        private fun fromProtocolAndBytes(protocol: Protocol, bytes: ByteArray): Result<Component> {
            return when (protocol) {
                Protocol.IP4 -> Ip4Component.fromBytes(bytes)
                Protocol.IP6 -> Ip6Component.fromBytes(bytes)
                Protocol.IP6ZONE -> Ip6ZoneComponent.fromBytes(bytes)
                Protocol.TCP -> PortComponent.fromBytes(Protocol.TCP, bytes)
                Protocol.UDP -> PortComponent.fromBytes(Protocol.UDP, bytes)
                Protocol.DCCP -> PortComponent.fromBytes(Protocol.DCCP, bytes)
                Protocol.SCTP -> PortComponent.fromBytes(Protocol.SCTP, bytes)
                Protocol.DNS -> DnsComponent.fromBytes(Protocol.DNS, bytes)
                Protocol.DNS4 -> DnsComponent.fromBytes(Protocol.DNS4, bytes)
                Protocol.DNS6 -> DnsComponent.fromBytes(Protocol.DNS6, bytes)
                Protocol.DNSADDR -> DnsComponent.fromBytes(Protocol.DNSADDR, bytes)
                Protocol.UNIX -> UnixComponent.fromBytes(bytes)
                Protocol.P2P -> MultihashComponent.fromBytes(bytes)
                Protocol.IPFS -> MultihashComponent.fromBytes(bytes)
                Protocol.ONION -> OnionComponent.fromBytes(Protocol.ONION, bytes)
                Protocol.ONION3 -> OnionComponent.fromBytes(Protocol.ONION3, bytes)
                Protocol.GARLIC32 -> Garlic32Component.fromBytes(bytes)
                Protocol.GARLIC64 -> Garlic64Component.fromBytes(bytes)
                Protocol.CERTHASH -> CertHashComponent.fromBytes(bytes)
                Protocol.IPCIDR -> IpCidrComponent.fromBytes(bytes)
                else -> {
                    logger.debug { "No specific component for $protocol. Defaulting to GenericComponent" }
                    GenericComponent.fromBytes(protocol, bytes)
                }
            }
        }

        private fun fromProtocolAndString(protocol: Protocol, string: String): Result<Component> {
            return when (protocol) {
                Protocol.IP4 -> Ip4Component.fromString(string)
                Protocol.IP6 -> Ip6Component.fromString(string)
                Protocol.IP6ZONE -> Ip6ZoneComponent.fromString(string)
                Protocol.TCP -> PortComponent.fromString(Protocol.TCP, string)
                Protocol.UDP -> PortComponent.fromString(Protocol.UDP, string)
                Protocol.DCCP -> PortComponent.fromString(Protocol.DCCP, string)
                Protocol.SCTP -> PortComponent.fromString(Protocol.SCTP, string)
                Protocol.DNS -> DnsComponent.fromString(Protocol.DNS, string)
                Protocol.DNS4 -> DnsComponent.fromString(Protocol.DNS4, string)
                Protocol.DNS6 -> DnsComponent.fromString(Protocol.DNS6, string)
                Protocol.DNSADDR -> DnsComponent.fromString(Protocol.DNSADDR, string)
                Protocol.UNIX -> UnixComponent.fromString(string)
                Protocol.P2P -> MultihashComponent.fromString(string)
                Protocol.IPFS -> MultihashComponent.fromString(string)
                Protocol.ONION -> OnionComponent.fromString(Protocol.ONION, string)
                Protocol.ONION3 -> OnionComponent.fromString(Protocol.ONION3, string)
                Protocol.GARLIC32 -> Garlic32Component.fromString(string)
                Protocol.GARLIC64 -> Garlic64Component.fromString(string)
                Protocol.CERTHASH -> CertHashComponent.fromString(string)
                Protocol.IPCIDR -> IpCidrComponent.fromString(string)
                else -> {
                    logger.debug { "No specific component for $protocol. Defaulting to GenericComponent" }
                    GenericComponent.fromString(protocol, string)
                }
            }
        }
    }
}
