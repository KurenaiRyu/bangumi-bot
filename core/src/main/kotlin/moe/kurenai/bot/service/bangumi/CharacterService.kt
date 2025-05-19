package moe.kurenai.bot.service.bangumi

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bangumi.models.Character
import moe.kurenai.bangumi.models.CharacterPerson
import moe.kurenai.bot.service.bangumi.BangumiApi.characterCache
import moe.kurenai.bot.service.bangumi.BangumiApi.characterPersonCache
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
internal object CharacterService {

    suspend fun findById(id: Int, token: String? = null): Character {
        return characterCache.get(id) { k ->
            useApi(token) {
                it.getCharacterById(k).result()
            }
        }
    }

    suspend fun findPersons(id: Int, token: String? = null): List<CharacterPerson> {
        return characterPersonCache.get(id) { k ->
            useApi(token) {
                it.getRelatedPersonsByCharacterId(k).result()
            }
        }
    }

//    suspend fun findByIds(ids: Collection<Int>, token: String? = null): Collection<CharacterDetail> {
//        return cache.getAll(ids) { keys ->
//            keys.map { k ->
//                CoroutineScope(Dispatchers.IO).async {
//                    BangumiBot.bgmClient.send(GetCharacterDetail(k).apply { this.token = token })
//                }
//            }.associate {
//                val subject = it.await()
//                subject.id to subject
//            }
//        }.values
//    }

    suspend fun getContent(
        character: Character,
        link: String,
        persons: List<CharacterPerson>? = null
    ): Array<InputInlineQueryResult> {
        val title = character.name
        val infoBox = character.infobox?.formatToList() ?: emptyList()
        var content = infoBox.format()
        persons?.let {
            val personStr = persons.joinToString("\n") {
                "${it.subjectName}: ${it.name}"
            }
            content = "$content\n\n$personStr"
        }

        val entities = arrayOf(TextEntity(0, character.name.length, TextEntityTypeTextUrl(link)))
        val message = listOfNotNull(title, infoBox.format()).joinToString("\n\n")
        val formattedText = FormattedText(message, entities)

        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                id = "C${character.id} - txt"
                thumbnailUrl = character.images?.grid?.toGrid()
                this.title = character.name
                this.inputMessageContent = InputMessageText().apply {
                    this.text = FormattedText(
                        " $message", arrayOf(
                            TextEntity(0, 1, TextEntityTypeTextUrl(character.images.getLarge())),
                            TextEntity(1, character.name.length, TextEntityTypeTextUrl(link)),
                        )
                    )
                }
            },
            InputInlineQueryResultPhoto().apply {
                id = "C${character.id} - img"
                photoUrl = character.images.getLarge().also {
//                    TelegramUserBot.fetchRemoteFileIdByUrl(it)
                }
                thumbnailUrl = character.images.getSmall()
                this.title = character.name
                this.inputMessageContent = InputMessagePhoto().apply {
                    this.caption = formattedText
                }
            },
        )
        infoBox.filter { it.second.startsWith("http") }.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }.forEachIndexed { i, url ->
//            TelegramUserBot.fetchRemoteFileIdByUrl(url)
            resultList.add(InputInlineQueryResultPhoto().apply {
                id = "C${character.id} - ${i + 1}"
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
