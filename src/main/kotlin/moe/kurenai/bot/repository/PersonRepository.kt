package moe.kurenai.bot.repository

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import moe.kurenai.bgm.model.person.PersonDetail
import moe.kurenai.bgm.model.subject.getLarge
import moe.kurenai.bgm.request.person.GetPersonDetail
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.util.BgmUtil.formatInfoBox
import moe.kurenai.tdlight.model.MessageEntityType
import moe.kurenai.tdlight.model.message.MessageEntity
import kotlin.time.Duration.Companion.days

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
object PersonRepository {

    private val cache = caffeineBuilder<Int, PersonDetail> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = ConcurrentStatsCounter()
    }.build()

    suspend fun findById(id: Int, token: String? = null): PersonDetail {
        return cache.get(id) { k ->
            BangumiBot.bgmClient.send(GetPersonDetail(k).apply { this.token = token })
        }
    }

    suspend fun findByIds(ids: Collection<Int>, token: String? = null): Collection<PersonDetail> {
        return cache.getAll(ids) { keys ->
            keys.map { k ->
                CoroutineScope(Dispatchers.IO).async {
                    BangumiBot.bgmClient.send(GetPersonDetail(k).apply { this.token = token })
                }
            }.associate {
                val subject = it.await()
                subject.id to subject
            }
        }.values
    }

    fun getContent(person: PersonDetail, link: String): Pair<String, List<MessageEntity>> {
        val title = " ${person.name}"
        val infoBox = person.infobox?.formatInfoBox() ?: ""

        val entities = listOf(
            MessageEntity(MessageEntityType.TEXT_LINK, 0, 1).apply { url = person.images.getLarge() },
            MessageEntity(MessageEntityType.TEXT_LINK, 1, person.name.length).apply { url = link },
        )

        return listOfNotNull(title, infoBox).joinToString("\n\n") to entities
    }

}
