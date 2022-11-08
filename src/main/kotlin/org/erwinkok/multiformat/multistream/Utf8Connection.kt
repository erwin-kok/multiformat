package org.erwinkok.multiformat.multistream

import org.erwinkok.result.Result

interface Utf8Connection {
    val availableForRead: Int
    suspend fun readUtf8(): Result<String>
    suspend fun writeUtf8(vararg messages: String): Result<Unit>
    suspend fun close()
}
