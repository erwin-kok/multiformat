// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multihash

import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.erwinkok.util.Tuple5
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream

internal class MultihashSumTest {
    @TestFactory
    fun sum(): Stream<DynamicTest> {
        return arguments.map { (code, length, input, hex, error) ->
            DynamicTest.dynamicTest(code.typeName) {
                val expectedMultihash = Multihash.fromHexString(hex).expectNoErrors()
                if (error.isNullOrBlank()) {
                    val actualMultihash = Multihash.sum(code, input.toByteArray(), length).expectNoErrors()
                    assertArrayEquals(expectedMultihash.bytes(), actualMultihash.bytes())

                    assertEquals(hex, expectedMultihash.hex())

                    val base58 = expectedMultihash.base58()
                    val base58Multihash = Multihash.fromBase58(base58).expectNoErrors()
                    assertArrayEquals(expectedMultihash.bytes(), base58Multihash.bytes())
                    assertEquals(base58Multihash.base58(), base58)
                } else {
                    assertErrorResult(error) { Multihash.sum(code, input.toByteArray(), length) }
                }
            }
        }.stream()
    }

    @Test
    fun smallerLengthHashId() {
        val data = "Identity hash input data.".toByteArray()
        Multihash.sum(Multicodec.IDENTITY, data, data.size).expectNoErrors()
        Multihash.sum(Multicodec.IDENTITY, data, -1).expectNoErrors()
        for (l in data.indices) {
            assertErrorResult("the length of the identity hash must be equal to the length of the data") { Multihash.sum(Multicodec.IDENTITY, data, l) }
        }
    }

    @Test
    fun tooLargeLength() {
        assertErrorResult("requested length was too large for digest of type: sha2-256") { Multihash.sum(Multicodec.SHA2_256, "test".toByteArray(), 33) }
    }

    @TestFactory
    fun spec(): Stream<DynamicTest> {
        val reader = File("./src/main/kotlin/org/erwinkok/multiformat/spec/multihash/tests/values/test_cases.csv").bufferedReader()
        reader.readLine()
        return reader.lineSequence()
            .filter { it.isNotBlank() }
            .map {
                val (function, length, input, expected) = it.split(',', ignoreCase = false, limit = 4)
                DynamicTest.dynamicTest("Test: $function / $length") {
                    var name = function.trim()
                    if (name == "sha3") {
                        name = "sha3-512"
                    }
                    val type = Multicodec.nameToType(name).expectNoErrors()
                    val len = Integer.decode(length.trim())
                    assertTrue(len % 8 == 0)
                    val actual = Multihash.sum(type, input.trim().toByteArray(), len / 8).expectNoErrors()
                    assertEquals(expected.trim(), actual.hex())
                }
            }.asStream()
    }

    companion object {
        var arguments = listOf(
            Tuple5(Multicodec.IDENTITY, 3, "foo", "0003666f6f", null),
            Tuple5(Multicodec.IDENTITY, -1, "foofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoo", "0030666f6f666f6f666f6f666f6f666f6f666f6f666f6f666f6f666f6f666f6f666f6f666f6f666f6f666f6f666f6f666f6f", null),
            Tuple5(Multicodec.SHA1, -1, "foo", "11140beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", null),
            Tuple5(Multicodec.SHA1, 10, "foo", "110a0beec7b5ea3f0fdbc95d", null),
            Tuple5(Multicodec.SHA2_256, -1, "foo", "12202c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae", null),
            Tuple5(Multicodec.SHA2_256, 31, "foo", "121f2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7", null),
            Tuple5(Multicodec.SHA2_256, 32, "foo", "12202c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae", null),
            Tuple5(Multicodec.SHA2_256, 16, "foo", "12102c26b46b68ffc68ff99b453c1d304134", null),
            Tuple5(Multicodec.SHA2_512, -1, "foo", "1340f7fbba6e0636f890e56fbbf3283e524c6fa3204ae298382d624741d0dc6638326e282c41be5e4254d8820772c5518a2c5a8c0c7f7eda19594a7eb539453e1ed7", null),
            Tuple5(Multicodec.SHA2_512, 32, "foo", "1320f7fbba6e0636f890e56fbbf3283e524c6fa3204ae298382d624741d0dc663832", null),
            Tuple5(Multicodec.SHA3_512, 32, "foo", "14204bca2b137edc580fe50a88983ef860ebaca36c857b1f492839d6d7392452a63c", null),
            Tuple5(Multicodec.SHA3_512, 16, "foo", "14104bca2b137edc580fe50a88983ef860eb", null),
            Tuple5(Multicodec.SHA3_512, -1, "foo", "14404bca2b137edc580fe50a88983ef860ebaca36c857b1f492839d6d7392452a63c82cbebc68e3b70a2a1480b4bb5d437a7cba6ecf9d89f9ff3ccd14cd6146ea7e7", null),
            Tuple5(Multicodec.SHA3_224, -1, "beep boop", "171c0da73a89549018df311c0a63250e008f7be357f93ba4e582aaea32b8", null),
            Tuple5(Multicodec.SHA3_224, 16, "beep boop", "17100da73a89549018df311c0a63250e008f", null),
            Tuple5(Multicodec.SHA3_256, -1, "beep boop", "1620828705da60284b39de02e3599d1f39e6c1df001f5dbf63c9ec2d2c91a95a427f", null),
            Tuple5(Multicodec.SHA3_256, 16, "beep boop", "1610828705da60284b39de02e3599d1f39e6", null),
            Tuple5(Multicodec.SHA3_384, -1, "beep boop", "153075a9cff1bcfbe8a7025aa225dd558fb002769d4bf3b67d2aaf180459172208bea989804aefccf060b583e629e5f41e8d", null),
            Tuple5(Multicodec.SHA3_384, 16, "beep boop", "151075a9cff1bcfbe8a7025aa225dd558fb0", null),
            Tuple5(Multicodec.DBL_SHA2_256, 32, "foo", "5620c7ade88fc7a21498a6a5e5c385e1f68bed822b72aa63c4a9a48a02c2466ee29e", null),
            Tuple5(Multicodec.BLAKE2B_512, -1, "foo", "c0e40240ca002330e69d3e6b84a46a56a6533fd79d51d97a3bb7cad6c2ff43b354185d6dc1e723fb3db4ae0737e120378424c714bb982d9dc5bbd7a0ab318240ddd18f8d", null),
            Tuple5(Multicodec.BLAKE2B_512, 64, "foo", "c0e40240ca002330e69d3e6b84a46a56a6533fd79d51d97a3bb7cad6c2ff43b354185d6dc1e723fb3db4ae0737e120378424c714bb982d9dc5bbd7a0ab318240ddd18f8d", null),
            Tuple5(Multicodec.BLAKE2B_256, -1, "foo", "a0e40220b8fe9f7f6255a6fa08f668ab632a8d081ad87983c77cd274e48ce450f0b349fd", null),
            Tuple5(Multicodec.BLAKE2B_256, 32, "foo", "a0e40220b8fe9f7f6255a6fa08f668ab632a8d081ad87983c77cd274e48ce450f0b349fd", null),
            Tuple5(Multicodec.BLAKE2B_360, -1, "foo", "ade4022dca82ab956d5885e3f5db10cca94182f01a6ca2c47f9f4228497dcc9f4a0121c725468b852a71ec21fcbeb725df", null),
            Tuple5(Multicodec.BLAKE2B_360, 45, "foo", "ade4022dca82ab956d5885e3f5db10cca94182f01a6ca2c47f9f4228497dcc9f4a0121c725468b852a71ec21fcbeb725df", null),
            Tuple5(Multicodec.BLAKE2B_384, -1, "foo", "b0e40230e629ee880953d32c8877e479e3b4cb0a4c9d5805e2b34c675b5a5863c4ad7d64bb2a9b8257fac9d82d289b3d39eb9cc2", null),
            Tuple5(Multicodec.BLAKE2B_384, 48, "foo", "b0e40230e629ee880953d32c8877e479e3b4cb0a4c9d5805e2b34c675b5a5863c4ad7d64bb2a9b8257fac9d82d289b3d39eb9cc2", null),
            Tuple5(Multicodec.BLAKE2B_160, -1, "foo", "94e40214983ceba2afea8694cc933336b27b907f90c53a88", null),
            Tuple5(Multicodec.BLAKE2B_160, 20, "foo", "94e40214983ceba2afea8694cc933336b27b907f90c53a88", null),
            Tuple5(Multicodec.BLAKE2B_8, -1, "foo", "81e4020152", null),
            Tuple5(Multicodec.BLAKE2B_8, 1, "foo", "81e4020152", null),
            Tuple5(Multicodec.BLAKE2S_256, 32, "foo", "e0e4022008d6cad88075de8f192db097573d0e829411cd91eb6ec65e8fc16c017edfdb74", null),
            Tuple5(Multicodec.KECCAK_256, 32, "foo", "1b2041b1a0649752af1b28b3dc29a1556eee781e4a4c3a1f7f53f90fa834de098c4d", null),
            Tuple5(Multicodec.KECCAK_512, -1, "beep boop", "1d40e161c54798f78eba3404ac5e7e12d27555b7b810e7fd0db3f25ffa0c785c438331b0fbb6156215f69edf403c642e5280f4521da9bd767296ec81f05100852e78", null),
            Tuple5(Multicodec.SHAKE_128, 32, "foo", "1820f84e95cb5fbd2038863ab27d3cdeac295ad2d4ab96ad1f4b070c0bf36078ef08", null),
            Tuple5(Multicodec.SHAKE_256, 64, "foo", "19401af97f7818a28edfdfce5ec66dbdc7e871813816d7d585fe1f12475ded5b6502b7723b74e2ee36f2651a10a8eaca72aa9148c3c761aaceac8f6d6cc64381ed39", null),
            Tuple5(Multicodec.MD5, -1, "foo", "d50110acbd18db4cc2f85cedef654fccc4a4d8", null),
            Tuple5(Multicodec.BLAKE3, 32, "foo", "1e2004e0bb39f30b1a3feb89f536c93be15055482df748674b00d26e5a75777702e9", null),
            Tuple5(Multicodec.BLAKE3, 64, "foo", "1e4004e0bb39f30b1a3feb89f536c93be15055482df748674b00d26e5a75777702e9791074b7511b59d31c71c62f5a745689fa6c9497f68bdf1061fe07f518d410c0", null),
            Tuple5(Multicodec.BLAKE3, 128, "foo", "1e800104e0bb39f30b1a3feb89f536c93be15055482df748674b00d26e5a75777702e9791074b7511b59d31c71c62f5a745689fa6c9497f68bdf1061fe07f518d410c0b0c27f41b3cf083f8a7fdc67a877e21790515762a754a45dcb8a356722698a7af5ed2bb608983d5aa75d4d61691ef132efe8631ce0afc15553a08fffc60ee936", null),
            Tuple5(Multicodec.BLAKE3, -1, "foo", "1e2004e0bb39f30b1a3feb89f536c93be15055482df748674b00d26e5a75777702e9", null),
            Tuple5(Multicodec.BLAKE3, 129, "foo", "1e810104e0bb39f30b1a3feb89f536c93be15055482df748674b00d26e5a75777702e9791074b7511b59d31c71c62f5a745689fa6c9497f68bdf1061fe07f518d410c0b0c27f41b3cf083f8a7fdc67a877e21790515762a754a45dcb8a356722698a7af5ed2bb608983d5aa75d4d61691ef132efe8631ce0afc15553a08fffc60ee9369b", "Unsupported size for Blake3: 129")
        )
    }
}
