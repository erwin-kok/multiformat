package org.erwinkok.multiformat.util

import org.erwinkok.result.Err
import org.erwinkok.result.Errors
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

fun InputStream.readUnsignedVarInt(): Result<ULong> {
    return UVarInt.readUnsignedVarInt { index ->
        try {
            val b = read()
            if (b < 0) {
                if (index != 0) {
                    Err(Errors.UnexpectedEndOfStream)
                } else {
                    Err(Errors.EndOfStream)
                }
            } else {
                Ok(b.toUByte())
            }
        } catch (e: IOException) {
            Err(Errors.EndOfStream)
        }
    }
}

fun OutputStream.writeUnsignedVarInt(x: Int): Result<Int> {
    return writeUnsignedVarInt(x.toULong())
}

fun OutputStream.writeUnsignedVarInt(x: Long): Result<Int> {
    return writeUnsignedVarInt(x.toULong())
}

fun OutputStream.writeUnsignedVarInt(x: ULong): Result<Int> {
    return UVarInt.writeUnsignedVarInt(x) {
        try {
            this.write(it.toInt())
            Ok(Unit)
        } catch (e: IOException) {
            Err(Errors.EndOfStream)
        }
    }
}
