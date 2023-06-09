// Copyright (c) 2023 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multiaddress

import org.erwinkok.multiformat.multiaddress.components.Component
import org.erwinkok.result.Result

interface Transcoder {
    fun bytesToComponent(protocol: Protocol, bytes: ByteArray): Result<Component>
    fun stringToComponent(protocol: Protocol, string: String): Result<Component>
}
