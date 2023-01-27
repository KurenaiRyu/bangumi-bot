package moe.kurenai.bot.repository

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import moe.kurenai.bgm.model.subject.Subject
import moe.kurenai.bgm.model.subject.getLarge
import moe.kurenai.bgm.request.subject.GetSubject
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.util.BgmUtil.category
import moe.kurenai.bot.util.BgmUtil.formatInfoBox
import moe.kurenai.tdlight.model.MessageEntityType
import moe.kurenai.tdlight.model.message.MessageEntity
import kotlin.time.Duration.Companion.days

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
object SubjectRepository {

    private val cache = caffeineBuilder<Int, Subject> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = ConcurrentStatsCounter()
    }.build()

    suspend fun findById(id: Int, token: String? = null): Subject {
        return cache.get(id) { k ->
            BangumiBot.bgmClient.send(GetSubject(k).apply { this.token = token })
        }
    }

    suspend fun findByIds(ids: Collection<Int>, token: String? = null): Collection<Subject> {
        return cache.getAll(ids) { keys ->
            keys.map { k ->
                CoroutineScope(Dispatchers.IO).async {
                    BangumiBot.bgmClient.send(GetSubject(k).apply { this.token = token })
                }
            }.associate {
                val subject = it.await()
                subject.id to subject
            }
        }.values
    }

    fun getContent(sub: Subject, link: String): Pair<String, List<MessageEntity>> {
        val title = " [${sub.type.category()}]　${sub.name}"
        val infoBox = sub.infobox?.formatInfoBox() ?: ""

        val titleIndex = sub.type.category().length + 4
        val entities = listOf(
            MessageEntity(MessageEntityType.TEXT_LINK, 0, 1).apply { url = sub.images.getLarge() },
            MessageEntity(MessageEntityType.TEXT_LINK, titleIndex, sub.name.length).apply { url = link },
        )

        return listOfNotNull(title, infoBox).joinToString("\n\n") to entities
    }

    fun getSimpleContent(sub: Subject, link: String): Pair<String, List<MessageEntity>> {
        val title = " [${sub.type.category()}]　${sub.name}"

        val titleIndex = sub.type.category().length + 4
        val entities = listOf(
            MessageEntity(MessageEntityType.TEXT_LINK, 0, 1).apply { url = sub.images.getLarge() },
            MessageEntity(MessageEntityType.TEXT_LINK, titleIndex, sub.name.length).apply { url = link },
        )

        return listOfNotNull(title, sub.summary).joinToString("\n\n") to entities
    }

}
