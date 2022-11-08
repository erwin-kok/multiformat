package org.erwinkok.multiformat.multiaddress.components

import org.erwinkok.multiformat.multiaddress.Protocol
import org.erwinkok.multiformat.multibase.bases.Base64
import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.getOrElse

class Garlic64Component private constructor(addressBytes: ByteArray) : Component(Protocol.GARLIC64, addressBytes) {
    override val value: String
        get() = Base64.encodeToStringStd(addressBytes).replace("\\+".toRegex(), "-").replace("/".toRegex(), "~")

    companion object {
        fun fromBytes(bytes: ByteArray): Result<Garlic64Component> {
            return Ok(Garlic64Component(bytes))
        }

        fun fromString(address: String): Result<Garlic64Component> {
            // i2p base64 address will be between 516 and 616 characters long, depending on certificate type
            if (address.length < 516 || address.length > 616) {
                return Err("Invalid garlic addr: $address not a i2p base64 address. len: ${address.length}")
            }
            val replace = address.replace("-".toRegex(), "+").replace("~".toRegex(), "/")
            val bytes = Base64.decodeStringStd(replace)
                .getOrElse { return Err("Invalid garlic addr: ${address.take(16)}... Could not decode Multibase") }
            if (bytes.size < 386) {
                return Err("Invalid garlic64 address length: ${bytes.size}")
            }
            return Ok(Garlic64Component(bytes))
        }
    }
}
