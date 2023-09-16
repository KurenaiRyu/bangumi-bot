package moe.kurenai.bot.repository

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import moe.kurenai.bgm.model.character.CharacterDetail
import moe.kurenai.bgm.model.character.CharacterPerson
import moe.kurenai.bgm.model.subject.getLarge
import moe.kurenai.bgm.model.subject.getSmall
import moe.kurenai.bgm.request.charater.GetCharacterDetail
import moe.kurenai.bgm.request.charater.GetCharacterRelatedPersons
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.TelegramUserBot
import moe.kurenai.bot.util.BgmUtil.format
import moe.kurenai.bot.util.BgmUtil.formatToList
import moe.kurenai.bot.util.BgmUtil.toGrid
import moe.kurenai.bot.util.HttpUtil
import kotlin.time.Duration.Companion.days

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
object CharacterRepository {

    val cacheStats = ConcurrentStatsCounter()
    private val cache = caffeineBuilder<Int, CharacterDetail> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = cacheStats
    }.build()

    private val personCache = caffeineBuilder<Int, List<CharacterPerson>> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = cacheStats
    }.build()

    suspend fun findById(id: Int, token: String? = null): CharacterDetail {
        return cache.get(id) { k ->
            BangumiBot.bgmClient.send(GetCharacterDetail(k).apply { this.token = token })
        }
    }

    suspend fun findPersons(id: Int, token: String? = null): List<CharacterPerson> {
        return personCache.get(id) { k ->
            BangumiBot.bgmClient.send(GetCharacterRelatedPersons(k).apply { this.token = token })
        }
    }

    suspend fun findByIds(ids: Collection<Int>, token: String? = null): Collection<CharacterDetail> {
        return cache.getAll(ids) { keys ->
            keys.map { k ->
                CoroutineScope(Dispatchers.IO).async {
                    BangumiBot.bgmClient.send(GetCharacterDetail(k).apply { this.token = token })
                }
            }.associate {
                val subject = it.await()
                subject.id to subject
            }
        }.values
    }

    suspend fun getContent(
        character: CharacterDetail,
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
                thumbnailUrl = character.images.getLarge().toGrid()
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
                    TelegramUserBot.fetchRemoteFile(it)
                }
                thumbnailUrl = character.images.getSmall()
                this.title = character.name
                this.inputMessageContent = InputMessageText().apply {
                    this.text = formattedText
                }
            },
        )
        infoBox.filter { it.second.startsWith("http") }.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }.forEachIndexed { i, url ->
            TelegramUserBot.fetchRemoteFile(url)
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
