package moe.kurenai.bot.util

import com.fasterxml.jackson.databind.JsonNode

/**
 * @author Kurenai
 * @since 2023/1/26 18:09
 */

object BgmUtil {
    fun JsonNode.formatInfoBox(): String {
        return this.joinToString("\n") { node ->
            val valueNode = node.findValue("value")
            val value = if (valueNode.isTextual) valueNode.textValue()
            else valueNode
                .toList()
                .joinToString("、") { it.findValue("v").textValue() }
            "${node.findValue("key").textValue()}: $value"
        }
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