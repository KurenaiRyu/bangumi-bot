package moe.kurenai.bot.util

import io.ktor.http.*
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

    suspend fun fetchOgImageInfo(infoBox: List<Pair<String, String>>): Map<String, MutableList<String>> {
        val map = HashMap<String, MutableList<String>>()
        infoBox.filter { it.second.startsWith("http") }.mapNotNull {
            kotlin.runCatching {
                HttpUtil.getOgImageInfo(Url(it.second))
            }.getOrNull()
        }.forEach { (title, list) ->
            val value = map.getOrPut(title) { mutableListOf() }
            value.addAll(list)
        }
        return map
    }

    suspend fun handleOgImageInfo(infoBox: List<Pair<String, String>>, block: (title: String, url: String, i: Int) -> Unit) {
        val info = fetchOgImageInfo(infoBox)
        for ((title, list) in info) {
            var i = 0
            for (url in list) {
                block(title, url, i++)
                i++
            }
        }
    }

    fun FormattedTextBuilder.appendInfoBox(infoBox: List<InfoBox>?): FormattedTextBuilder {
        if (infoBox?.isEmpty()?:true) return this

        return this.appendFormattedInfoBox(infoBox.formatToList())
    }

    fun FormattedTextBuilder.appendFormattedInfoBox(infoBox: List<Pair<String, String>>): FormattedTextBuilder {
        val info = infoBox.filter { it.second.isNotBlank() }
        if (info.isEmpty()) return this

        val handle = {
            joinList(info, "\n\n") { (k, v) ->
                appendBold(k)
                appendText(": $v")
            }
        }

        if (info.size > 8) {
            wrapQuote {
                handle()
            }
        } else {
            handle()
            appendLine()
            appendLine()
        }

        return this
    }

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
        val url =
            "${ApiClient.OAUTH_BASE_URL}/oauth/authorize?client_id=$appId&response_type=code&redirect_uri=$redirectUri"
        return state?.let { "$url&state=$state" } ?: url
    }

}
