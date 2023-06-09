// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multihash

import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.util.writeUnsignedVarInt
import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.erwinkok.util.Hex
import org.erwinkok.util.Tuple4
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.ByteArrayOutputStream
import java.util.stream.Stream

internal class MultihashTest {
    private val MaxVarintLen64 = 10

    @Test
    fun toB58String() {
        val src = "QmPfjpVaf593UQJ9a5ECvdh2x17XuJYG5Yanv5UFnH3jPE"
        val expected = Hex.decode("122013bf801597d74a660453412635edd8c34271e5998f801fac5d700c6ce8d8e461").expectNoErrors()
        val multihash = Multihash.fromBase58(src).expectNoErrors()
        assertArrayEquals(multihash.bytes(), expected)
        assertEquals(src, multihash.base58())
    }

    @Test
    fun fromB58String() {
        val src = "QmPfjpVaf593UQJ9a5ECvdh2x17XuJYG5Yanv5UFnH3jPE"
        val expected = Hex.decode("122013bf801597d74a660453412635edd8c34271e5998f801fac5d700c6ce8d8e461").expectNoErrors()
        assertArrayEquals(Multihash.fromBase58(src).expectNoErrors().bytes(), expected)
    }

    @Test
    fun encodeName() {
        val digest = Hex.decode("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33").expectNoErrors()
        val mh = Multihash.encodeName(digest, "sha1")
            .expectNoErrors()
        assertEquals("11140beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", Hex.encode(mh.bytes()))
        val mh2 = Multihash.fromBytes(mh.bytes())
            .expectNoErrors()
        assertEquals("sha1", mh2.name)
        assertEquals(0x11, mh2.code)
        assertEquals(20, mh2.length)
        assertEquals("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", Hex.encode(mh2.digest))
    }

    @TestFactory
    fun encode(): Stream<DynamicTest> {
        return arguments.map { (hex, code, name, _) ->
            DynamicTest.dynamicTest(name) {
                val digest = Hex.decode(hex).expectNoErrors()
                val stream = ByteArrayOutputStream(2 * MaxVarintLen64)
                stream.writeUnsignedVarInt(code)
                stream.writeUnsignedVarInt(digest.size)
                val multihashBytes = stream.toByteArray() + digest

                val typec = Multicodec.codeToType(code)
                    .expectNoErrors()
                val encc = Multihash.fromTypeAndDigest(typec, digest)
                    .expectNoErrors()
                assertArrayEquals(multihashBytes, encc.bytes())

                val typen = Multicodec.nameToType(name)
                    .expectNoErrors()
                val encn = Multihash.fromTypeAndDigest(typen, digest)
                    .expectNoErrors()
                assertArrayEquals(multihashBytes, encn.bytes())
            }
        }.stream()
    }

    @TestFactory
    fun decode(): Stream<DynamicTest> {
        return arguments.map { (hex, code, name, error) ->
            DynamicTest.dynamicTest(name) {
                val digest = Hex.decode(hex).expectNoErrors()
                val stream = ByteArrayOutputStream(2 * MaxVarintLen64)
                stream.writeUnsignedVarInt(code)
                stream.writeUnsignedVarInt(digest.size)
                val multihashBytes = stream.toByteArray() + digest
                val multihash = Multihash.fromBytes(multihashBytes)
                    .expectNoErrors()
                assertEquals(code, multihash.code)
                assertEquals(name, multihash.name)
                assertEquals(digest.size, multihash.length)
                assertArrayEquals(digest, multihash.digest)

                assertErrorResult(error) { Multihash.fromBytes(digest) }
            }
        }.stream()
    }

    @TestFactory
    fun hex(): Stream<DynamicTest> {
        return arguments.map { (hex, code, name, _) ->
            DynamicTest.dynamicTest(name) {
                val digest = Hex.decode(hex).expectNoErrors()
                val stream = ByteArrayOutputStream(2 * MaxVarintLen64)
                stream.writeUnsignedVarInt(code)
                stream.writeUnsignedVarInt(digest.size)
                val multihashBytes = stream.toByteArray() + digest
                val hexBytes = Hex.encode(multihashBytes)
                val multihash = Multihash.fromHexString(hexBytes)
                    .expectNoErrors()
                assertArrayEquals(multihashBytes, multihash.bytes())
                assertEquals(hexBytes, multihash.hex())
            }
        }.stream()
    }

    @Test
    fun decodeErrorInvalid() {
        assertErrorResult("input isn't valid multihash") { Multihash.fromBase58("/ipfs/QmQTw94j68Dgakgtfd45bG3TZG6CAfc427UVRH4mugg4q4") }
    }

    @Test
    fun badVarint() {
        assertErrorResult("varints larger than uint63 not supported") {
            Multihash.fromBytes(byteArrayOf(129.toByte(), 128.toByte(), 128.toByte(), 128.toByte(), 128.toByte(), 128.toByte(), 128.toByte(), 128.toByte(), 128.toByte(), 128.toByte(), 129.toByte(), 1.toByte()))
        }
        assertErrorResult("UnexpectedEndOfStream") { Multihash.fromBytes(byteArrayOf(129.toByte(), 128.toByte(), 128.toByte())) }
        assertErrorResult("varint not minimally encoded") { Multihash.fromBytes(byteArrayOf(129.toByte(), 0.toByte())) }
        assertErrorResult("varint not minimally encoded") { Multihash.fromBytes(byteArrayOf(128.toByte(), 0.toByte())) }
    }

    companion object {
        var arguments = listOf(
            Tuple4("2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae", 0x00, "identity", "Unknown Multicodec code: 44"),
            Tuple4("", 0x00, "identity", "multihash too short. must be >= 2 bytes"),
            Tuple4("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", 0x11, "sha1", "Unknown Multicodec code: 11"),
            Tuple4("0beec7b8", 0x11, "sha1", "Unknown Multicodec code: 11"),
            Tuple4("2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae", 0x12, "sha2-256", "Unknown Multicodec code: 44"),
            Tuple4("2c26b46b", 0x12, "sha2-256", "Unknown Multicodec code: 44"),
            Tuple4("2c26b46b68ffc68ff99b453c1d30413413", 0xb240, "blake2b-512", "Unknown Multicodec code: 44"),
            Tuple4("243ddb9e", 0x22, "murmur3-x64-64", "Unknown Multicodec code: 36"),
            Tuple4("f00ba4", 0x1b, "keccak-256", "Unknown Multicodec code: 1520"),
            Tuple4("f84e95cb5fbd2038863ab27d3cdeac295ad2d4ab96ad1f4b070c0bf36078ef08", 0x18, "shake-128", "Unknown Multicodec code: 10104"),
            Tuple4("1af97f7818a28edfdfce5ec66dbdc7e871813816d7d585fe1f12475ded5b6502b7723b74e2ee36f2651a10a8eaca72aa9148c3c761aaceac8f6d6cc64381ed39", 0x19, "shake-256", "length greater than remaining number of bytes in buffer"),
            Tuple4("4bca2b137edc580fe50a88983ef860ebaca36c857b1f492839d6d7392452a63c82cbebc68e3b70a2a1480b4bb5d437a7cba6ecf9d89f9ff3ccd14cd6146ea7e7", 0x14, "sha3-512", "Unknown Multicodec code: 75"),
            Tuple4("d41d8cd98f00b204e9800998ecf8427e", 0xd5, "md5", "Unknown Multicodec code: 3796"),
            Tuple4("14fcb37dc45fa9a3c492557121bd4d461c0db40e5dcfcaa98498bd238486c307", 0x1012, "sha2-256-trunc254-padded", "length greater than remaining number of bytes in buffer"),
            Tuple4("14fcb37dc45fa9a3c492557121bd4d461c0db40e5dcfcaa98498bd238486c307", 0xb401, "poseidon-bls12_381-a2-fc1", "length greater than remaining number of bytes in buffer"),
            Tuple4("04e0bb39f30b1a3feb89f536c93be15055482df748674b00d26e5a75777702e9", 0x1e, "blake3", "length greater than remaining number of bytes in buffer"),
        )
    }
}
