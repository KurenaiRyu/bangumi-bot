package moe.kurenai.bot.repository

import com.elbekd.bot.types.*
import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import moe.kurenai.bgm.model.character.CharacterDetail
import moe.kurenai.bgm.model.character.CharacterPerson
import moe.kurenai.bgm.model.subject.getLarge
import moe.kurenai.bgm.request.charater.GetCharacterDetail
import moe.kurenai.bgm.request.charater.GetCharacterRelatedPersons
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.util.BgmUtil.formatInfoBox
import moe.kurenai.bot.util.BgmUtil.formatInfoBoxToList
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

    suspend fun getContent(character: CharacterDetail, link: String, persons: List<CharacterPerson>? = null): List<InlineQueryResult> {
        val title = character.name
        val infoBox = character.infobox?.formatInfoBoxToList() ?: emptyList()
        var content = infoBox.formatInfoBox()
        persons?.let {
            val personStr = persons.joinToString("\n") {
                "${it.subjectName}: ${it.name}"
            }
            content = "$content\n\n$personStr"
        }

        val entities = listOf(
            MessageEntity(MessageEntity.Type.TEXT_LINK, 0, character.name.length, url = link),
        )

        val message = listOfNotNull(title, infoBox.formatInfoBox()).joinToString("\n\n")

        val defaultResult = InlineQueryResultPhoto(
            "C${character.id} - img",
            photoUrl = character.images.getLarge(),
            thumbUrl = character.images.getLarge(),
            title = character.name,
            caption = message,
            captionEntities = entities,
        )

        val resultList = mutableListOf(
            InlineQueryResultArticle(
                "C${character.id} - txt",
                thumbUrl = character.images.getLarge().toGrid(),
                title = character.name,
                inputMessageContent = InputTextMessageContent(
                    messageText = " $message",
                    entities = listOf(
                        MessageEntity(MessageEntity.Type.TEXT_LINK, 0, 1, url = character.images.getLarge()),
                        MessageEntity(MessageEntity.Type.TEXT_LINK, 1, character.name.length, url = link)
                    )
                )
            ),
            defaultResult,
        )
        infoBox.filter { it.second.startsWith("http") }.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }.forEachIndexed { i, url ->
            resultList.add(defaultResult.copy(id = "C${character.id} - ${i + 1}", photoUrl = url, thumbUrl = url))
        }

        return resultList
    }

}
