package moe.kurenai.bot.service.bangumi

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bangumi.models.Character
import moe.kurenai.bangumi.models.CharacterPerson
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi
import moe.kurenai.bot.util.BgmUtil.appendInfoBox
import moe.kurenai.bot.util.BgmUtil.formatToList
import moe.kurenai.bot.util.BgmUtil.getLarge
import moe.kurenai.bot.util.BgmUtil.getSmall
import moe.kurenai.bot.util.BgmUtil.toGrid
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.HttpUtil
import moe.kurenai.bot.util.TelegramUtil.trimCaption
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
        val builder = FormattedTextBuilder()

        builder.appendLink(character.name, link)
        builder.appendLine().appendLine()

        if (character.infobox?.isNotEmpty()?:true) {
            builder.appendInfoBox(character.infobox)
            builder.appendLine()
        }

        persons?.let {
            builder.wrapQuote {
                for ((name, list) in persons.groupBy { it.name }) {
                    builder.appendBold(name)
                    builder.appendText(": " + list.joinToString("、") { "「${it.subjectName}」" })
                    builder.appendLine()
                }
            }
            builder.appendLine()
        }

        builder.appendQuote(character.summary)
        builder.appendText(" ")

        val formattedText = builder.build()

        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                id = "TC${character.id}"
                thumbnailUrl = character.images?.grid?.toGrid()
                this.title = character.name
                this.inputMessageContent = InputMessageText().apply {
                    this.text = formattedText.trimMessage()
                    this.linkPreviewOptions = LinkPreviewOptions().apply {
                        this.url = character.images.getLarge()
                        this.forceLargeMedia = true
                        this.showAboveText = true
                    }
                }
            },
            InputInlineQueryResultPhoto().apply {
                id = "PC${character.id}"
                photoUrl = character.images.getLarge()
                thumbnailUrl = character.images.getSmall()
                this.title = character.name
                this.inputMessageContent = InputMessagePhoto().apply {
                    this.caption = formattedText.trimCaption()
                }
            },
        )

        val infoBox = character.infobox?.formatToList() ?: emptyList()
        infoBox.filter { it.second.startsWith("http") }.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }.forEachIndexed { i, url ->
            resultList.add(InputInlineQueryResultPhoto().apply {
                id = "C${character.id}_P${i + 1}"
                photoUrl = url
                thumbnailUrl = url
                this.title = character.name
                this.inputMessageContent = InputMessagePhoto().apply {
                    this.caption = formattedText
                }
            })
        }

        return resultList.toTypedArray()
    }

}
