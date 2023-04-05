// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
@file:OptIn(ExperimentalCoroutinesApi::class)

package org.erwinkok.multiformat.multistream

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class ProtocolIdTest {
    @Test
    fun sameProtocol() {
        assertSame(ProtocolId.from("/abc"), ProtocolId.from("/abc"))
    }

    @Test
    fun notTheSameProtocol() {
        assertNotSame(ProtocolId.from("/abc"), ProtocolId.from("/def"))
    }

    @Test
    fun nullProtocol() {
        assertSame(ProtocolId.Null, ProtocolId.from("<Unknown>"))
        assertSame(ProtocolId.Null, ProtocolId.from(""))
        assertSame(ProtocolId.Null, ProtocolId.from(null))
        assertEquals("<Unknown>", ProtocolId.Null.id)
    }
}
