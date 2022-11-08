package org.erwinkok.multiformat.multistream

import org.erwinkok.result.Result

data class ProtocolHandlerInfo<T : Utf8Connection>(
    val match: (String) -> Boolean,
    val protocol: String,
    val handler: (suspend (protocol: String, stream: T) -> Result<Unit>)? = null
)
