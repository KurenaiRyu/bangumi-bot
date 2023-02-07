package moe.kurenai.bot.repository

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
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
import moe.kurenai.tdlight.model.MessageEntityType
import moe.kurenai.tdlight.model.message.MessageEntity
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

    fun getContent(character: CharacterDetail, link: String, persons: List<CharacterPerson>? = null): Pair<String, List<MessageEntity>> {
        val title = " ${character.name}"
        var content = character.infobox?.formatInfoBox() ?: ""
        persons?.let {
            val personStr = persons.joinToString("\n") {
                "${it.subjectName}: ${it.name}"
            }
            content = "$content\n\n$personStr"
        }

        val entities = listOf(
            MessageEntity(MessageEntityType.TEXT_LINK, 0, 1).apply { url = character.images.getLarge() },
            MessageEntity(MessageEntityType.TEXT_LINK, 1, character.name.length).apply { url = link },
        )
        return listOfNotNull(title, content).joinToString("\n\n") to entities
    }

}
