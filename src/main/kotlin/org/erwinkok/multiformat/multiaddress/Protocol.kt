// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress

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

enum class Protocol constructor(val codec: Multicodec, val size: Int, val resolvable: Boolean = false, val path: Boolean = false) {
    IP4(Multicodec.IP4, 32),
    TCP(Multicodec.TCP, 16),
    DCCP(Multicodec.DCCP, 16),
    IP6(Multicodec.IP6, 128),
    IP6ZONE(Multicodec.IP6ZONE, -1),
    DNS(Multicodec.DNS, -1, true, false), // 4 or 6
    DNS4(Multicodec.DNS4, -1, true, false),
    DNS6(Multicodec.DNS6, -1, true, false),
    DNSADDR(Multicodec.DNSADDR, -1, true, false),
    SCTP(Multicodec.SCTP, 16),
    UDP(Multicodec.UDP, 16),
    P2P_WEBRTC_STAR(Multicodec.P2P_WEBRTC_STAR, 0),
    P2P_WEBRTC_DIRECT(Multicodec.P2P_WEBRTC_DIRECT, 0),
    P2P_STARDUST(Multicodec.P2P_STARDUST, 0),
    P2P_CIRCUIT(Multicodec.P2P_CIRCUIT, 0),
    UDT(Multicodec.UDT, 0),
    UTP(Multicodec.UTP, 0),
    UNIX(Multicodec.UNIX, -1, false, true),
    P2P(Multicodec.P2P, -1),
    IPFS(Multicodec.IPFS, -1), // alias for backwards compatibility
    HTTPS(Multicodec.HTTPS, 0), // deprecated alias for /tls/http
    ONION(Multicodec.ONION, 96), // also for backwards compatibility
    ONION3(Multicodec.ONION3, 296),
    GARLIC64(Multicodec.GARLIC64, -1),
    GARLIC32(Multicodec.GARLIC32, -1),
    TLS(Multicodec.TLS, 0),
    NOISE(Multicodec.NOISE, 0),
    QUIC(Multicodec.QUIC, 0),
    WS(Multicodec.WS, 0),
    WSS(Multicodec.WSS, 0), // deprecated alias for /tls/ws
    P2P_WEBSOCKET_STAR(Multicodec.P2P_WEBSOCKET_STAR, 0),
    HTTP(Multicodec.HTTP, 0),
    PLAINTEXTV2(Multicodec.PLAINTEXTV2, 0),
    IPCIDR(Multicodec.IPCIDR, 8),
    WEBTRANSPORT(Multicodec.WEBTRANSPORT, 0),
    CERTHASH(Multicodec.CERTHASH, -1),
    WEBRTC(Multicodec.WEBRTC, 0);

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

    override fun toString(): String {
        return codec.typeName
    }

    companion object {
        private val byName = mutableMapOf<String, Protocol>()
        private val byCode = mutableMapOf<Int, Protocol>()

        init {
            for (t in values()) {
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
