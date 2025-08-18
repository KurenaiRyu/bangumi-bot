package moe.kurenai.bot.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.asCache
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.common.util.MimeTypes
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import org.jsoup.Jsoup
import java.net.URI
import java.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
internal object SakugabooruService {

    val client = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Cookie }
        }

    }

    val cacheStats = ConcurrentStatsCounter()
    private val cache = Caffeine.newBuilder()
        .maximumSize(50)
        .expireAfterWrite(Duration.ofDays(7))
        .expireAfterAccess(Duration.ofDays(1))
        .recordStats { cacheStats }
        .asCache<URI, InputInlineQueryResult>()

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
