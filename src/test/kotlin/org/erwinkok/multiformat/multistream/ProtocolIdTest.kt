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
        assertSame(ProtocolId.of("/abc"), ProtocolId.of("/abc"))
    }

    @Test
    fun notTheSameProtocol() {
        assertNotSame(ProtocolId.of("/abc"), ProtocolId.of("/def"))
    }

    @Test
    fun nullProtocol() {
        assertSame(ProtocolId.Null, ProtocolId.of("<Unknown>"))
        assertSame(ProtocolId.Null, ProtocolId.of(""))
        assertSame(ProtocolId.Null, ProtocolId.of(null))
        assertEquals("<Unknown>", ProtocolId.Null.id)
    }
}
