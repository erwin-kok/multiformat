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
    private val connection = TestTransportConnection()
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
            "/proto1"
        )

        val selectOneResult = MultistreamMuxer.selectOneOf(setOf("/proto1"), localConnection).expectNoErrors()
        assertEquals("/proto1", selectOneResult)

        remoteReceived(
            "/multistream/1.0.0",
            "/proto1"
        )
    }

    @Test
    fun selectOneOfMultiple() = runTest {
        remoteSends(
            "/multistream/1.0.0",
            "na",
            "na",
            "/proto3"
        )

        val selectOneResult = MultistreamMuxer.selectOneOf(setOf("/proto1", "/proto2", "/proto3", "/proto4", "/proto5"), localConnection).expectNoErrors()
        assertEquals("/proto3", selectOneResult)

        remoteReceived(
            "/multistream/1.0.0",
            "/proto1",
            "/proto2",
            "/proto3"
        )
    }

    @Test
    fun selectOneOfNA() = runTest {
        remoteSends(
            "/multistream/1.0.0",
            "na"
        )

        val selectOneResult = MultistreamMuxer.selectOneOf(setOf("/proto2"), localConnection)
        assertEquals(Error("Peer does not support any of the given protocols"), selectOneResult.getError())

        remoteReceived(
            "/multistream/1.0.0",
            "/proto2"
        )
    }

    @Test
    fun negotiateMatchNoHandler() = runTest {
        remoteSends(
            "/multistream/1.0.0",
            "/proto3"
        )

        muxer.addHandler("/proto3")
        val negotiateResult = muxer.negotiate(localConnection).expectNoErrors()
        assertEquals("/proto3", negotiateResult.protocol)
        assertEquals(null, negotiateResult.handler)

        remoteReceived(
            "/multistream/1.0.0",
            "/proto3"
        )
    }

    @Test
    fun negotiateMatchWithHandler() {
        runTest {
            remoteSends(
                "/multistream/1.0.0",
                "/proto4"
            )

            var hit = false
            muxer.addHandler("/proto4") { protocol, connection ->
                assertEquals("AProtocol", protocol)
                assertSame(remoteConnection, connection)
                hit = true
                Ok(Unit)
            }
            val negotiateResult = muxer.negotiate(localConnection).expectNoErrors()
            assertEquals("/proto4", negotiateResult.protocol)
            assertNotNull(negotiateResult.handler)
            negotiateResult.handler?.invoke("AProtocol", remoteConnection)
            assertTrue(hit)

            remoteReceived(
                "/multistream/1.0.0",
                "/proto4"
            )
        }
    }

    @Test
    fun list() {
        runTest {
            remoteSends(
                "/multistream/1.0.0",
                "/proto1\n/proto2\n/proto3/sub-proto\n"
            )

            val listResult = muxer.list(localConnection).expectNoErrors()
            assertEquals("/proto1, /proto2, /proto3/sub-proto", listResult.joinToString(", "))

            remoteReceived(
                "/multistream/1.0.0",
                "ls"
            )
        }
    }

    @Test
    fun protocolNegotiation() = runTest {
        val mux = MultistreamMuxer<Utf8Connection>()
        mux.addHandler("/a")
        mux.addHandler("/b")
        mux.addHandler("/c")
        val job = launch {
            val selected = mux.negotiate(localConnection).expectNoErrors()
            assertEquals("/a", selected.protocol, "incorrect protocol selected")
        }
        MultistreamMuxer.selectProtoOrFail("/a", remoteConnection).expectNoErrors()
        job.join()
    }

    @Test
    fun selectOne() = runTest {
        muxer.addHandler("/a")
        muxer.addHandler("/b")
        muxer.addHandler("/c")
        val job = launch {
            val selected = muxer.negotiate(localConnection).expectNoErrors()
            assertEquals("/c", selected.protocol, "incorrect protocol selected")
        }
        val selected = MultistreamMuxer.selectOneOf(setOf("/d", "/e", "/c"), remoteConnection).expectNoErrors()
        assertEquals("/c", selected, "incorrect protocol selected")
        job.join()
    }

    @Test
    fun selectFails() = runTest {
        muxer.addHandler("/a")
        muxer.addHandler("/b")
        muxer.addHandler("/c")
        val job = launch {
            muxer.negotiate(localConnection)
            localConnection.close()
        }
        coAssertErrorResult("Peer does not support any of the given protocols") { MultistreamMuxer.selectOneOf(setOf("/d", "/e"), remoteConnection) }
        remoteConnection.close()
        job.join()
    }

    @Test
    fun removeProtocol() = runTest {
        muxer.addHandler("/a")
        muxer.addHandler("/b")
        muxer.addHandler("/c")
        assertEquals(listOf("/a", "/b", "/c"), muxer.protocols().sorted())
        muxer.removeHandler("/b")
        assertEquals(listOf("/a", "/c"), muxer.protocols().sorted())
    }

    @Test
    fun handleFunc() = runTest {
        muxer.addHandler("/a")
        muxer.addHandler("/b")
        muxer.addHandler("/c") { p, _ ->
            assertEquals("/c", p, "incorrect protocol selected")
            Ok(Unit)
        }
        val job = launch {
            MultistreamMuxer.selectProtoOrFail("/c", localConnection).expectNoErrors()
        }
        muxer.handle(this, remoteConnection).expectNoErrors()
        job.join()
    }

    @Test
    fun simOpenClientServer() = runTest {
        muxer.addHandler("/a")
        val job = launch {
            val selected = muxer.negotiate(localConnection).expectNoErrors()
            assertEquals("/a", selected.protocol, "incorrect protocol selected")
        }
        val simOpenInfo = MultistreamMuxer.selectWithSimopenOrFail(setOf("/a"), remoteConnection).expectNoErrors()
        assertEquals("/a", simOpenInfo.protocol, "incorrect protocol selected")
        assertFalse(simOpenInfo.server)
        job.join()
    }

    @Test
    fun simOpenClientServerFail() = runTest {
        muxer.addHandler("/a")
        launch {
            coAssertErrorResult("EndOfStream") { muxer.negotiate(localConnection) }
        }
        coAssertErrorResult("Peer does not support any of the given protocols") { MultistreamMuxer.selectWithSimopenOrFail(setOf("/b"), remoteConnection) }
        remoteConnection.close()
    }

    @Test
    fun simOpenClientClient() = runTest {
        var simOpenInfo: SimOpenInfo? = null
        val job = launch {
            simOpenInfo = MultistreamMuxer.selectWithSimopenOrFail(setOf("/a"), remoteConnection).expectNoErrors()
            assertEquals("/a", simOpenInfo!!.protocol, "incorrect protocol selected")
        }
        val simOpenInfo2 = MultistreamMuxer.selectWithSimopenOrFail(setOf("/a"), localConnection).expectNoErrors()
        assertEquals("/a", simOpenInfo2.protocol, "incorrect protocol selected")
        job.join()
        assertNotEquals(simOpenInfo!!.server, simOpenInfo2.server)
    }

    @Test
    fun simOpenClientClient2() = runTest {
        var simOpenInfo: SimOpenInfo? = null
        val job = launch {
            simOpenInfo = MultistreamMuxer.selectWithSimopenOrFail(setOf("/a", "/b"), remoteConnection).expectNoErrors()
            assertEquals("/b", simOpenInfo!!.protocol, "incorrect protocol selected")
        }
        val simOpenInfo2 = MultistreamMuxer.selectWithSimopenOrFail(setOf("/b"), localConnection).expectNoErrors()
        assertEquals("/b", simOpenInfo2.protocol, "incorrect protocol selected")
        job.join()
        assertNotEquals(simOpenInfo!!.server, simOpenInfo2.server)
    }

    @Test
    fun simOpenClientClientFail() = runTest {
        val job = launch {
            coAssertErrorResult("Peer does not support any of the given protocols") { MultistreamMuxer.selectWithSimopenOrFail(setOf("/a"), remoteConnection) }
            remoteConnection.close()
        }
        coAssertErrorResult("Peer does not support any of the given protocols") { MultistreamMuxer.selectWithSimopenOrFail(setOf("/b"), localConnection) }
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
