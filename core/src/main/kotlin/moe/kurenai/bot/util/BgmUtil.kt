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

    suspend fun fetchOgImageInfo(infoBoxes: List<InfoBox>?): Map<String, MutableList<String>> {
        if (infoBoxes.isNullOrEmpty()) return emptyMap()
        val map = HashMap<String, MutableList<String>>()
        infoBoxes.flatMap {
            it.value.filter { it.v?.startsWith("http")?:false }.map { it.v!! }
        }.mapNotNull {
            kotlin.runCatching {
                HttpUtil.getOgImageInfo(Url(it))
            }.getOrNull()
        }.forEach { (title, list) ->
            val value = map.getOrPut(title) { mutableListOf() }
            value.addAll(list)
        }
        return map
    }

    suspend fun handleOgImageInfo(infoBox: List<InfoBox>?, block: (title: String, url: String, i: Int) -> Unit) {
        val info = fetchOgImageInfo(infoBox)
        for ((title, list) in info) {
            var i = 0
            for (url in list) {
                block(title, url, i++)
                i++
            }
        }
    }

    fun FormattedTextBuilder.appendInfoBox(infoBoxes: List<InfoBox>?): FormattedTextBuilder {
        if (infoBoxes?.isEmpty()?:true) return this
        val infoList = infoBoxes.filter { it.value.size > 1 || it.value.isNotEmpty() && it.value[0].v != null }
        if (infoList.isEmpty()) return this

        if (infoList.size > 8) {
            wrapQuote {
                joinInfoBoxList(infoList)
            }
        } else {
            joinInfoBoxList(infoList)
            appendLine()
            appendLine()
        }

        return this
    }

    private fun FormattedTextBuilder.joinInfoBoxList(infoBoxes: List<InfoBox>) {
        joinList(infoBoxes, "\n") { (label, items) ->
            appendText("$label: ")
            var first = true
            for (item in items) {
                if (first) {
                    first = false
                    if (item.k != null) {
                        appendLine()
                        appendText("        ${item.k}: ")
                    }
                    appendCode(item.v?:"")
                } else {
                    if (item.k != null) {
                        appendLine()
                        appendText("        ${item.k}  ")
                        appendCode(item.v?:"")
                    } else {
                        item.v?.let {
                            appendLine()
                            appendText("        ")
                            appendCode(it)
                        }
                    }
                }
            }
        }
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
        var first = true
        return infoBox.key to infoBox.value.joinToString("\n") { item ->
            if (item.k != null) {
                if (first) {
                    first = false
                    "\n    ${item.k} ${item.v}"
                } else {
                    "    ${item.k} ${item.v}"
                }
            } else {
                if (first) {
                    first = false
                    item.v!!
                } else {
                    "    ${item.v}"
                }
            }
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
