// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multistream

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readBuffer
import io.ktor.utils.io.readByte
import io.ktor.utils.io.writeBuffer
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.readByteArray
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

    class Inner(
        private val input: ByteChannel,
        private val output: ByteChannel,
    ) : Utf8Connection {
        override suspend fun readUtf8(): Result<String> {
            return readUnsignedVarInt()
                .map { it.toInt() }
                .map { wanted ->
                    try {
                        val source = input.readBuffer(wanted)
                        if (!source.request(wanted.toLong())) {
                            return Err("Required $wanted bytes, but stream closed")
                        }
                        val bytes = source.readByteArray(wanted)
                        if (bytes.isEmpty() || Char(bytes[bytes.size - 1].toInt()) != '\n') {
                            return Err("message did not have trailing newline")
                        }
                        return Ok(String(bytes).trim { it <= ' ' })
                    } catch (_: ClosedReceiveChannelException) {
                        return Err(Errors.EndOfStream)
                    }
                }
        }

        override suspend fun writeUtf8(vararg messages: String): Result<Unit> {
            val buffer = Buffer()
            for (message in messages) {
                val messageNewline = message + '\n'
                buffer.writeUnsignedVarInt(messageNewline.length.toULong())
                buffer.write(messageNewline.toByteArray())
            }
            output.writeBuffer(buffer)
            output.flush()
            return Ok(Unit)
        }

        suspend fun readRawBuffer(): Buffer {
            return input.readBuffer()
        }

        suspend fun writeRawBuffer(buffer: Buffer) {
            output.writeBuffer(buffer)
            output.flush()
        }

        override fun close() {
            input.cancel()
            output.close()
        }

        private suspend fun readUnsignedVarInt(): Result<ULong> {
            return UVarInt.coReadUnsignedVarInt { readByte() }
        }

        private suspend fun readByte(): Result<UByte> {
            return try {
                Ok(input.readByte().toUByte())
            } catch (_: EOFException) {
                Err(Errors.EndOfStream)
            } catch (_: ClosedReceiveChannelException) {
                Err(Errors.EndOfStream)
            }
        }
    }
}

fun Buffer.writeUnsignedVarInt(x: ULong): Result<Int> {
    return UVarInt.writeUnsignedVarInt(x) {
        try {
            this.writeByte(it)
            Ok(Unit)
        } catch (_: Exception) {
            Err(Errors.EndOfStream)
        }
    }
}
