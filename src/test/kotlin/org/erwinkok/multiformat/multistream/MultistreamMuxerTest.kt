// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
@file:OptIn(ExperimentalCoroutinesApi::class)

package org.erwinkok.multiformat.multistream

import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.writeFully
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.erwinkok.multiformat.util.UVarInt
import org.erwinkok.result.Err
import org.erwinkok.result.Error
import org.erwinkok.result.Errors
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.coAssertErrorResult
import org.erwinkok.result.expectNoErrors
import org.erwinkok.result.getError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MultistreamMuxerTest {
    private val connection = TestUtf8Connection()
    private val localConnection = connection.local
    private val remoteConnection = connection.remote
    private val muxer = MultistreamMuxer<Utf8Connection>()

    @BeforeEach
    fun setup() {
        muxer.clearHandlers()
    }

    @Test
    fun selectOneOf() = runTest {
        remoteSends(
            "/multistream/1.0.0",
            "/proto1",
        )

        val selectOneResult = MultistreamMuxer.selectOneOf(setOf(ProtocolId.of("/proto1")), localConnection).expectNoErrors()
        assertEquals("/proto1", selectOneResult.id)

        remoteReceived(
            "/multistream/1.0.0",
            "/proto1",
        )
    }

    @Test
    fun selectOneOfMultiple() = runTest {
        remoteSends(
            "/multistream/1.0.0",
            "na",
            "na",
            "/proto3",
        )

        val selectOneResult = MultistreamMuxer.selectOneOf(
            setOf(
                ProtocolId.of("/proto1"),
                ProtocolId.of("/proto2"),
                ProtocolId.of("/proto3"),
                ProtocolId.of("/proto4"),
                ProtocolId.of("/proto5"),
            ),
            localConnection,
        ).expectNoErrors()
        assertEquals("/proto3", selectOneResult.id)

        remoteReceived(
            "/multistream/1.0.0",
            "/proto1",
            "/proto2",
            "/proto3",
        )
    }

    @Test
    fun selectOneOfNA() = runTest {
        remoteSends(
            "/multistream/1.0.0",
            "na",
        )

        val selectOneResult = MultistreamMuxer.selectOneOf(setOf(ProtocolId.of("/proto2")), localConnection)
        assertEquals(Error("Peer does not support any of the given protocols"), selectOneResult.getError())

        remoteReceived(
            "/multistream/1.0.0",
            "/proto2",
        )
    }

    @Test
    fun negotiateMatchNoHandler() = runTest {
        remoteSends(
            "/multistream/1.0.0",
            "/proto3",
        )

        muxer.addHandler(ProtocolId.of("/proto3"))
        val negotiateResult = muxer.negotiate(localConnection).expectNoErrors()
        assertEquals("/proto3", negotiateResult.protocol.id)
        assertEquals(null, negotiateResult.handler)

        remoteReceived(
            "/multistream/1.0.0",
            "/proto3",
        )
    }

    @Test
    fun negotiateMatchWithHandler() {
        runTest {
            remoteSends(
                "/multistream/1.0.0",
                "/proto4",
            )

            var hit = false
            muxer.addHandler(ProtocolId.of("/proto4")) { protocol, connection ->
                assertEquals("AProtocol", protocol.id)
                assertSame(remoteConnection, connection)
                hit = true
                Ok(Unit)
            }
            val negotiateResult = muxer.negotiate(localConnection).expectNoErrors()
            assertEquals("/proto4", negotiateResult.protocol.id)
            assertNotNull(negotiateResult.handler)
            negotiateResult.handler?.invoke(ProtocolId.of("AProtocol"), remoteConnection)
            assertTrue(hit)

            remoteReceived(
                "/multistream/1.0.0",
                "/proto4",
            )
        }
    }

    @Test
    fun list() {
        runTest {
            remoteSends(
                "/multistream/1.0.0",
                "/proto1\n/proto2\n/proto3/sub-proto\n",
            )

            val listResult = muxer.list(localConnection).expectNoErrors()
            assertEquals("/proto1, /proto2, /proto3/sub-proto", listResult.joinToString(", "))

            remoteReceived(
                "/multistream/1.0.0",
                "ls",
            )
        }
    }

    @Test
    fun protocolNegotiation() = runTest {
        val mux = MultistreamMuxer<Utf8Connection>()
        mux.addHandler(ProtocolId.of("/a"))
        mux.addHandler(ProtocolId.of("/b"))
        mux.addHandler(ProtocolId.of("/c"))
        val job = launch {
            val selected = mux.negotiate(localConnection).expectNoErrors()
            assertEquals("/a", selected.protocol.id, "incorrect protocol selected")
        }
        MultistreamMuxer.selectProtoOrFail(ProtocolId.of("/a"), remoteConnection).expectNoErrors()
        job.join()
    }

    @Test
    fun selectOne() = runTest {
        muxer.addHandler(ProtocolId.of("/a"))
        muxer.addHandler(ProtocolId.of("/b"))
        muxer.addHandler(ProtocolId.of("/c"))
        val job = launch {
            val selected = muxer.negotiate(localConnection).expectNoErrors()
            assertEquals("/c", selected.protocol.id, "incorrect protocol selected")
        }
        val selected = MultistreamMuxer.selectOneOf(
            setOf(
                ProtocolId.of("/d"),
                ProtocolId.of("/e"),
                ProtocolId.of("/c"),
            ),
            remoteConnection,
        ).expectNoErrors()
        assertEquals("/c", selected.id, "incorrect protocol selected")
        job.join()
    }

    @Test
    fun selectFails() = runTest {
        muxer.addHandler(ProtocolId.of("/a"))
        muxer.addHandler(ProtocolId.of("/b"))
        muxer.addHandler(ProtocolId.of("/c"))
        val job = launch {
            muxer.negotiate(localConnection)
            localConnection.close()
        }
        coAssertErrorResult("Peer does not support any of the given protocols") { MultistreamMuxer.selectOneOf(setOf(ProtocolId.of("/d"), ProtocolId.of("/e")), remoteConnection) }
        remoteConnection.close()
        job.join()
    }

    @Test
    fun removeProtocol() = runTest {
        muxer.addHandler(ProtocolId.of("/a"))
        muxer.addHandler(ProtocolId.of("/b"))
        muxer.addHandler(ProtocolId.of("/c"))
        assertEquals(listOf("/a", "/b", "/c"), muxer.protocols().map { it.id }.sorted())
        muxer.removeHandler(ProtocolId.of("/b"))
        assertEquals(listOf("/a", "/c"), muxer.protocols().map { it.id }.sorted())
    }

    @Test
    fun handleFunc() = runTest {
        muxer.addHandler(ProtocolId.of("/a"))
        muxer.addHandler(ProtocolId.of("/b"))
        muxer.addHandler(ProtocolId.of("/c")) { p, _ ->
            assertEquals(ProtocolId.of("/c"), p, "incorrect protocol selected")
            Ok(Unit)
        }
        val job = launch {
            MultistreamMuxer.selectProtoOrFail(ProtocolId.of("/c"), localConnection).expectNoErrors()
        }
        val job2 = muxer.handle(this, remoteConnection).expectNoErrors()
        job.join()
        job2.join()
    }

    @Test
    fun simOpenClientServer() = runTest {
        muxer.addHandler(ProtocolId.of("/a"))
        val job = launch {
            val selected = muxer.negotiate(localConnection).expectNoErrors()
            assertEquals("/a", selected.protocol.id, "incorrect protocol selected")
        }
        val simOpenInfo = MultistreamMuxer.selectWithSimopenOrFail(setOf(ProtocolId.of("/a")), remoteConnection).expectNoErrors()
        assertEquals("/a", simOpenInfo.protocol.id, "incorrect protocol selected")
        assertFalse(simOpenInfo.server)
        job.join()
    }

    @Test
    fun simOpenClientServerFail() = runTest {
        muxer.addHandler(ProtocolId.of("/a"))
        launch {
            coAssertErrorResult("end negotiating: we do not support any of the requested protocols") { muxer.negotiate(localConnection) }
        }
        coAssertErrorResult("Peer does not support any of the given protocols") { MultistreamMuxer.selectWithSimopenOrFail(setOf(ProtocolId.of("/b")), remoteConnection) }
        remoteConnection.close()
    }

    @Test
    fun simOpenClientClient() = runTest {
        var simOpenInfo: SimOpenInfo? = null
        val job = launch {
            simOpenInfo = MultistreamMuxer.selectWithSimopenOrFail(setOf(ProtocolId.of("/a")), remoteConnection).expectNoErrors()
            assertEquals("/a", simOpenInfo!!.protocol.id, "incorrect protocol selected")
        }
        val simOpenInfo2 = MultistreamMuxer.selectWithSimopenOrFail(setOf(ProtocolId.of("/a")), localConnection).expectNoErrors()
        assertEquals("/a", simOpenInfo2.protocol.id, "incorrect protocol selected")
        job.join()
        assertNotEquals(simOpenInfo!!.server, simOpenInfo2.server)
    }

    @Test
    fun simOpenClientClient2() = runTest {
        var simOpenInfo: SimOpenInfo? = null
        val job = launch {
            simOpenInfo = MultistreamMuxer.selectWithSimopenOrFail(setOf(ProtocolId.of("/a"), ProtocolId.of("/b")), remoteConnection).expectNoErrors()
            assertEquals("/b", simOpenInfo!!.protocol.id, "incorrect protocol selected")
        }
        val simOpenInfo2 = MultistreamMuxer.selectWithSimopenOrFail(setOf(ProtocolId.of("/b")), localConnection).expectNoErrors()
        assertEquals("/b", simOpenInfo2.protocol.id, "incorrect protocol selected")
        job.join()
        assertNotEquals(simOpenInfo!!.server, simOpenInfo2.server)
    }

    @Test
    fun simOpenClientClientFail() = runTest {
        val job = launch {
            coAssertErrorResult("Peer does not support any of the given protocols") { MultistreamMuxer.selectWithSimopenOrFail(setOf(ProtocolId.of("/a")), remoteConnection) }
            remoteConnection.close()
        }
        coAssertErrorResult("Peer does not support any of the given protocols") { MultistreamMuxer.selectWithSimopenOrFail(setOf(ProtocolId.of("/b")), localConnection) }
        localConnection.close()
        job.join()
    }

    private suspend fun remoteSends(vararg messages: String) {
        val packet = buildPacket {
            for (message in messages) {
                val messageNewline = message + '\n'
                writeUnsignedVarInt(messageNewline.length.toULong())
                writeFully(messageNewline.toByteArray())
            }
        }
        remoteConnection.output.writePacket(packet)
        remoteConnection.output.flush()
    }

    private suspend fun remoteReceived(vararg messages: String) {
        val packet = buildPacket {
            for (message in messages) {
                val messageNewline = message + '\n'
                writeUnsignedVarInt(messageNewline.length.toULong())
                writeFully(messageNewline.toByteArray())
            }
        }
        val expected = String(packet.readBytes())
        val actual = String(remoteConnection.readAll().readBytes())
        assertEquals(expected, actual)
    }
}

fun BytePacketBuilder.writeUnsignedVarInt(x: ULong): Result<Int> {
    return UVarInt.writeUnsignedVarInt(x) {
        try {
            this.writeByte(it)
            Ok(Unit)
        } catch (e: Exception) {
            Err(Errors.EndOfStream)
        }
    }
}
