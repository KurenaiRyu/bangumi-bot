package moe.kurenai.bot.repository

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import moe.kurenai.tdlight.model.ParseMode
import moe.kurenai.tdlight.model.inline.InlineQueryResult
import moe.kurenai.tdlight.model.inline.InlineQueryResultVideo
import moe.kurenai.tdlight.model.inline.MIMEType
import moe.kurenai.tdlight.util.MarkdownUtil.fm2md
import org.jsoup.Jsoup
import java.net.URI
import kotlin.time.Duration.Companion.days

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
object SakugabooruRepository {

    val client = HttpClient()

    private val cache = caffeineBuilder<URI, InlineQueryResult> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = ConcurrentStatsCounter()
    }.build()

    suspend fun findOne(id: String, uri: URI): InlineQueryResult {
        return cache.get(uri) { k ->
            client.get(k.toURL()).bodyAsText().let { html ->
                val doc = Jsoup.parse(html)
                val url = doc.select("video source").attr("src")
                if (url.isBlank()) throw IllegalStateException("Cannot found video url")
                val artist = doc.select(".tag-type-artist")
                    .joinToString("\n") { " \\- [${it.child(1).text().fm2md()}](${uri.host}${it.child(1).attr("href")}) ${it.child(2).text()}" }
                val copyright = doc.select(".tag-type-copyright")
                    .joinToString("\n") { " \\- [${it.child(1).text().fm2md()}](${uri.host}${it.child(1).attr("href")}) ${it.child(2).text()}" }

                InlineQueryResultVideo("SAKUGA-POST-$id", id).apply {
                    this.videoUrl = url
                    this.mimeType = MIMEType.MP4
                    this.thumbUrl = "https://www.sakugabooru.com/data/preview/" + url.substringAfterLast('/').substringBeforeLast('.') + ".jpg"
                    this.caption = "[$id](${uri})\nArtist\n$artist\nCopyright\n$copyright"
                    this.parseMode = ParseMode.MARKDOWN_V2
                }
            }
        }
    }

}
