// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.cid

import org.erwinkok.multiformat.multibase.Multibase
import org.erwinkok.multiformat.multicodec.Multicodec
import org.erwinkok.multiformat.multihash.Multihash
import org.erwinkok.multiformat.multihash.MultihashRegistry
import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.erwinkok.util.Hex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream

internal class CidTest {
    @TestFactory
    fun `prefix sum`(): Stream<DynamicTest> {
        return listOf(
            Multicodec.IDENTITY,
            Multicodec.SHA3_512,
            Multicodec.SHA2_256
        ).map { hash ->
            DynamicTest.dynamicTest("Test: $hash") {
                val h1 = Multihash.sum(hash, "TEST".toByteArray(), -1).expectNoErrors()
                val c1 = CidV1(h1, Multicodec.RAW)

                val h2 = Multihash.sum(hash, "foobar".toByteArray(), -1).expectNoErrors()
                val c2 = CidV1(h2, Multicodec.RAW)

                val c3 = c1.prefix().sum("foobar".toByteArray()).expectNoErrors()
                assertEquals(c2, c3)
            }
        }.stream()
    }

    @Test
    fun `basic marshaling`() {
        val h = Multihash.sum(Multicodec.SHA3_512, "TEST".toByteArray(), 4).expectNoErrors()
        val cid = CidV1(h, Multicodec.SHA3_512)
        val data = cid.bytes()
        val out = Cid.fromBytes(data).expectNoErrors()
        assertEquals(cid, out)
        val s = cid.toString()
        val out2 = Cid.fromString(s).expectNoErrors()
        assertEquals(cid, out2)
    }

    @TestFactory
    fun `bases marshalling`(): Stream<DynamicTest> {
        val h = Multihash.sum(Multicodec.SHA3_512, "TEST".toByteArray(), 4).expectNoErrors()
        val cid = CidV1(h, Multicodec.SHA3_512)
        val data = cid.bytes()
        val out = Cid.fromBytes(data).expectNoErrors()
        assertEquals(cid, out)
        val cid2 = Cid.fromString("QmdfTbBqBPQ7VNxZEYEj14VmRuZBkqFbiwReogJgS1zR1n").expectNoErrors()
        assertEquals(Multibase.BASE58_BTC, cid2.multibase)
        return listOf(
            Multibase.BASE16,
            Multibase.BASE32,
            Multibase.BASE32_HEX,
            Multibase.BASE32_PAD,
            Multibase.BASE32_HEX_PAD,
            Multibase.BASE58_BTC,
            Multibase.BASE58_FLICKR,
            Multibase.BASE64_PAD,
            Multibase.BASE64_URL_PAD,
            Multibase.BASE64_URL,
            Multibase.BASE64
        ).map { base ->
            DynamicTest.dynamicTest("Test: $base") {
                val s = cid.toString(base).expectNoErrors()
                assertEquals(base.code, s[0].toString())
                val out2 = Cid.fromString(s).expectNoErrors()
                assertEquals(cid.multihash, out2.multihash)
                assertEquals(cid.multicodec, out2.multicodec)
            }
        }.stream()
    }

    @Test
    fun handlesB58StrMultihash() {
        val old = "QmdfTbBqBPQ7VNxZEYEj14VmRuZBkqFbiwReogJgS1zR1n"
        val cid = Cid.fromString(old).expectNoErrors()
        assertEquals("dag-pb", cid.multicodec.typeName)
        assertEquals(112, cid.multicodec.code)
        assertEquals(0, cid.version)
        assertEquals(old, cid.toString())
        assertEquals(34, cid.bytes().size)
        assertEquals(Multihash.fromBase58(old).expectNoErrors(), cid.multihash)
        assertEquals(Multibase.BASE58_BTC, cid.multibase)
    }

    @Test
    fun handlesUint8ArrayMultihash() {
        val mh = createHashSha256("hello world")
        val mhStr = "QmaozNR7DZHQK1ZcU9p7QdrshMvXqWK6gpu5rmrkPdT3L4"
        val cid = Cid.fromBytes(mh.bytes()).expectNoErrors()
        assertEquals("dag-pb", cid.multicodec.typeName)
        assertEquals(112, cid.multicodec.code)
        assertEquals(0, cid.version)
        assertEquals(mh, cid.multihash)
        assertEquals(Multibase.BASE58_BTC, cid.multibase)
        assertEquals(mhStr, cid.toString())
    }

    @Test
    fun throwsOnInvalidBS58StrMultihash() {
        assertErrorResult("input isn't valid multihash") { Cid.fromString("QmdfTbBqBPQ7VNxZEYEj14VmRuZBkqFbiwReogJgS1zIII") }
    }

    @Test
    fun throwsOnTryingToCreateACIDv0WithACodecOtherThanDagpb() {
        val hash = createHashSha256("abc")
        assertErrorResult("codec must be 'dag-pb' for CIDv0") {
            Cid.builder()
                .withVersion(0)
                .withMulticodec(Multicodec.DAG_CBOR)
                .withMultihash(hash)
                .build()
        }
    }

    @Test
    fun throwsOnTryingToCreateACIDv0WithABaseOtherThanBase58btc() {
        val hash = createHashSha256("abc")
        assertErrorResult("multibase must be 'base58btc' for CIDv0") {
            Cid.builder()
                .withVersion(0)
                .withMulticodec(Multicodec.DAG_PB)
                .withMultihash(hash)
                .withMultibase(Multibase.BASE32)
                .build()
        }
    }

    @Test
    fun v0Prefix() {
        val hash = createHashSha256("abc")
        val cid = Cid.builder()
            .withVersion(0)
            .withMulticodec(Multicodec.DAG_PB)
            .withMultihash(hash)
            .build()
            .expectNoErrors()
        assertEquals("00701220", Hex.encode(cid.prefix().bytes()))
    }

    @Test
    fun v0Bytes() {
        val hash = createHashSha256("abc")
        val cid = Cid.builder()
            .withVersion(0)
            .withMulticodec(Multicodec.DAG_PB)
            .withMultihash(hash)
            .build()
            .expectNoErrors()
        assertEquals("1220ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Hex.encode(cid.bytes()))
    }

    @Test
    fun v1HandlesCIDStringMultibaseEncoded() {
        val cidStr = "zdj7Wd8AMwqnhJGQCbFxBVodGSBG84TM7Hs1rcJuQMwTyfEDS"
        val cid = Cid.fromString(cidStr).expectNoErrors()
        assertEquals("dag-pb", cid.multicodec.typeName)
        assertEquals(112, cid.multicodec.code)
        assertEquals(1, cid.version)
        assertEquals(Multibase.BASE58_BTC, cid.multibase)
        assertEquals(cidStr, cid.toString())
    }

    @Test
    fun handlesCIDNoMultibase() {
        val cidStr = "bafybeidskjjd4zmr7oh6ku6wp72vvbxyibcli2r6if3ocdcy7jjjusvl2u"
        val cidBuf = Hex.decode("017012207252523e6591fb8fe553d67ff55a86f84044b46a3e4176e10c58fa529a4aabd5").expectNoErrors()
        val cid = Cid.fromBytes(cidBuf).expectNoErrors()
        assertEquals("dag-pb", cid.multicodec.typeName)
        assertEquals(112, cid.multicodec.code)
        assertEquals(1, cid.version)
        assertEquals(Multibase.BASE32, cid.multibase)
        assertEquals(cidStr, cid.toString())
    }

    @Test
    fun handlesED25519PeerIDAsCIDInBase36() {
        val peerIdStr = "k51qzi5uqu5dj16qyiq0tajolkojyl9qdkr254920wxv7ghtuwcz593tp69z9m"
        val cid = Cid.fromString(peerIdStr).expectNoErrors()
        assertEquals("libp2p-key", cid.multicodec.typeName)
        assertEquals(114, cid.multicodec.code)
        assertEquals(1, cid.version)
        assertEquals(Multibase.BASE36, cid.multibase)
        assertEquals(peerIdStr, cid.toString())
    }

    @Test
    fun handlesMultibyteVarintEncodedCodecCodes() {
        val ethBlockHash = Hex.decode("8a8e84c797605fbe75d5b5af107d4220a2db0ad35fd66d9be3d38d87c472b26d").expectNoErrors()
        val mh = Multihash.fromTypeAndDigest(Multicodec.KECCAK_256, ethBlockHash).expectNoErrors()
        val cid1 = Cid.builder()
            .withVersion(1)
            .withMulticodec(Multicodec.ETH_BLOCK)
            .withMultihash(mh)
            .build()
            .expectNoErrors()
        val cid2 = Cid.fromString(cid1.toString()).expectNoErrors()
        assertEquals("eth-block", cid1.multicodec.typeName)
        assertEquals(144, cid1.multicodec.code)
        assertEquals(1, cid1.version)
        assertEquals(mh, cid1.multihash)
        assertEquals(Multibase.BASE32, cid1.multibase)
        assertEquals("eth-block", cid2.multicodec.typeName)
        assertEquals(144, cid2.multicodec.code)
        assertEquals(1, cid2.version)
        assertEquals(mh, cid2.multihash)
        assertEquals(Multibase.BASE32, cid2.multibase)
    }

    @Test
    fun v1Prefix() {
        val hash = createHashSha256("abc")
        val cid = Cid.builder()
            .withVersion(1)
            .withMulticodec(Multicodec.DAG_CBOR)
            .withMultihash(hash)
            .build()
            .expectNoErrors()
        assertEquals("01711220", Hex.encode(cid.prefix().bytes()))
    }

    @Test
    fun v1bytes() {
        val hash = createHashSha256("abc")
        val cid = Cid.builder()
            .withVersion(1)
            .withMulticodec(Multicodec.DAG_CBOR)
            .withMultihash(hash)
            .build()
            .expectNoErrors()
        assertEquals("01711220ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Hex.encode(cid.bytes()))
    }

    @Test
    fun prefixIdentityMultihash() {
        val mh = Multihash.fromTypeAndDigest(Multicodec.IDENTITY, "abc".toByteArray()).expectNoErrors()
        val cid0 = Cid.builder()
            .withVersion(0)
            .withMulticodec(Multicodec.DAG_PB)
            .withMultihash(mh)
            .build()
            .expectNoErrors()
        assertEquals("dag-pb", cid0.multicodec.typeName)
        assertEquals(112, cid0.multicodec.code)
        assertEquals(0, cid0.version)
        assertEquals(mh, cid0.multihash)
        assertEquals("161g3c", cid0.toString())
    }

    @Test
    fun returnsACIDString() {
        val hash = createHashSha256("abc")
        val cid = Cid.fromBytes(hash.bytes()).expectNoErrors()
        assertEquals("QmatYkNGZnELf8cAGdyJpUca2PyY4szai3RHyyWofNY1pY", cid.toString())
    }

    @Test
    fun returnsAStringInTheBaseProvided() {
        val b58v1Str = "zdj7Wd8AMwqnhJGQCbFxBVodGSBG84TM7Hs1rcJuQMwTyfEDS"
        val b64urlv1Str = "uAXASIHJSUj5lkfuP5VPWf_VahvhARLRqPkF24QxY-lKaSqvV"
        val cid = Cid.fromString(b58v1Str).expectNoErrors()
        assertEquals(b64urlv1Str, Multibase.BASE64_URL.encode(cid.bytes()))
    }

    @Test
    fun equalsV0ToV0() {
        val h1 = "QmdfTbBqBPQ7VNxZEYEj14VmRuZBkqFbiwReogJgS1zR1n"
        val h2 = "QmdfTbBqBPQ7VNxZEYEj14VmRuZBkqFbiwReogJgS1zR1o"
        assertEquals(Cid.fromString(h1), Cid.fromString(h1))
        assertNotEquals(Cid.fromString(h1), Cid.fromString(h2))
    }

    @Test
    fun equalsV0ToV1AndViceVersa() {
        val cidV1Str = "zdj7Wd8AMwqnhJGQCbFxBVodGSBG84TM7Hs1rcJuQMwTyfEDS"
        val cidV1 = Cid.fromString(cidV1Str).expectNoErrors()
        val cidV0 = cidV1.toV0().expectNoErrors()
        assertNotEquals(cidV0, cidV1)
        assertNotEquals(cidV1, cidV0)
        assertEquals(cidV1.multihash, cidV0.multihash)
    }

    @Test
    fun equalsASimilarCIDWithAUint8ArrayMultihash() {
        val str = "QmdfTbBqBPQ7VNxZEYEj14VmRuZBkqFbiwReogJgS1zR1n"
        val cid = Cid.fromString(str).expectNoErrors()
        val cidA = Cid.builder()
            .withVersion(cid.version)
            .withMulticodec(cid.multicodec)
            .withMultihash(cid.multihash)
            .build()
            .expectNoErrors()
        val cidB = Cid.fromString(str).expectNoErrors()
        assertEquals(cidA.copy(), cidB.copy())
    }

    @Test
    fun emptyString() {
        assertErrorResult("cid too short") { Cid.fromString("") }
    }

    @Test
    fun v0Handling() {
        val old = "QmdfTbBqBPQ7VNxZEYEj14VmRuZBkqFbiwReogJgS1zR1n"
        val cid = Cid.fromString(old).expectNoErrors()
        assertEquals(0, cid.version)
        assertEquals(old, cid.multihash.base58())
        assertEquals(old, cid.toString())
        assertEquals(34, cid.bytes().size)
    }

    @Test
    fun `prefix v1`() {
        val data = "this is some test content".toByteArray()
        val prefix = Prefix(1, Multicodec.DAG_CBOR, Multicodec.SHA2_256, MultihashRegistry.defaultHashSize(Multicodec.SHA2_256).expectNoErrors())
        val cid1 = prefix.sum(data).expectNoErrors()
        assertEquals(prefix, cid1.prefix())
        val hash = Multihash.sum(Multicodec.SHA2_256, data).expectNoErrors()
        val cid2 = Cid.builder()
            .withVersion(1)
            .withMulticodec(Multicodec.DAG_CBOR)
            .withMultihash(hash)
            .build()
            .expectNoErrors()
        assertEquals(cid1, cid2)
    }

    @Test
    fun `prefix v0`() {
        val data = "this is some test content".toByteArray()
        val prefix = Prefix(0, Multicodec.DAG_PB, Multicodec.SHA2_256, MultihashRegistry.defaultHashSize(Multicodec.SHA2_256).expectNoErrors())
        val cid1 = prefix.sum(data).expectNoErrors()
        assertEquals(prefix, cid1.prefix())
        val hash = Multihash.sum(Multicodec.SHA2_256, data).expectNoErrors()
        val cid2 = Cid.builder()
            .withVersion(0)
            .withMulticodec(Multicodec.DAG_PB)
            .withMultihash(hash)
            .build()
            .expectNoErrors()
        assertEquals(cid1, cid2)
    }

    @Test
    fun `bad prefix`() {
        val prefix = Prefix(3, Multicodec.DAG_PB, Multicodec.SHA2_256, 3)
        assertErrorResult("invalid cid version") { prefix.sum(byteArrayOf(0x00, 0x01, 0x03)) }
    }

    @Test
    fun `round trip`() {
        val data = "this is some test content".toByteArray()
        val hash = Multihash.sum(Multicodec.SHA2_256, data).expectNoErrors()
        val cid1 = Cid.builder()
            .withVersion(1)
            .withMulticodec(Multicodec.DAG_CBOR)
            .withMultihash(hash)
            .build()
            .expectNoErrors()
        val prefix1 = cid1.prefix()
        val cid2 = prefix1.sum(data).expectNoErrors()
        assertEquals(cid1, cid2)
        val pb = prefix1.bytes()
        val prefix2 = Prefix.fromBytes(pb).expectNoErrors()
        assertEquals(prefix1, prefix2)
    }

    @Test
    fun `bad prefix from bytes`() {
        assertErrorResult("UnexpectedEndOfStream") { Prefix.fromBytes(byteArrayOf(0x80.toByte())) }
        assertErrorResult("UnexpectedEndOfStream") { Prefix.fromBytes(byteArrayOf(0x01, 0x80.toByte())) }
        assertErrorResult("UnexpectedEndOfStream") { Prefix.fromBytes(byteArrayOf(0x01, 0x01, 0x80.toByte())) }
        assertErrorResult("UnexpectedEndOfStream") { Prefix.fromBytes(byteArrayOf(0x01, 0x01, 0x01, 0x80.toByte())) }
    }

    private fun createHashSha256(text: String): Multihash {
        return Multihash.sum(Multicodec.SHA2_256, text.toByteArray()).expectNoErrors()
    }
}
