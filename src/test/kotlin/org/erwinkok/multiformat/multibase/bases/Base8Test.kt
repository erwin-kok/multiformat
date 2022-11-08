// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.util.Random

internal class Base8Test {
    @Test
    fun `round trip`() {
        val random = Random()
        for (i in 0..512) {
            val data = ByteArray(256)
            random.nextBytes(data)
            val encoded = Base8.encode(data)
            val decoded = Base8.decode(encoded).expectNoErrors()
            assertArrayEquals(data, decoded)
        }
    }

    @Test
    fun empty() {
        assertErrorResult("can not decode zero-length string") { Base8.decode("") }
    }

    @Test
    fun `invalid character`() {
        assertErrorResult("invalid alphabet digit (9)") { Base8.decode("9") }
    }
}
