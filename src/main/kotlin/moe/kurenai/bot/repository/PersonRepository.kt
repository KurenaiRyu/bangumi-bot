package moe.kurenai.bot.repository

import com.elbekd.bot.types.*
import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import moe.kurenai.bgm.model.person.PersonDetail
import moe.kurenai.bgm.model.subject.getLarge
import moe.kurenai.bgm.model.subject.getSmall
import moe.kurenai.bgm.request.person.GetPersonDetail
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
object PersonRepository {

    val cacheStats = ConcurrentStatsCounter()
    private val cache = caffeineBuilder<Int, PersonDetail> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = cacheStats
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

    suspend fun getContent(person: PersonDetail, link: String): List<InlineQueryResult> {
        val title = person.name
        val infoBox = person.infobox?.formatInfoBoxToList() ?: emptyList()

        val entities = listOf(
            MessageEntity(MessageEntity.Type.TEXT_LINK, 0, person.name.length, url = link),
        )

        val message = listOfNotNull(title, infoBox.formatInfoBox()).joinToString("\n\n")
        val default = InlineQueryResultPhoto(
            "P${person.id} - img",
            photoUrl = person.images.getLarge(),
            thumbUrl = person.images.getSmall(),
            title = person.name,
            caption = message,
            captionEntities = entities,
        )

        val resultList = mutableListOf(
            InlineQueryResultArticle(
                "P${person.id} - text",
                thumbUrl = person.images.getLarge().toGrid(),
                title = person.name,
                inputMessageContent = InputTextMessageContent(
                    messageText = " $message",
                    entities = listOf(
                        MessageEntity(MessageEntity.Type.TEXT_LINK, 0, 1, url = person.images.getLarge()),
                        MessageEntity(MessageEntity.Type.TEXT_LINK, 0, person.name.length, url = link)
                    )
                )
            ),
            default
        )
        infoBox.filter { it.second.startsWith("http") }.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }.forEachIndexed { i, url ->
            resultList.add(default.copy(id = "P${person.id} - ${i + 1}", photoUrl = url, thumbUrl = url))
        }
        return resultList
    }

}
