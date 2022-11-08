// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress

import inet.ipaddr.IPAddressString
import org.erwinkok.multiformat.multiaddress.components.CertHashComponent
import org.erwinkok.multiformat.multiaddress.components.Component
import org.erwinkok.multiformat.multiaddress.components.DnsComponent
import org.erwinkok.multiformat.multiaddress.components.GenericComponent
import org.erwinkok.multiformat.multiaddress.components.Ip4Component
import org.erwinkok.multiformat.multiaddress.components.Ip6Component
import org.erwinkok.multiformat.multiaddress.components.MultihashComponent
import org.erwinkok.multiformat.multiaddress.components.OnionComponent
import org.erwinkok.multiformat.multiaddress.components.PortComponent
import org.erwinkok.multiformat.multibase.Multibase
import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.erwinkok.util.Hex
import org.erwinkok.util.Tuple3
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.ByteArrayOutputStream
import java.util.stream.Stream

internal class ComponentsTest {
    @Test
    fun handlesIp4Buffers() {
        val actual = Ip4Component.fromBytes(Hex.decode("c0a80001").expectNoErrors()).expectNoErrors()
        assertEquals("/ip4/192.168.0.1", actual.toString())
    }

    @Test
    fun handlesIp6Buffers() {
        val actual = Ip6Component.fromBytes(Hex.decode("abcd0000000100020003000400050006").expectNoErrors()).expectNoErrors()
        assertEquals("/ip6/abcd:0:1:2:3:4:5:6", actual.toString())
    }

    @Test
    fun handlesIp4Strings() {
        val actual = Ip4Component.fromString("192.168.0.1").expectNoErrors().addressBytes
        Assertions.assertArrayEquals(Hex.decode("c0a80001").expectNoErrors(), actual)
    }

    @Test
    fun handlesIp6Strings() {
        val actual = Ip6Component.fromString("ABCD::1:2:3:4:5:6").expectNoErrors().addressBytes
        Assertions.assertArrayEquals(Hex.decode("ABCD0000000100020003000400050006").expectNoErrors(), actual)
    }

    @Test
    fun throwsOnInvalidIp4Conversion() {
        assertErrorResult("Invalid IPv4 address: 555.168.0.1") { Ip4Component.fromString("555.168.0.1") }
    }

    @Test
    fun throwsOnInvalidIp6Conversion() {
        assertErrorResult("Invalid IPv6 address: FFFF::GGGG") { Ip6Component.fromString("FFFF::GGGG") }
    }

    @TestFactory
    fun construct(): Stream<DynamicTest> {
        val local = IPAddressString("127.0.0.1").address.toIPv4()
        val addr6 = IPAddressString("2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095").address.toIPv6()
        val decoded = Multibase.decode("uEiDDq4_xNyDorZBH3TlGazyJdOWSwvo4PUo5YHFMrvDE8g").expectNoErrors()
        return listOf(
            Tuple3(
                "/ip4/1.2.3.4",
                "0401020304",
                listOf(
                    Ip4Component.fromIPv4Address(IPAddressString("1.2.3.4").address.toIPv4()).expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/0.0.0.0",
                "0400000000",
                listOf(
                    Ip4Component.fromIPv4Address(IPAddressString("0.0.0.0").address.toIPv4()).expectNoErrors()
                )
            ),
            Tuple3(
                "/ip6/::1",
                "2900000000000000000000000000000001",
                listOf(
                    Ip6Component.fromIPv6Address(IPAddressString("::1").address.toIPv6()).expectNoErrors()
                )
            ),
            Tuple3(
                "/ip6/2601:9:4f81:9700:803e:ca65:66e8:c21",
                "29260100094F819700803ECA6566E80C21",
                listOf(
                    Ip6Component.fromIPv6Address(IPAddressString("2601:9:4f81:9700:803e:ca65:66e8:c21").address.toIPv6()).expectNoErrors()
                )
            ),
            Tuple3(
                "/udp/0",
                "91020000",
                listOf(
                    PortComponent.fromString(Protocol.UDP, "0").expectNoErrors()
                )
            ),
            Tuple3(
                "/tcp/0",
                "060000",
                listOf(
                    PortComponent.fromString(Protocol.TCP, "0").expectNoErrors()
                )
            ),
            Tuple3(
                "/sctp/0",
                "84010000",
                listOf(
                    PortComponent.fromString(Protocol.SCTP, "0").expectNoErrors()
                )
            ),

            Tuple3(
                "/udp/1234",
                "910204D2",
                listOf(
                    PortComponent.fromString(Protocol.UDP, "1234").expectNoErrors()
                )
            ),
            Tuple3(
                "/tcp/1234",
                "0604D2",
                listOf(
                    PortComponent.fromString(Protocol.TCP, "1234").expectNoErrors()
                )
            ),
            Tuple3(
                "/sctp/1234",
                "840104D2",
                listOf(
                    PortComponent.fromString(Protocol.SCTP, "1234").expectNoErrors()
                )
            ),
            Tuple3(
                "/udp/65535",
                "9102FFFF",
                listOf(
                    PortComponent.fromString(Protocol.UDP, "65535").expectNoErrors()
                )
            ),
            Tuple3(
                "/tcp/65535",
                "06FFFF",
                listOf(
                    PortComponent.fromString(Protocol.TCP, "65535").expectNoErrors()
                )
            ),
            Tuple3(
                "/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC",
                "A503221220D52EBB89D85B02A284948203A62FF28389C57C9F42BEEC4EC20DB76A68911C0B",
                listOf(
                    MultihashComponent.fromString("QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC").expectNoErrors()
                )
            ),
            Tuple3(
                "/udp/1234/sctp/1234",
                "910204D2840104D2",
                listOf(
                    PortComponent.fromString(Protocol.UDP, "1234").expectNoErrors(),
                    PortComponent.fromString(Protocol.SCTP, "1234").expectNoErrors()
                )
            ),
            Tuple3(
                "/udp/1234/udt",
                "910204D2AD02",
                listOf(
                    PortComponent.fromString(Protocol.UDP, "1234").expectNoErrors(),
                    GenericComponent.fromBytes(Protocol.UDT, byteArrayOf()).expectNoErrors()
                )
            ),
            Tuple3(
                "/udp/1234/utp",
                "910204D2AE02",
                listOf(
                    PortComponent.fromString(Protocol.UDP, "1234").expectNoErrors(),
                    GenericComponent.fromBytes(Protocol.UTP, byteArrayOf()).expectNoErrors()
                )
            ),
            Tuple3(
                "/tcp/1234/http",
                "0604D2E003",
                listOf(
                    PortComponent.fromString(Protocol.TCP, "1234").expectNoErrors(),
                    GenericComponent.fromBytes(Protocol.HTTP, byteArrayOf()).expectNoErrors()
                )
            ),
            Tuple3(
                "/tcp/1234/tls/http",
                "0604D2C003E003",
                listOf(
                    PortComponent.fromString(Protocol.TCP, "1234").expectNoErrors(),
                    GenericComponent.fromBytes(Protocol.TLS, byteArrayOf()).expectNoErrors(),
                    GenericComponent.fromBytes(Protocol.HTTP, byteArrayOf()).expectNoErrors()
                )
            ),
            Tuple3(
                "/tcp/1234/https",
                "0604D2BB03",
                listOf(
                    PortComponent.fromString(Protocol.TCP, "1234").expectNoErrors(),
                    GenericComponent.fromBytes(Protocol.HTTPS, byteArrayOf()).expectNoErrors()
                )
            ),
            Tuple3(
                "/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC/tcp/1234",
                "A503221220D52EBB89D85B02A284948203A62FF28389C57C9F42BEEC4EC20DB76A68911C0B0604D2",
                listOf(
                    MultihashComponent.fromString("QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC").expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "1234").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/udp/1234",
                "047F000001910204D2",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.UDP, "1234").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/udp/0",
                "047F00000191020000",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.UDP, "0").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/tcp/1234",
                "047F0000010604D2",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "1234").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC",
                "047F000001A503221220D52EBB89D85B02A284948203A62FF28389C57C9F42BEEC4EC20DB76A68911C0B",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    MultihashComponent.fromString("QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC/tcp/1234",
                "047F000001A503221220D52EBB89D85B02A284948203A62FF28389C57C9F42BEEC4EC20DB76A68911C0B0604D2",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    MultihashComponent.fromString("QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC").expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "1234").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/tcp/8000/ws/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC",
                "29200108A07AC542013AC986FFFE317095061F40DD03A503221220D52EBB89D85B02A284948203A62FF28389C57C9F42BEEC4EC20DB76A68911C0B",
                listOf(
                    Ip6Component.fromIPv6Address(addr6).expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "8000").expectNoErrors(),
                    GenericComponent.fromString(Protocol.WS, "").expectNoErrors(),
                    MultihashComponent.fromString("QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC").expectNoErrors()
                )
            ),
            Tuple3(
                "/p2p-webrtc-star/ip4/127.0.0.1/tcp/9090/ws/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC",
                "9302047F000001062382DD03A503221220D52EBB89D85B02A284948203A62FF28389C57C9F42BEEC4EC20DB76A68911C0B",
                listOf(
                    GenericComponent.fromString(Protocol.P2P_WEBRTC_STAR, "").expectNoErrors(),
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "9090").expectNoErrors(),
                    GenericComponent.fromString(Protocol.WS, "").expectNoErrors(),
                    MultihashComponent.fromString("QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip6/2001:8a0:7ac5:4201:3ac9:86ff:fe31:7095/tcp/8000/wss/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC",
                "29200108A07AC542013AC986FFFE317095061F40DE03A503221220D52EBB89D85B02A284948203A62FF28389C57C9F42BEEC4EC20DB76A68911C0B",
                listOf(
                    Ip6Component.fromIPv6Address(addr6).expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "8000").expectNoErrors(),
                    GenericComponent.fromString(Protocol.WSS, "").expectNoErrors(),
                    MultihashComponent.fromString("QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/tcp/9090/p2p-circuit/p2p/QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC",
                "047F000001062382A202A503221220D52EBB89D85B02A284948203A62FF28389C57C9F42BEEC4EC20DB76A68911C0B",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "9090").expectNoErrors(),
                    GenericComponent.fromString(Protocol.P2P_CIRCUIT, "").expectNoErrors(),
                    MultihashComponent.fromString("QmcgpsyWgH8Y8ajJz1Cu72KnS5uo2Aa2LpzU7kinSupNKC").expectNoErrors()
                )
            ),
            Tuple3(
                "/onion/aaimaq4ygg2iegci:80",
                "BC030010C0439831B48218480050",
                listOf(
                    OnionComponent.fromBytes(Protocol.ONION, byteArrayOf(0.toByte(), 16.toByte(), 192.toByte(), 67.toByte(), 152.toByte(), 49.toByte(), 180.toByte(), 130.toByte(), 24.toByte(), 72.toByte(), 0.toByte(), 80.toByte())).expectNoErrors()
                )
            ),
            Tuple3(
                "/onion3/vww6ybal4bd7szmgncyruucpgfkqahzddi37ktceo3ah7ngmcopnpyyd:1234",
                "BD03ADADEC040BE047F9658668B11A504F3155001F231A37F54C4476C07FB4CC139ED7E30304D2",
                listOf(
                    OnionComponent.fromBytes(
                        Protocol.ONION3,
                        byteArrayOf(
                            173.toByte(), 173.toByte(), 236.toByte(), 4.toByte(), 11.toByte(), 224.toByte(), 71.toByte(), 249.toByte(), 101.toByte(), 134.toByte(), 104.toByte(), 177.toByte(), 26.toByte(), 80.toByte(), 79.toByte(), 49.toByte(), 85.toByte(), 0.toByte(),
                            31.toByte(), 35.toByte(), 26.toByte(), 55.toByte(), 245.toByte(), 76.toByte(), 68.toByte(), 118.toByte(), 192.toByte(), 127.toByte(), 180.toByte(), 204.toByte(), 19.toByte(), 158.toByte(), 215.toByte(), 227.toByte(), 3.toByte(),
                            4.toByte(), 210.toByte()
                        )
                    ).expectNoErrors()
                )
            ),
            Tuple3(
                "/dnsaddr/sjc-1.bootstrap.libp2p.io",
                "3819736A632D312E626F6F7473747261702E6C69627032702E696F",
                listOf(
                    DnsComponent.fromString(Protocol.DNSADDR, "sjc-1.bootstrap.libp2p.io").expectNoErrors()
                )
            ),
            Tuple3(
                "/dnsaddr/sjc-1.bootstrap.libp2p.io/tcp/1234/p2p/QmNnooDu7bfjPFoTZYxMNLWUQJyrVwtbZg5gBMjTezGAJN",
                "3819736A632D312E626F6F7473747261702E6C69627032702E696F0604D2A50322122006B3608AA000274049EB28AD8E793A26FF6FAB281A7D3BD77CD18EB745DFAABB",
                listOf(
                    DnsComponent.fromString(Protocol.DNSADDR, "sjc-1.bootstrap.libp2p.io").expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "1234").expectNoErrors(),
                    MultihashComponent.fromString("QmNnooDu7bfjPFoTZYxMNLWUQJyrVwtbZg5gBMjTezGAJN").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/tcp/127/ws",
                "047F00000106007FDD03",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "127").expectNoErrors(),
                    GenericComponent.fromString(Protocol.WS, "").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/tcp/127/tls",
                "047F00000106007FC003",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "127").expectNoErrors(),
                    GenericComponent.fromBytes(Protocol.TLS, byteArrayOf()).expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/tcp/127/tls/ws",
                "047F00000106007FC003DD03",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "127").expectNoErrors(),
                    GenericComponent.fromBytes(Protocol.TLS, byteArrayOf()).expectNoErrors(),
                    GenericComponent.fromString(Protocol.WS, "").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/tcp/127/noise",
                "047F00000106007FC603",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.TCP, "127").expectNoErrors(),
                    GenericComponent.fromString(Protocol.NOISE, "").expectNoErrors()
                )
            ),
            Tuple3(
                "/ip4/127.0.0.1/udp/1234/webrtc",
                "047F000001910204D29802",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.UDP, "1234").expectNoErrors(),
                    GenericComponent.fromString(Protocol.WEBRTC, "").expectNoErrors()
                )
            ),

            Tuple3(
                "/ip4/127.0.0.1/udp/1234/webrtc/certhash/uEiDDq4_xNyDorZBH3TlGazyJdOWSwvo4PUo5YHFMrvDE8g",
                "047F000001910204D29802D203221220C3AB8FF13720E8AD9047DD39466B3C8974E592C2FA383D4A3960714CAEF0C4F2",
                listOf(
                    Ip4Component.fromIPv4Address(local).expectNoErrors(),
                    PortComponent.fromString(Protocol.UDP, "1234").expectNoErrors(),
                    GenericComponent.fromBytes(Protocol.WEBRTC, byteArrayOf()).expectNoErrors(),
                    CertHashComponent.fromBytes(decoded).expectNoErrors()
                )
            )
        ).map { (source: String, target: String, components: List<Component>) ->
            DynamicTest.dynamicTest("Test: $source") {
                val bytes = Hex.decode(target).expectNoErrors()
                val multiaddress1 = Multiaddress.fromString(source).expectNoErrors()
                assertEquals(source, multiaddress1.toString())
                assertEquals(Hex.encode(bytes), Hex.encode(multiaddress1.bytes))
                val multiaddress2 = Multiaddress.fromBytes(bytes).expectNoErrors()
                assertEquals(multiaddress2, multiaddress1)
                val out = ByteArrayOutputStream()
                components.forEach { it.writeTo(out) }
                assertEquals(Hex.encode(out.toByteArray()), Hex.encode(multiaddress1.bytes))
            }
        }.stream()
    }
}
