package moe.kurenai.bot.util

import com.fasterxml.jackson.databind.JsonNode

/**
 * @author Kurenai
 * @since 2023/1/26 18:09
 */

object BgmUtil {
    fun JsonNode.formatInfoBox(): String {
        return this.joinToString("\n") { node ->
            val (k, v) = formatInfoBox(node)
            "$k: $v"
        }
    }

    fun JsonNode.formatInfoBoxToList(): List<Pair<String, String>> {
        return this.map { node ->
            formatInfoBox(node)
        }
    }

    private fun formatInfoBox(node: JsonNode): Pair<String, String> {
        val valueNode = node.findValue("value")
        val value = if (valueNode.isTextual) valueNode.textValue()
        else valueNode
            .toList()
            .joinToString("、") { it.findValue("v").textValue() }
        return node.findValue("key").textValue() to value
    }

    fun Int.category(): String = when (this) {
        1 -> "书籍" //book
        2 -> "动画" //anime
        3 -> "音乐" //music
        4 -> "游戏" //game
        6 -> "三次元" //real
        else -> "？？"        //?
    }
}
