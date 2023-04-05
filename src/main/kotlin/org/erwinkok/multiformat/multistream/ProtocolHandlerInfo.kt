// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multistream

import org.erwinkok.result.Result

data class ProtocolHandlerInfo<T : Utf8Connection>(
    val match: (ProtocolId) -> Boolean,
    val protocol: ProtocolId,
    val handler: (suspend (protocol: ProtocolId, stream: T) -> Result<Unit>)? = null,
)
