// ktlint-disable filename
package org.erwinkok.multiformat.multicodec

import java.io.BufferedReader
import java.io.File

private data class Codec(
    val name: String,
    val tag: String,
    val code: Int,
    val status: String,
    val description: String
)

fun main() {
    val reader = File("../spec/multicodec/table.csv").bufferedReader()
    val codecs = readCsv(reader)
    val tags = codecs.map { it.tag.replaceFirstChar { ch -> ch.uppercaseChar() } }.distinct().sorted()
    writeTagFile(tags)
    writeEnumFile(codecs)
}

private fun writeEnumFile(codecs: List<Codec>) {
    val writer = File("Multicodec.kt").bufferedWriter()
    writer.appendLine(
        """
//
// GENERATED FILE -- DO NOT EDIT!!
//
package org.erwinkok.multiformat.multicodec

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import java.util.TreeMap
        """.trimIndent()
    )
    writer.appendLine()
    writer.appendLine("enum class Multicodec(val typeName: String, val code: Int, val tag: MulticodecTag) {")
    writer.append(
        codecs.joinToString(separator = ",\n") {
            val name = it.name.uppercase().replace('-', '_')
            val tag = it.tag.replaceFirstChar { ch -> ch.uppercaseChar() }
            "    $name(\"${it.name}\", ${String.format("%#04x", it.code)}, MulticodecTag.$tag)"
        }
    )
    writer.appendLine(";")
    writer.appendLine()
    writer.appendLine(
        """    override fun toString(): String {
        return typeName
    }

    companion object {
        private val codeToName: MutableMap<Int, String> = TreeMap()
        private val nameToType: MutableMap<String, Multicodec> = TreeMap()
        private val codeToType: MutableMap<Int, Multicodec> = TreeMap()

        init {
            for (type in values()) {
                codeToName[type.code] = type.typeName
                nameToType[type.typeName] = type
                codeToType[type.code] = type
            }
        }

        fun codeToName(code: Int): Result<String> {
            val name = codeToName[code] ?: return Err("Unknown Multicodec code: ${'$'}code")
            return Ok(name)
        }

        fun nameToType(name: String): Result<Multicodec> {
            val type = nameToType[name] ?: return Err("Unknown Multicodec name: ${'$'}name")
            return Ok(type)
        }

        fun codeToType(code: Int): Result<Multicodec> {
            val type = codeToType[code] ?: return Err("Unknown Multicodec code: ${'$'}code")
            return Ok(type)
        }
    }"""
    )
    writer.appendLine("}")
    writer.flush()
    writer.close()
}

private fun writeTagFile(distinct: List<String>) {
    val writer = File("MulticodecTag.kt").bufferedWriter()
    writer.appendLine(
        """
//
// GENERATED FILE -- DO NOT EDIT!!
//
package org.erwinkok.multiformat.multicodec

enum class MulticodecTag {
        """.trimIndent()
    )
    writer.appendLine(distinct.joinToString(separator = ",\n") { "    $it" })
    writer.appendLine("}")
    writer.flush()
    writer.close()
}

private fun readCsv(reader: BufferedReader): List<Codec> {
    reader.readLine()
    return reader.lineSequence()
        .filter { it.isNotBlank() }
        .map {
            val (name, tag, code, status, description) = it.split(',', ignoreCase = false, limit = 5)
            Codec(name.trim(), tag.trim(), Integer.decode(code.trim()), status.trim(), description.trim())
        }.toList()
}
