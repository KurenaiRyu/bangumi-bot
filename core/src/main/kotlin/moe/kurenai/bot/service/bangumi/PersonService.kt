package moe.kurenai.bot.service.bangumi

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bangumi.models.PersonDetail
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi
import moe.kurenai.bot.util.BgmUtil.format
import moe.kurenai.bot.util.BgmUtil.formatToList
import moe.kurenai.bot.util.BgmUtil.getLarge
import moe.kurenai.bot.util.BgmUtil.getSmall
import moe.kurenai.bot.util.BgmUtil.toGrid
import moe.kurenai.bot.util.HttpUtil

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
        val title = person.name
        val infoBox = person.infobox?.formatToList()

        val entities = arrayOf(TextEntity(0, person.name.length, TextEntityTypeTextUrl(link)))
        val caption = listOfNotNull(title, infoBox?.format()).joinToString("\n\n")
        val formattedText = FormattedText(caption, entities)
        val default = InputInlineQueryResultPhoto().apply {
            this.id = "P${person.id} - img"
            photoUrl = person.images.getLarge()
            thumbnailUrl = person.images.getSmall()
            this.title = person.name
            this.inputMessageContent = InputMessagePhoto().apply {
                this.caption = formattedText
            }
        }

        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                val url = person.images.getLarge().toGrid()
                this.id = "P${person.id} - text"
                this.url = url
                thumbnailUrl = url
                this.title = person.name
                this.inputMessageContent = InputMessageText().apply {
                    text = FormattedText(
                        " $caption", arrayOf(
                            TextEntity(0, 1, TextEntityTypeTextUrl(person.images.getLarge())),
                            TextEntity(1, person.name.length, TextEntityTypeTextUrl(link))
                        )
                    )
                }
            },
            default
        )
        infoBox?.filter { it.second.startsWith("http") }?.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }?.forEachIndexed { i, url ->
            resultList.add(InputInlineQueryResultPhoto().apply {
                this.id = "P${person.id} - ${i + 1}"
                photoUrl = url
                thumbnailUrl = url
                this.title = person.name
                this.inputMessageContent = InputMessagePhoto().apply {
                    this.caption = formattedText
                }
            })
        }
        return resultList.toTypedArray()
    }

}
