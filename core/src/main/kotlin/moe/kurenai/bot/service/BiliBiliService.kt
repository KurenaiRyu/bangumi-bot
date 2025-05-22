package moe.kurenai.bot.service

import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.model.bilibili.DynamicInfo
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
internal object BiliBiliService {

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0"

    private val log = getLogger()

    private val httpLogger = object : Logger {
        override fun log(message: String) {
            log.info(message)
        }
    }

    private val dontRedirectClient = HttpClient(OkHttp) {
        followRedirects = false
        defaultRequest {
            header(HttpHeaders.UserAgent, USER_AGENT)
        }
    }
    private val client = HttpClient(OkHttp) {
        install(Logging) {
            logger = httpLogger
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Cookie }
        }
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Cookie, CONFIG.bilibili.cookie)
        }
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

    suspend fun getRedirectUrl(uri: URI): Url {
        val response = dontRedirectClient.get(Url(uri))
        val redirectUrl = (response.headers[HttpHeaders.Location]?: let {
            val doc = Jsoup.parse(response.bodyAsText())
            doc.select("a").attr("href")
        }).substringBefore('?')
        log.info("Get redirect url: $redirectUrl")
        return Url(redirectUrl)
    }

    fun getIdAndPByShortLink(uri: URI, redirectUrl: Url): Triple<String, Int, Float> {
        val segments = redirectUrl.rawSegments
        if (segments.isEmpty()) error("Get short link error, origin: ${uri}, redirect: ${redirectUrl}, segments: $segments")
        val p = redirectUrl.parameters["p"]?.toInt() ?: 0
        val t = redirectUrl.parameters["t"]?.toFloat() ?: 0F
        val id = redirectUrl.rawSegments.last().takeIf { it.isNotBlank() } ?: segments[segments.lastIndex - 1]
        return Triple(id, p, t)
    }

    suspend fun getPlayUrl(bvid: String, cid: Long) = urlCache.get(bvid) { _ ->
        client.get("https://api.bilibili.com/x/player/playurl") {
            parameter("bvid", bvid)
            parameter("cid", cid)
            parameter("platform", "html5")
        }.body()
    }

    suspend fun getVideoInfo(id: String) = cache.get(id) { _ ->
        client.get("https://api.bilibili.com/x/web-interface/view") {
            if (id.startsWith("av", true)) {
                parameter("aid", id.substring(2))
            } else {
                parameter("bvid", id)
            }
        }.body()
    }

    suspend fun getDynamicDetail(id: String): DynamicInfo {
        val jsonObject = client.get("https://api.bilibili.com/x/polymer/web-dynamic/v1/detail") {
            parameter("timezone_offset", "-480")
            parameter("id", id)
            parameter("features", "itemOpusStyle")
        }.body<JsonElement>().jsonObject
        return if (jsonObject["code"]?.jsonPrimitive?.intOrNull == 0) {
            json.decodeFromJsonElement(DynamicInfo.serializer(), jsonObject)
        } else {
            error("Return code ${jsonObject["code"]?.jsonPrimitive?.intOrNull}")
        }
    }

}
