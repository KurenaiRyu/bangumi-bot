package moe.kurenai.bot.util

import moe.kurenai.bangumi.infrastructure.ApiClient
import moe.kurenai.bangumi.models.Images
import moe.kurenai.bangumi.models.InfoBox
import moe.kurenai.bangumi.models.PersonImages
import moe.kurenai.bangumi.models.SubjectType
import moe.kurenai.bot.Config.Companion.CONFIG

/**
 * @author Kurenai
 * @since 2023/1/26 18:09
 */

object BgmUtil {

    const val DEFAULT_IMAGE = "https://bgm.tv/img/no_icon_subject.png"

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

    fun SubjectType.category(): String = when (this) {
        SubjectType.Book -> "书籍" //book
        SubjectType.Anime -> "动画" //anime
        SubjectType.Music -> "音乐" //music
        SubjectType.Game -> "游戏" //game
        SubjectType.Real -> "三次元" //real
        else -> "？？"        //?
    }

    fun String.toGrid(): String = this.replace("crt/l", "crt/g")

    fun PersonImages?.getLarge(): String {
        return this?.large?.takeIf { it.isNotBlank() } ?: this.getMedium()
    }

    fun PersonImages?.getMedium(): String {
        return this?.medium?.takeIf { it.isNotBlank() } ?: getGrid()
    }

    fun PersonImages?.getGrid(): String {
        return this?.grid?.takeIf { it.isNotBlank() } ?: getSmall()
    }

    fun PersonImages?.getSmall(): String {
        return this?.small?.takeIf { it.isNotBlank() } ?: DEFAULT_IMAGE
    }


    fun Images?.getLarge(): String {
        return this?.large?.takeIf { it.isNotBlank() } ?: this.getMedium()
    }

    fun Images?.getMedium(): String {
        return this?.medium?.takeIf { it.isNotBlank() } ?: getCommon()
    }

    fun Images?.getCommon(): String {
        return this?.common?.takeIf { it.isNotBlank() } ?: getGrid()
    }

    fun Images?.getGrid(): String {
        return this?.grid?.takeIf { it.isNotBlank() } ?: getSmall()
    }

    fun Images?.getSmall(): String {
        return this?.small?.takeIf { it.isNotBlank() } ?: DEFAULT_IMAGE
    }

    fun buildOauthUrl(state: String? = null): String {
        val appId = CONFIG.bgm.appId
        val redirectUri = CONFIG.bgm.redirectUrl
        val url = "${ApiClient.OAUTH_BASE_URL}/authorize?client_id=$appId&response_type=code&redirect_uri=$redirectUri"
        return state?.let { "$url&state=$state" } ?: url
    }

}
