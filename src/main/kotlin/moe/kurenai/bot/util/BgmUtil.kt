package moe.kurenai.bot.util

import moe.kurenai.bgm.model.InfoBox

/**
 * @author Kurenai
 * @since 2023/1/26 18:09
 */

object BgmUtil {

    fun List<Pair<String, String>>.format(): String {
        return this.joinToString("\n") { (k, v) ->
            "$k: $v"
        }
    }

    fun List<InfoBox>.formatToList(): List<Pair<String, String>> {
        return this.map { infoBox ->
            format(infoBox)
        }
    }

    private fun format(infoBox: InfoBox): Pair<String, String> {
        return infoBox.key to infoBox.value.joinToString("、") { item ->
            item.v + (item.k?.let { " (${it})" } ?: "")
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

    fun String.toGrid(): String = this.replace("crt/l", "crt/g")
}
