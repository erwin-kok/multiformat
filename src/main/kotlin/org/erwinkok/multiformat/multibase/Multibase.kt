// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase

import org.erwinkok.multiformat.multibase.bases.Base10
import org.erwinkok.multiformat.multibase.bases.Base16
import org.erwinkok.multiformat.multibase.bases.Base2
import org.erwinkok.multiformat.multibase.bases.Base256Emoji
import org.erwinkok.multiformat.multibase.bases.Base32
import org.erwinkok.multiformat.multibase.bases.Base36
import org.erwinkok.multiformat.multibase.bases.Base58
import org.erwinkok.multiformat.multibase.bases.Base64
import org.erwinkok.multiformat.multibase.bases.Base8
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.flatMap
import org.erwinkok.result.map
import java.nio.charset.StandardCharsets

enum class Multibase(val code: String, val encoding: String, private val encoder: (ByteArray) -> String, private val decoder: (String) -> Result<ByteArray>) {
    // https://github.com/multiformats/multibase
    IDENTITY("\u0000", "identity", { data -> String(data, StandardCharsets.UTF_8) }, { data -> Ok(data.toByteArray(StandardCharsets.UTF_8)) }),
    BASE2("0", "base2", { data -> Base2.encode(data) }, { data -> Base2.decode(data) }),
    BASE8("7", "base8", { data -> Base8.encode(data) }, { data -> Base8.decode(data) }),
    BASE10("9", "base10", { data -> Base10.encode(data) }, { data -> Base10.decode(data) }),
    BASE16("f", "base16", { data -> Base16.encodeToStringLc(data) }, { data -> Base16.decode(data) }),
    BASE16_UPPER("F", "base16upper", { data -> Base16.encodeToStringUc(data) }, { data -> Base16.decode(data) }),
    BASE32("b", "base32", { data -> Base32.encodeStdLowerNoPad(data) }, { data -> Base32.decodeStdNoPad(data) }),
    BASE32_UPPER("B", "base32upper", { data -> Base32.encodeStdUpperNoPad(data) }, { data -> Base32.decodeStdNoPad(data) }),
    BASE32_HEX("v", "base32hex", { data -> Base32.encodeHexLowerNoPad(data) }, { data -> Base32.decodeHexNoPad(data) }),
    BASE32_HEX_UPPER("V", "base32hexupper", { data -> Base32.encodeHexUpperNoPad(data) }, { data -> Base32.decodeHexNoPad(data) }),
    BASE32_PAD("c", "base32pad", { data -> Base32.encodeStdLowerPad(data) }, { data -> Base32.decodeStdPad(data) }),
    BASE32_PAD_UPPER("C", "base32padupper", { data -> Base32.encodeStdUpperPad(data) }, { data -> Base32.decodeStdPad(data) }),
    BASE32_HEX_PAD("t", "base32hexpad", { data -> Base32.encodeHexLowerPad(data) }, { data -> Base32.decodeHexPad(data) }),
    BASE32_HEX_PAD_UPPER("T", "base32hexpadupper", { data -> Base32.encodeHexUpperPad(data) }, { data -> Base32.decodeHexPad(data) }),
    BASE32_Z("h", "base32z", { data -> Base32.encodeZ(data) }, { data -> Base32.decodeZ(data) }),
    BASE36("k", "base36", { data -> Base36.encodeToStringLc(data) }, { data -> Base36.decodeString(data) }),
    BASE36_UPPER("K", "base36upper", { data -> Base36.encodeToStringUc(data) }, { data -> Base36.decodeString(data) }),
    BASE58_BTC("z", "base58btc", { data -> Base58.encodeToStringBtc(data) }, { data -> Base58.decodeStringBtc(data) }),
    BASE58_FLICKR("Z", "base58flickr", { data -> Base58.encodeToStringFlickr(data) }, { data -> Base58.decodeStringFlickr(data) }),
    BASE64("m", "base64", { data -> Base64.encodeToStringStd(data) }, { data -> Base64.decodeStringStd(data) }),
    BASE64_URL("u", "base64url", { data -> Base64.encodeToStringUrl(data) }, { data -> Base64.decodeStringUrl(data) }),
    BASE64_PAD("M", "base64pad", { data -> Base64.encodeToStringPad(data) }, { data -> Base64.decodeStringPad(data) }),
    BASE64_URL_PAD("U", "base64urlpad", { data -> Base64.encodeToStringUrlPad(data) }, { data -> Base64.decodeStringUrlPad(data) }),
    BASE256_EMOJI("🚀", "base256emoji", { data -> Base256Emoji.encodeToString(data) }, { data -> Base256Emoji.decodeString(data) }),
    ;

    fun encode(data: ByteArray): String {
        return code + encoder(data)
    }

    fun decode(data: String): Result<ByteArray> {
        val cp = data.codePointAt(0)
        val charCount = Character.charCount(cp)
        return decoder(data.substring(charCount))
    }

    companion object {
        private val codeToBase = mutableMapOf<Int, Multibase>()
        private val nameToBase = mutableMapOf<String, Multibase>()

        init {
            for (base in values()) {
                codeToBase[decodeBase(base.code)] = base
                nameToBase[base.encoding] = base
            }
        }

        fun nameToBase(name: String): Result<Multibase> {
            val base = nameToBase[name] ?: return Err("Unknown Multibase name: $name")
            return Ok(base)
        }

        fun encode(name: String, data: ByteArray): Result<String> {
            return nameToBase(name)
                .map { base -> base.encode(data) }
        }

        fun decode(data: String): Result<ByteArray> {
            return encoding(data)
                .flatMap { it.decode(data) }
        }

        fun encoding(data: String): Result<Multibase> {
            if (data.isEmpty()) {
                return Err("cannot decode Multibase for empty string")
            }
            val baseIndex = decodeBase(data)
            val base = codeToBase[baseIndex] ?: return Err("Could not determine Multibase from: $data")
            return Ok(base)
        }

        private fun decodeBase(data: String): Int {
            return data.codePointAt(0)
        }
    }
}
