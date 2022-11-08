package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class UnixComponent private constructor(val path: String) : Component(Protocol.UNIX, path.toByteArray()) {
    override val value: String
        get() = path

    companion object {
        fun fromBytes(bytes: ByteArray): Result<UnixComponent> {
            return fromString(String(bytes))
        }

        fun fromString(string: String): Result<UnixComponent> {
            return Ok(UnixComponent(string))
        }
    }
}
