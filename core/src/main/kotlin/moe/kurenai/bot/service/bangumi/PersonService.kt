package moe.kurenai.bot.service.bangumi

import it.tdlight.jni.TdApi.*
import moe.kurenai.bangumi.models.PersonDetail
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi
import moe.kurenai.bot.util.BgmUtil
import moe.kurenai.bot.util.BgmUtil.appendFormattedInfoBox
import moe.kurenai.bot.util.BgmUtil.formatToList
import moe.kurenai.bot.util.BgmUtil.getLarge
import moe.kurenai.bot.util.BgmUtil.toGrid
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.TelegramUtil.trimMessage

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
internal object PersonService {

    context(token: UserAccessToken?)
    suspend fun findById(id: Int): PersonDetail {
        return useApi {
            it.getPersonById(id).result()
        }
    }

    suspend fun getContent(person: PersonDetail, link: String): Array<InputInlineQueryResult> {
        val infoBox = person.infobox?.formatToList()?: emptyList()
        val formattedText = FormattedTextBuilder()
            .appendLink(person.name, link)
            .appendLine().appendLine()
            .appendFormattedInfoBox(infoBox)
            .wrapQuote {
                appendText(person.summary)
            }.build()
            .trimMessage()

        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                this.id = "P${person.id}"
                this.url = person.images.getLarge().toGrid()
                this.title = person.name
                this.inputMessageContent = InputMessageText().apply {
                    this.text = formattedText
                    this.linkPreviewOptions = LinkPreviewOptions().apply {
                        this.url = person.images.getLarge()
                        this.forceLargeMedia = true
                        this.showAboveText = true
                    }
                }
            },
        )

        BgmUtil.handleOgImageInfo(infoBox) { title, url, i ->
            resultList.add(InputInlineQueryResultArticle().apply {
                this.id = "P${person.id}_$i"
                this.title = title
                this.inputMessageContent = InputMessageText().apply {
                    this.text = formattedText
                    this.linkPreviewOptions = LinkPreviewOptions().apply {
                        this.url = url
                        this.forceLargeMedia = true
                        this.showAboveText = true
                    }
                }
            })
        }

        return resultList.toTypedArray()
    }

}
