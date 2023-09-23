package moe.kurenai.bot.repository

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import moe.kurenai.bgm.model.SubjectType
import moe.kurenai.bgm.model.subject.Subject
import moe.kurenai.bgm.model.subject.getLarge
import moe.kurenai.bgm.request.subject.GetSubject
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.TelegramUserBot
import moe.kurenai.bot.util.BgmUtil.category
import moe.kurenai.bot.util.BgmUtil.formatToList
import moe.kurenai.bot.util.BgmUtil.toGrid
import moe.kurenai.bot.util.HttpUtil
import kotlin.time.Duration.Companion.days

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
object SubjectRepository {
    val cacheStats = ConcurrentStatsCounter()
    private val cache = caffeineBuilder<Int, Subject> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = cacheStats
    }.build()

    private val mainInfoProperties =
        listOf("中文名", "话数", "放送开始", "原作", "导演", "音乐", "人物设定", "系列构成", "总作画监督", "製作", "动画制作", "别名", "官方网站", "Copyright")

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

    suspend fun getContent(sub: Subject, link: String): Array<InputInlineQueryResult> {

        val title = "[${sub.type.category()}]　${sub.name}"
        val infoBox = sub.infobox?.formatToList() ?: emptyList()
        val simpleInfoBot =
            if (sub.type == SubjectType.ANIME) infoBox.filter { mainInfoProperties.contains(it.first) } else infoBox
        val content = simpleInfoBot.joinToString("\n") { (k, v) ->
            "$k: $v"
        }

        val titleIndex = sub.type.category().length + 3
        val entities = arrayOf(TextEntity(titleIndex, sub.name.length, TextEntityTypeTextUrl(link)))
        val caption = listOfNotNull(title, content).joinToString("\n\n")
        val formattedText = FormattedText(caption, entities)

        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                this.id = "S${sub.id} - txt"
                this.thumbnailUrl = sub.images.getLarge().toGrid()
                this.title = sub.name + "(${sub.nameCn})"
                inputMessageContent = InputMessageText().apply {
                    this.text = FormattedText(
                        " $caption", arrayOf(
                            TextEntity(0, 1, TextEntityTypeTextUrl(sub.images.getLarge())),
                            TextEntity(titleIndex + 1, sub.name.length, TextEntityTypeTextUrl(link))
                        )
                    )
                }
            },
            InputInlineQueryResultPhoto().apply {
                this.id = "S${sub.id} - img"
                this.title = sub.name
                this.photoUrl = sub.images.getLarge().also {
                    TelegramUserBot.fetchRemoteFileId(it)
                }
                this.thumbnailUrl = sub.images.getLarge()
                this.inputMessageContent = InputMessagePhoto().apply {
                    this.caption = formattedText
                }
            }
        )

        infoBox.filter { it.second.startsWith("http") }.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }.forEachIndexed { i, url ->
            TelegramUserBot.fetchRemoteFileId(url)
            resultList.add(
                InputInlineQueryResultPhoto().apply {
                    this.id = "S${sub.id} - ${i + 1}"
                    this.title = sub.name
                    this.photoUrl = url
                    this.thumbnailUrl = url
                    this.inputMessageContent = InputMessagePhoto().apply {
                        this.caption = formattedText
                    }
                }
            )
        }
        return resultList.toTypedArray()
    }

}
