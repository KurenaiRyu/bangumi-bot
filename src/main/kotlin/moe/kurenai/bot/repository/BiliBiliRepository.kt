package moe.kurenai.bot.repository

import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import moe.kurenai.bot.model.bilibili.VideoInfo
import moe.kurenai.bot.model.bilibili.VideoStreamUrl
import moe.kurenai.bot.util.getLogger
import moe.kurenai.bot.util.json
import org.jsoup.Jsoup
import java.net.URI
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
object BiliBiliRepository {

    private val log = getLogger()
    private val client = HttpClient()
    private val dontRedirectClient = HttpClient {
        followRedirects = false
    }

    private val cache = caffeineBuilder<String, VideoInfo> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterWrite = 7.days
    }.build()

    private val urlCache = caffeineBuilder<String, VideoStreamUrl> {
        maximumSize = 200
        expireAfterWrite = 100.minutes
    }.build()

    suspend fun getIdAndPByShortLink(uri: URI): Pair<String, Int> {
        val doc = Jsoup.parse(dontRedirectClient.get(Url(uri)).bodyAsText())
        val redirectUrl = Url(doc.select("a").attr("href"))
        val segments = redirectUrl.pathSegments
        if (segments.isEmpty()) error("Get short link error, origin: ${uri}, redirect: ${redirectUrl}, segments: $segments")
        val p = redirectUrl.parameters["p"]?.toInt() ?: 1
        val id = redirectUrl.pathSegments.last().takeIf { it.isNotBlank() } ?: segments[segments.lastIndex - 1]
        return id to p
    }

    suspend fun getPlayUrl(bvid: String, cid: Int) = urlCache.get(bvid) { _ ->
        json.decodeFromString(
            VideoStreamUrl.serializer(),
            client.get("https://api.bilibili.com/x/player/playurl") {
                parameter("bvid", bvid)
                parameter("cid", cid)
                parameter("platform", "html5")
            }.bodyAsText().also {
                log.debug("url: $it")
            }
        )
    }

    suspend fun getVideoInfo(id: String) = cache.get(id) { _ ->
        json.decodeFromString(
            VideoInfo.serializer(),
            client.get("https://api.bilibili.com/x/web-interface/view") {
                if (id.startsWith("av", true)) {
                    parameter("aid", id)
                } else {
                    parameter("bvid", id)
                }
            }.bodyAsText().also {
                log.debug("info: $it")
            }
        )
    }

}
