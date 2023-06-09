// Copyright (c) 2023 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multistream

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

@Serializable
class ProtocolId private constructor(val id: String) {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ProtocolId) {
            return false
        }
        return id == other.id
    }

    override fun toString(): String {
        return id
    }

    companion object {
        private const val UnknownProtocolId = "<Unknown>"
        private val protocols = ConcurrentHashMap<String, ProtocolId>()

        val Null = ProtocolId(UnknownProtocolId)

        fun from(key: String?): ProtocolId {
            if (key.isNullOrBlank() || key == UnknownProtocolId) {
                return Null
            }
            return protocols.computeIfAbsent(key) {
                ProtocolId(key)
            }
        }
    }
}
