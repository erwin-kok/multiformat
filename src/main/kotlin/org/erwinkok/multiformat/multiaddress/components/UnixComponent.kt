// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multiaddress.Transcoder
import org.erwinkok.result.Ok
import org.erwinkok.result.Result

class UnixComponent private constructor(private val path: String) : Component(Protocol.UNIX, path.toByteArray()) {
    override val value: String
        get() = path

    companion object : Transcoder {
        override fun bytesToComponent(protocol: Protocol, bytes: ByteArray): Result<UnixComponent> {
            return stringToComponent(protocol, String(bytes))
        }

        override fun stringToComponent(protocol: Protocol, string: String): Result<UnixComponent> {
            return Ok(UnixComponent(string))
        }
    }
}
