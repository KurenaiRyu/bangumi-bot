package moe.kurenai.bot.service.bangumi

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import moe.kurenai.bangumi.models.Subject
import moe.kurenai.bangumi.models.SubjectType
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi
import moe.kurenai.bot.util.BgmUtil.category
import moe.kurenai.bot.util.BgmUtil.formatToList
import moe.kurenai.bot.util.BgmUtil.getLarge
import moe.kurenai.bot.util.BgmUtil.toGrid
import moe.kurenai.bot.util.HttpUtil

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
internal object SubjectService {

    private val mainInfoProperties =
        listOf(
            "中文名",
            "话数",
            "放送开始",
            "原作",
            "导演",
            "音乐",
            "人物设定",
            "系列构成",
            "总作画监督",
            "製作",
            "动画制作",
            "别名",
            "官方网站",
            "Copyright"
        )


    context(token: UserAccessToken?)
    suspend fun findById(id: Int): Subject {
        return useApi {
            it.getSubjectById(id).result()
        }
    }

    context(token: UserAccessToken?)
    suspend fun findByIds(ids: Collection<Int>): Collection<Subject> {
        return ids.map { id ->
            CoroutineScope(Dispatchers.Default).async {
                useApi {
                    it.getSubjectById(id).result<Subject>()
                }
            }
        }.awaitAll()
    }

    suspend fun getContent(sub: Subject, link: String): Array<InputInlineQueryResult> {

        val title = "[${sub.type.category()}]　${sub.name}"
        val infoBox = sub.infobox?.formatToList() ?: emptyList()
        val simpleInfoBot =
            if (sub.type == SubjectType.Anime) infoBox.filter { mainInfoProperties.contains(it.first) } else infoBox
        val content = simpleInfoBot.joinToString("\n") { (k, v) ->
            "$k: $v"
        }

        val titleIndex = sub.type.category().length + 3
        var caption = listOfNotNull(title, content).joinToString("\n\n") + "\n\n"
        val summaryIndex = caption.length
        caption += sub.summary

        val formattedText = FormattedText(caption, arrayOf(
            TextEntity(titleIndex, sub.name.length, TextEntityTypeTextUrl(link)),
            TextEntity(summaryIndex + 1, sub.summary.length, TextEntityTypeBlockQuote()),
        ))

        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                this.id = "S${sub.id}_txt"
                this.thumbnailUrl = sub.images.getLarge().toGrid()
                this.title = sub.name + "(${sub.nameCn})"
                inputMessageContent = InputMessageText().apply {
                    this.text = FormattedText(
                        " $caption", arrayOf(
                            TextEntity(0, 1, TextEntityTypeTextUrl(sub.images.getLarge())),
                            TextEntity(titleIndex + 1, sub.name.length, TextEntityTypeTextUrl(link)),
                            TextEntity(summaryIndex + 1, sub.summary.length, TextEntityTypeBlockQuote()),
                        )
                    )
                }
            },
            InputInlineQueryResultPhoto().apply {
                this.id = "S${sub.id}_img"
                this.title = sub.name
                this.photoUrl = sub.images.getLarge()
                this.thumbnailUrl = sub.images.getLarge()
                this.inputMessageContent = InputMessagePhoto().apply {
                    this.caption = formattedText
                }
            }
        )

        infoBox.filter { it.second.startsWith("http") }.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }.forEachIndexed { i, url ->
            resultList.add(
                InputInlineQueryResultPhoto().apply {
                    this.id = "S${sub.id} - ${i + 1}"
                    this.title = sub.name
                    this.photoUrl = url
                    this.thumbnailUrl = url
                    this.inputMessageContent = InputMessagePhoto().apply {
                        this.caption = formattedText
                    }
                }
            )
        }
        return resultList.toTypedArray()
    }

}
