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
import moe.kurenai.bot.util.BgmUtil.appendInfoBox
import moe.kurenai.bot.util.BgmUtil.category
import moe.kurenai.bot.util.BgmUtil.getLarge
import moe.kurenai.bot.util.BgmUtil.toGrid
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.TelegramUtil.trimMessage

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
internal object SubjectService {

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

        val formattedText = FormattedTextBuilder().appendText("[${sub.type.category()}]　")
            .appendLink(sub.name, link)
            .appendLine().appendLine()
            .appendInfoBox(sub.infobox)
            .wrapQuoteIfNeeded {
                appendText(sub.summary)
            }.build()
            .trimMessage()

        val itemTitle = sub.name + "(${sub.nameCn})"
        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                this.id = "S${sub.id}"
                this.thumbnailUrl = sub.images.getLarge().toGrid()
                this.title = itemTitle
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


        BgmUtil.handleOgImageInfo(sub.infobox) { title, url, i ->
            resultList.add(
                InputInlineQueryResultArticle().apply {
                    this.id = "S${sub.id}_${i + 1}"
                    this.title = title.ifBlank { itemTitle }
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
