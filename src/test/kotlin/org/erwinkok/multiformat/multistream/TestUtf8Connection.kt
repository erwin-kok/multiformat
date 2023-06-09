// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multistream

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.writeFully
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.erwinkok.multiformat.util.UVarInt
import org.erwinkok.result.Err
import org.erwinkok.result.Errors
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.map

class TestUtf8Connection {
    private val input = ByteChannel(true)
    private val output = ByteChannel(true)

    val local = Inner(input, output)
    val remote = Inner(output, input)

    inner class Inner(
        val input: ByteChannel,
        val output: ByteChannel,
    ) : Utf8Connection {
        override suspend fun readUtf8(): Result<String> {
            return readUnsignedVarInt()
                .map { it.toInt() }
                .map { wanted ->
                    try {
                        val packet = input.readPacket(wanted)
                        if (packet.remaining.toInt() != wanted) {
                            val result = Err("Required $wanted bytes, but received ${packet.remaining}")
                            packet.close()
                            return result
                        }
                        val bytes = packet.readBytes()
                        if (bytes.isEmpty() || Char(bytes[bytes.size - 1].toInt()) != '\n') {
                            return Err("message did not have trailing newline")
                        }
                        packet.close()
                        return Ok(String(bytes).trim { it <= ' ' })
                    } catch (e: ClosedReceiveChannelException) {
                        return Err(Errors.EndOfStream)
                    }
                }
        }

        override suspend fun writeUtf8(vararg messages: String): Result<Unit> {
            val packet = buildPacket {
                for (message in messages) {
                    val messageNewline = message + '\n'
                    writeUnsignedVarInt(messageNewline.length.toULong())
                    writeFully(messageNewline.toByteArray())
                }
            }
            output.writePacket(packet)
            return Ok(Unit)
        }

        override fun close() {
            input.cancel()
            output.close()
        }

        suspend fun readAll(): ByteReadPacket {
            return buildPacket {
                val count = input.availableForRead
                writePacket(input.readPacket(count))
            }
        }

        private suspend fun readUnsignedVarInt(): Result<ULong> {
            return UVarInt.coReadUnsignedVarInt { readByte() }
        }

        private suspend fun readByte(): Result<UByte> {
            return try {
                Ok(input.readByte().toUByte())
            } catch (e: ClosedReceiveChannelException) {
                Err(Errors.EndOfStream)
            }
        }
    }
}
