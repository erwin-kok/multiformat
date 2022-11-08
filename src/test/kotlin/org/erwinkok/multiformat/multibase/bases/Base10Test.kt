// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.assertErrorResult
import org.erwinkok.result.expectNoErrors
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.util.Random

internal class Base10Test {
    @Test
    fun `round trip`() {
        val random = Random()
        for (i in 0..512) {
            val data = ByteArray(256)
            random.nextBytes(data)
            val encoded = Base10.encode(data)
            val decoded = Base10.decode(encoded).expectNoErrors()
            assertArrayEquals(data, decoded)
        }
    }

    @Test
    fun empty() {
        assertErrorResult("can not decode zero-length string") { Base10.decode("") }
    }

    @Test
    fun `invalid character`() {
        assertErrorResult("InvalidCharacter in base 10") { Base10.decode("sfjklgh") }
    }
}
