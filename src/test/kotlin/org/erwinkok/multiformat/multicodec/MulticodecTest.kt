package org.erwinkok.multiformat.multicodec

import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MulticodecTest {
    @Test
    fun `test some multicodec values`() {
        assertEquals("identity", Multicodec.codeToName(0x00).expectNoErrors())
        assertEquals(Multicodec.IDENTITY, Multicodec.nameToType("identity").expectNoErrors())
        assertEquals(Multicodec.IDENTITY, Multicodec.codeToType(0x00).expectNoErrors())
        assertEquals(0x00, Multicodec.IDENTITY.code)
        assertEquals("identity", Multicodec.IDENTITY.typeName)

        assertEquals("cidv2", Multicodec.codeToName(0x02).expectNoErrors())
        assertEquals(Multicodec.CIDV2, Multicodec.nameToType("cidv2").expectNoErrors())
        assertEquals(Multicodec.CIDV2, Multicodec.codeToType(0x02).expectNoErrors())
        assertEquals(0x02, Multicodec.CIDV2.code)
        assertEquals("cidv2", Multicodec.CIDV2.typeName)

        assertEquals("path", Multicodec.codeToName(0x2f).expectNoErrors())
        assertEquals(Multicodec.PATH, Multicodec.nameToType("path").expectNoErrors())
        assertEquals(Multicodec.PATH, Multicodec.codeToType(0x2f).expectNoErrors())
        assertEquals(0x2f, Multicodec.PATH.code)
        assertEquals("path", Multicodec.PATH.typeName)

        assertEquals("md5", Multicodec.codeToName(0xd5).expectNoErrors())
        assertEquals(Multicodec.MD5, Multicodec.nameToType("md5").expectNoErrors())
        assertEquals(Multicodec.MD5, Multicodec.codeToType(0xd5).expectNoErrors())
        assertEquals(0xd5, Multicodec.MD5.code)
        assertEquals("md5", Multicodec.MD5.typeName)

        assertEquals("eth-block", Multicodec.codeToName(0x90).expectNoErrors())
        assertEquals(Multicodec.ETH_BLOCK, Multicodec.nameToType("eth-block").expectNoErrors())
        assertEquals(Multicodec.ETH_BLOCK, Multicodec.codeToType(0x90).expectNoErrors())
        assertEquals(0x90, Multicodec.ETH_BLOCK.code)
        assertEquals("eth-block", Multicodec.ETH_BLOCK.typeName)

        assertEquals("dag-pb", Multicodec.codeToName(0x70).expectNoErrors())
        assertEquals(Multicodec.DAG_PB, Multicodec.nameToType("dag-pb").expectNoErrors())
        assertEquals(Multicodec.DAG_PB, Multicodec.codeToType(0x70).expectNoErrors())
        assertEquals(0x70, Multicodec.DAG_PB.code)
        assertEquals("dag-pb", Multicodec.DAG_PB.typeName)

        assertEquals("blake2b-8", Multicodec.codeToName(0xb201).expectNoErrors())
        assertEquals(Multicodec.BLAKE2B_8, Multicodec.nameToType("blake2b-8").expectNoErrors())
        assertEquals(Multicodec.BLAKE2B_8, Multicodec.codeToType(0xb201).expectNoErrors())
        assertEquals(0xb201, Multicodec.BLAKE2B_8.code)
        assertEquals("blake2b-8", Multicodec.BLAKE2B_8.typeName)

        assertEquals("udp", Multicodec.codeToName(0x0111).expectNoErrors())
        assertEquals(Multicodec.UDP, Multicodec.nameToType("udp").expectNoErrors())
        assertEquals(Multicodec.UDP, Multicodec.codeToType(0x0111).expectNoErrors())
        assertEquals(0x0111, Multicodec.UDP.code)
        assertEquals("udp", Multicodec.UDP.typeName)

        assertEquals("protobuf", Multicodec.codeToName(0x50).expectNoErrors())
        assertEquals(Multicodec.PROTOBUF, Multicodec.nameToType("protobuf").expectNoErrors())
        assertEquals(Multicodec.PROTOBUF, Multicodec.codeToType(0x50).expectNoErrors())
        assertEquals(0x50, Multicodec.PROTOBUF.code)
        assertEquals("protobuf", Multicodec.PROTOBUF.typeName)

        assertEquals("keccak-256", Multicodec.codeToName(0x1b).expectNoErrors())
        assertEquals(Multicodec.KECCAK_256, Multicodec.nameToType("keccak-256").expectNoErrors())
        assertEquals(Multicodec.KECCAK_256, Multicodec.codeToType(0x1b).expectNoErrors())
        assertEquals(0x1b, Multicodec.KECCAK_256.code)
        assertEquals("keccak-256", Multicodec.KECCAK_256.typeName)
    }

    @Test
    fun `generates error when getting type from unknown multicodec name`() {
        assertErrorResult("Unknown Multicodec name: this-codec-doesnt-exist") { Multicodec.nameToType("this-codec-doesnt-exist") }
    }

    @Test
    fun `generates error when getting type from unknown multicodec type`() {
        assertErrorResult("Unknown Multicodec code: 65518") { Multicodec.codeToType(0xffee) }
    }
}
