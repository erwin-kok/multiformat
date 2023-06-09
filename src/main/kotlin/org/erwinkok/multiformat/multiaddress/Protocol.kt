// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress

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
import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.util.readUnsignedVarInt
import org.erwinkok.multiformat.util.writeUnsignedVarInt
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.getOrElse
import org.erwinkok.result.map
import org.erwinkok.util.Tuple
import org.erwinkok.util.Tuple2
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private const val LengthPrefixedVarSize = -1

enum class Protocol constructor(
    // Code is the protocol's multicodec (a normal, non-varint number).
    val codec: Multicodec,

    // Size is the size of the argument to this protocol.
    //
    // * Size == 0 means this protocol takes no argument.
    // * Size >  0 means this protocol takes a constant sized argument.
    // * Size <  0 means this protocol takes a variable length, varint prefixed argument.
    val size: Int,

    // Path indicates a path protocol (e.g., unix). When parsing multiaddr
    // strings, path protocols consume the remainder of the address instead
    // of stopping at the next forward slash.
    //
    // Size must be LengthPrefixedVarSize.
    val path: Boolean,

    // Transcoder converts between the byte representation and the string
    // representation of this protocol's argument (if any).
    //
    // This should only be non-nil if Size != 0
    val transcoder: Transcoder?,
) {
    IP4(Multicodec.IP4, 32, false, Ip4Component),
    TCP(Multicodec.TCP, 16, false, PortComponent),
    DNS(Multicodec.DNS, LengthPrefixedVarSize, false, DnsComponent), // 4 or 6
    DNS4(Multicodec.DNS4, LengthPrefixedVarSize, false, DnsComponent),
    DNS6(Multicodec.DNS6, LengthPrefixedVarSize, false, DnsComponent),
    DNSADDR(Multicodec.DNSADDR, LengthPrefixedVarSize, false, DnsComponent),
    UDP(Multicodec.UDP, 16, false, PortComponent),
    DCCP(Multicodec.DCCP, 16, false, PortComponent),
    IP6(Multicodec.IP6, 128, false, Ip6Component),
    IPCIDR(Multicodec.IPCIDR, 8, false, IpCidrComponent),
    IP6ZONE(Multicodec.IP6ZONE, LengthPrefixedVarSize, false, Ip6ZoneComponent),
    SCTP(Multicodec.SCTP, 16, false, PortComponent),
    P2P_CIRCUIT(Multicodec.P2P_CIRCUIT, 0, false, null),
    ONION(Multicodec.ONION, 96, false, OnionComponent), // also for backwards compatibility
    ONION3(Multicodec.ONION3, 296, false, OnionComponent),
    GARLIC32(Multicodec.GARLIC32, LengthPrefixedVarSize, false, Garlic32Component),
    GARLIC64(Multicodec.GARLIC64, LengthPrefixedVarSize, false, Garlic64Component),
    UTP(Multicodec.UTP, 0, false, null),
    UDT(Multicodec.UDT, 0, false, null),
    QUIC(Multicodec.QUIC, 0, false, null),
    QUIC_V1(Multicodec.QUIC_V1, 0, false, null),
    WEBTRANSPORT(Multicodec.WEBTRANSPORT, 0, false, null),
    CERTHASH(Multicodec.CERTHASH, LengthPrefixedVarSize, false, CertHashComponent),
    HTTP(Multicodec.HTTP, 0, false, null),
    HTTPS(Multicodec.HTTPS, 0, false, null), // deprecated alias for /tls/http
    P2P(Multicodec.P2P, LengthPrefixedVarSize, false, MultihashComponent),
    IPFS(Multicodec.IPFS, LengthPrefixedVarSize, false, MultihashComponent), // alias for backwards compatibility
    UNIX(Multicodec.UNIX, LengthPrefixedVarSize, true, UnixComponent),
    P2P_WEBRTC_DIRECT(Multicodec.P2P_WEBRTC_DIRECT, 0, false, null), // Deprecated. use webrtc-direct instead
    TLS(Multicodec.TLS, 0, false, null),
    SNI(Multicodec.SNI, LengthPrefixedVarSize, false, DnsComponent),
    NOISE(Multicodec.NOISE, 0, false, null),
    PLAINTEXTV2(Multicodec.PLAINTEXTV2, 0, false, null),
    WS(Multicodec.WS, 0, false, null),
    WSS(Multicodec.WSS, 0, false, null), // deprecated alias for /tls/ws
    WEBRTC_DIRECT(Multicodec.WEBRTC_DIRECT, 0, false, null),
    WEBRTC(Multicodec.WEBRTC, 0, false, null),

    P2P_WEBRTC_STAR(Multicodec.P2P_WEBRTC_STAR, 0, false, null),
    P2P_STARDUST(Multicodec.P2P_STARDUST, 0, false, null),
    P2P_WEBSOCKET_STAR(Multicodec.P2P_WEBSOCKET_STAR, 0, false, null),
    ;

    fun sizeForAddress(stream: ByteArrayInputStream): Result<Int> {
        if (size > 0) {
            return Ok(size / 8)
        }
        return if (size == 0) {
            Ok(0)
        } else {
            stream.readUnsignedVarInt()
                .map { it.toInt() }
        }
    }

    fun bytesToComponent(bytes: ByteArray): Result<Component> {
        val t = transcoder ?: GenericComponent
        return t.bytesToComponent(this, bytes)
    }

    fun stringToComponent(string: String): Result<Component> {
        val t = transcoder ?: GenericComponent
        return t.stringToComponent(this, string)
    }

    override fun toString(): String {
        return codec.typeName
    }

    companion object {
        private val byName = mutableMapOf<String, Protocol>()
        private val byCode = mutableMapOf<Int, Protocol>()

        init {
            for (t in values()) {
                require(t.size == 0 || t.transcoder != null) { "protocols with arguments must define transcoders" }
                require(!t.path || t.size < 0) { "protocols with arguments must define transcoders" }
                byName[t.codec.typeName.lowercase()] = t
                if (t != IPFS) {
                    byCode[t.codec.code] = t
                }
            }
        }

        fun withName(name: String): Protocol? {
            if (name == "ipfs") {
                return P2P
            }
            return byName[name.lowercase()]
        }

        fun withCode(code: Int): Protocol? {
            if (code == 0x01a5) {
                return P2P
            }
            return byCode[code]
        }

        fun readFrom(stream: ByteArrayInputStream): Result<Tuple2<Protocol, ByteArray>> {
            val code = stream.readUnsignedVarInt()
                .getOrElse { return Err(it) }
            val protocol = Protocol.withCode(code.toInt()) ?: return Err("no protocol with code $code")
            val addressBytes: ByteArray?
            if (protocol.size != 0) {
                val size = protocol.sizeForAddress(stream)
                    .getOrElse { return Err(it) }
                if (stream.available() < size || size < 0) {
                    return Err("invalid value for size " + stream.available())
                }
                addressBytes = ByteArray(size)
                stream.read(addressBytes, 0, size)
            } else {
                addressBytes = byteArrayOf()
            }
            return Ok(Tuple(protocol, addressBytes))
        }

        fun readFrom(list: MutableList<String>): Result<Tuple2<Protocol, String>> {
            val part = list.removeFirst()
            val protocol = Protocol.withName(part) ?: return Err("no protocol with name $part")
            val addressString = if (protocol.size != 0) {
                if (list.isEmpty()) {
                    return Err("unexpected end of Multiaddress")
                }
                if (protocol.path) {
                    // if it's a path protocol, take the rest
                    val path = list.joinToString("/")
                    list.clear()
                    "/$path"
                } else {
                    list.removeFirst()
                }
            } else {
                ""
            }
            return Ok(Tuple(protocol, addressString))
        }

        fun writeTo(outputStream: ByteArrayOutputStream, protocol: Protocol, addressBytes: ByteArray) {
            outputStream.writeUnsignedVarInt(protocol.codec.code)
            if (protocol.size < 0) {
                outputStream.writeUnsignedVarInt(addressBytes.size)
            }
            outputStream.writeBytes(addressBytes)
        }

        fun writeTo(stringBuilder: StringBuilder, protocol: Protocol, addressString: String) {
            stringBuilder.append("/${protocol.codec.typeName}")
            if (addressString.isNotEmpty()) {
                if (!protocol.path || addressString[0] != '/') {
                    stringBuilder.append("/")
                }
                stringBuilder.append(addressString)
            }
        }
    }
}
