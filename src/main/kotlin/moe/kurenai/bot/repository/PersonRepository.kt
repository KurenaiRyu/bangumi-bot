package moe.kurenai.bot.repository

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import moe.kurenai.bgm.model.person.PersonDetail
import moe.kurenai.bgm.model.subject.getLarge
import moe.kurenai.bgm.model.subject.getSmall
import moe.kurenai.bgm.request.person.GetPersonDetail
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

    suspend fun getContent(person: PersonDetail, link: String): Array<InputInlineQueryResult> {
        val title = person.name
        val infoBox = person.infobox.formatToList()

        val entities = arrayOf(TextEntity(0, person.name.length, TextEntityTypeTextUrl(link)))
        val caption = listOfNotNull(title, infoBox.format()).joinToString("\n\n")
        val formattedText = FormattedText(caption, entities)
        val default = InputInlineQueryResultPhoto().apply {
            this.id = "P${person.id} - img"
            photoUrl = person.images.getLarge().also {
                TelegramUserBot.fetchRemoteFile(it)
            }
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
        infoBox.filter { it.second.startsWith("http") }.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }.forEachIndexed { i, url ->
            TelegramUserBot.fetchRemoteFile(url)
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
