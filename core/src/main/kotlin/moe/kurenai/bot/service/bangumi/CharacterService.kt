package moe.kurenai.bot.service.bangumi

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bangumi.models.Character
import moe.kurenai.bangumi.models.CharacterPerson
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
internal object CharacterService {

    context(token: UserAccessToken?)
    suspend fun findById(id: Int): Character {
        return useApi {
            it.getCharacterById(id).result()
        }
    }

    context(token: UserAccessToken?)
    suspend fun findPersons(id: Int): List<CharacterPerson> {
        return useApi {
            it.getRelatedPersonsByCharacterId(id).result()
        }
    }

    suspend fun getContent(
        character: Character,
        link: String,
        persons: List<CharacterPerson>? = null
    ): Array<InputInlineQueryResult> {
        val host = Url(link).hostWithPortIfSpecified
        val builder = FormattedTextBuilder()

        val infoBox = character.infobox?.formatToList() ?: emptyList()
        builder.appendLink(character.name, link)
            .appendLine().appendLine()
            .appendFormattedInfoBox(infoBox)



        persons?.let {
            builder.wrapQuote {
                appendBold("关联人物")
                appendLine()
                appendLine()

                joinList(persons.groupBy { it.name }.entries, "\n\n") { (name, list) ->
                    appendBold(name)
                    appendText(": ")
                    joinList(list, "、") {
                        appendLink(it.subjectName, "https://$host/subject/${it.subjectId}")
                    }
                }
            }
        }

        builder.appendQuote(character.summary)
        builder.appendText(" ")

        val formattedText = builder.build().trimMessage()

        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                id = "C${character.id}"
                thumbnailUrl = character.images?.grid?.toGrid()
                this.title = character.name
                this.inputMessageContent = InputMessageText().apply {
                    this.text = formattedText
                    this.linkPreviewOptions = LinkPreviewOptions().apply {
                        this.url = character.images.getLarge()
                        this.forceLargeMedia = true
                        this.showAboveText = true
                    }
                }
            },
        )

        BgmUtil.handleOgImageInfo(infoBox) { title, url, i ->
            resultList.add(InputInlineQueryResultArticle().apply {
                id = "C${character.id}_$i"
                thumbnailUrl = url
                this.title = title.ifBlank { character.name }
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
