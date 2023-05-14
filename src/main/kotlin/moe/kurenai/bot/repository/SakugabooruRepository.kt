package moe.kurenai.bot.repository

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bot.util.MimeTypes
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import org.jsoup.Jsoup
import java.net.URI
import kotlin.time.Duration.Companion.days

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
object SakugabooruRepository {

    val client = HttpClient()

    val cacheStats = ConcurrentStatsCounter()
    private val cache = caffeineBuilder<URI, InputInlineQueryResult> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = cacheStats
    }.build()

    suspend fun findOne(id: String, uri: URI): InputInlineQueryResult {
        return cache.get(uri) { k ->
            client.get(k.toURL()).bodyAsText().let { html ->
                val doc = Jsoup.parse(html)
                val url = doc.select("video source").attr("src")
                if (url.isBlank()) throw IllegalStateException("Cannot found video url")
                val artist = doc.select(".tag-type-artist")
                    .joinToString("\n") {
                        " \\- [${it.child(1).text().markdown()}](${uri.host}${
                            it.child(1).attr("href")
                        }) ${it.child(2).text()}"
                    }
                val copyright = doc.select(".tag-type-copyright")
                    .joinToString("\n") {
                        " \\- [${it.child(1).text().markdown()}](${uri.host}${
                            it.child(1).attr("href")
                        }) ${it.child(2).text()}"
                    }

                InputInlineQueryResultVideo().apply {

                    this.id = "SAKUGA-POST-$id"
                    title = id
                    videoUrl = url
                    mimeType = MimeTypes.Video.MP4
                    thumbnailUrl = "https://www.sakugabooru.com/data/preview/" + url.substringAfterLast('/')
                        .substringBeforeLast('.') + ".jpg"
                    inputMessageContent = InputMessageVideo().apply {
                        this.caption = "[$id](${uri})\nArtist\n$artist\nCopyright\n$copyright".fmt()
                    }
                }
            }
        }
    }

}
