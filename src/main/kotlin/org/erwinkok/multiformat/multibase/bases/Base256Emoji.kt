// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multibase.bases

import org.erwinkok.result.Err
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import kotlin.streams.toList

object Base256Emoji {
    private val base256emojiTable = listOf(
        // Curated list, this is just a list of things that *somwhat* are related to our comunity
        "🚀", "🪐", "☄", "🛰", "🌌", // Space
        "🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘", // Moon
        "🌍", "🌏", "🌎", // Our Home, for now (earth)
        "🐉", // Dragon!!!
        "☀", // Our Garden, for now (sol)
        "💻", "🖥", "💾", "💿", // Computer
        // The rest is completed from https://home.unicode.org/emoji/emoji-frequency/ at the time of creation (december 2021) (the data is from 2019), most used first until we reach 256.
        // We exclude modifier based emojies (such as flags) as they are bigger than one single codepoint.
        // Some other emojies were removed adhoc for various reasons.
        "😂", "❤", "😍", "🤣", "😊", "🙏", "💕", "😭", "😘", "👍",
        "😅", "👏", "😁", "🔥", "🥰", "💔", "💖", "💙", "😢", "🤔",
        "😆", "🙄", "💪", "😉", "☺", "👌", "🤗", "💜", "😔", "😎",
        "😇", "🌹", "🤦", "🎉", "💞", "✌", "✨", "🤷", "😱", "😌",
        "🌸", "🙌", "😋", "💗", "💚", "😏", "💛", "🙂", "💓", "🤩",
        "😄", "😀", "🖤", "😃", "💯", "🙈", "👇", "🎶", "😒", "🤭",
        "❣", "😜", "💋", "👀", "😪", "😑", "💥", "🙋", "😞", "😩",
        "😡", "🤪", "👊", "🥳", "😥", "🤤", "👉", "💃", "😳", "✋",
        "😚", "😝", "😴", "🌟", "😬", "🙃", "🍀", "🌷", "😻", "😓",
        "⭐", "✅", "🥺", "🌈", "😈", "🤘", "💦", "✔", "😣", "🏃",
        "💐", "☹", "🎊", "💘", "😠", "☝", "😕", "🌺", "🎂", "🌻",
        "😐", "🖕", "💝", "🙊", "😹", "🗣", "💫", "💀", "👑", "🎵",
        "🤞", "😛", "🔴", "😤", "🌼", "😫", "⚽", "🤙", "☕", "🏆",
        "🤫", "👈", "😮", "🙆", "🍻", "🍃", "🐶", "💁", "😲", "🌿",
        "🧡", "🎁", "⚡", "🌞", "🎈", "❌", "✊", "👋", "😰", "🤨",
        "😶", "🤝", "🚶", "💰", "🍓", "💢", "🤟", "🙁", "🚨", "💨",
        "🤬", "✈", "🎀", "🍺", "🤓", "😙", "💟", "🌱", "😖", "👶",
        "🥴", "▶", "➡", "❓", "💎", "💸", "⬇", "😨", "🌚", "🦋",
        "😷", "🕺", "⚠", "🙅", "😟", "😵", "👎", "🤲", "🤠", "🤧",
        "📌", "🔵", "💅", "🧐", "🐾", "🍒", "😗", "🤑", "🌊", "🤯",
        "🐷", "☎", "💧", "😯", "💆", "👆", "🎤", "🙇", "🍑", "❄",
        "🌴", "💣", "🐸", "💌", "📍", "🥀", "🤢", "👅", "💡", "💩",
        "👐", "📸", "👻", "🤐", "🤮", "🎼", "🥵", "🚩", "🍎", "🍊",
        "👼", "💍", "📣", "🥂",
    )
    private val forwardTable = HashMap<Int, String>(base256emojiTable.size)
    private val reverseTable = HashMap<Int, Byte>(base256emojiTable.size)

    init {
        for ((i, v) in base256emojiTable.withIndex()) {
            val codePoint = v.codePointAt(0)
            forwardTable[i] = v
            reverseTable[codePoint] = i.toByte()
        }
    }

    fun encodeToString(data: ByteArray): String {
        val sb = StringBuilder()
        for (b in data) {
            sb.append(forwardTable[b.toInt()])
        }
        return sb.toString()
    }

    fun decodeString(data: String): Result<ByteArray> {
        val codePoints = data.codePoints().toList()
        val out = ByteArray(codePoints.size)
        for ((i, cp) in codePoints.withIndex()) {
            out[i] = (reverseTable[cp] ?: return Err("illegal base256emoji data at input byte ${Character.toString(cp)}"))
        }
        return Ok(out)
    }
}
