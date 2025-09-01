package moe.kurenai.bot.service.bangumi

import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import moe.kurenai.bangumi.models.Subject
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi
import moe.kurenai.bot.util.BgmUtil
import moe.kurenai.bot.util.BgmUtil.appendFormattedInfoBox
import moe.kurenai.bot.util.BgmUtil.category
import moe.kurenai.bot.util.BgmUtil.formatToList
import moe.kurenai.bot.util.BgmUtil.getLarge
import moe.kurenai.bot.util.BgmUtil.toGrid
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.TelegramUtil.trimMessage

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

        val infoBox = sub.infobox?.formatToList() ?: emptyList()
        val formattedText = FormattedTextBuilder().appendText("[${sub.type.category()}]　")
            .appendLink(sub.name, link)
            .appendLine().appendLine()
            .appendFormattedInfoBox(infoBox)
            .wrapQuote {
                appendText(sub.summary)
            }.build()
            .trimMessage()

        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                this.id = "S${sub.id}"
                this.thumbnailUrl = sub.images.getLarge().toGrid()
                this.title = sub.name + "(${sub.nameCn})"
                inputMessageContent = InputMessageText().apply {
                    this.text = formattedText
                    this.linkPreviewOptions = LinkPreviewOptions().apply {
                        this.url = sub.images.getLarge()
                        this.forceLargeMedia = true
                        this.showAboveText = true
                    }
                }
            },
        )


        BgmUtil.handleOgImageInfo(infoBox) { title, url, i ->
            resultList.add(
                InputInlineQueryResultArticle().apply {
                    this.id = "S${sub.id}_${i + 1}"
                    this.title = title
                    this.thumbnailUrl = url
                    this.inputMessageContent = InputMessageText().apply {
                        this.text = formattedText
                        this.linkPreviewOptions = LinkPreviewOptions().apply {
                            this.url = url
                            this.forceLargeMedia = true
                            this.showAboveText = true
                        }
                    }
                }
            )
        }

        return resultList.toTypedArray()
    }

}
