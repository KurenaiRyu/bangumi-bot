package moe.kurenai.bot.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
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
import it.tdlight.jni.TdApi.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.command.InlineDispatcher
import moe.kurenai.bot.model.bilibili.DynamicInfo
import moe.kurenai.bot.model.bilibili.VideoInfo
import moe.kurenai.bot.model.bilibili.VideoStreamUrl
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.HttpUtil.DYNAMIC_USER_AGENT
import moe.kurenai.bot.util.TelegramUtil.trimCaption
import moe.kurenai.bot.util.TelegramUtil.trimMessage
import moe.kurenai.common.util.*
import org.jsoup.Jsoup
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
internal object BiliBiliService {

    private val log = getLogger()

    private val httpLogger = object : Logger {
        override fun log(message: String) {
            log.debug(message)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val hexFormat = HexFormat{
        number {
            removeLeadingZeros = true
        }
    }

    private val dontRedirectClient = HttpClient(OkHttp) {
        followRedirects = false
        install(DYNAMIC_USER_AGENT)
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
            header(HttpHeaders.Cookie, CONFIG.bilibili.cookie)
        }
        install(DYNAMIC_USER_AGENT)
    }

    private val cache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(Duration.ofMinutes(5))
        .asCache<String, VideoInfo>()

    suspend fun getRedirectUrl(uri: URI): Url {
        val response = dontRedirectClient.get(Url(uri))
        val redirectUrl = (response.headers[HttpHeaders.Location]?: let {
            val doc = Jsoup.parse(response.bodyAsText())
            doc.select("a").attr("href")
        }).substringBefore('?')
        log.info("Get redirect url: $redirectUrl")
        return Url(redirectUrl)
    }

    fun getIdPartNumAndTime(uri: URI, redirectUrl: Url): Triple<String, Int, Float> {
        val segments = redirectUrl.rawSegments
        if (segments.isEmpty()) error("Get short link error, origin: ${uri}, redirect: ${redirectUrl}, segments: $segments")
        val p = redirectUrl.parameters["p"]?.toInt() ?: 0
        val t = redirectUrl.parameters["t"]?.toFloat() ?: 0F
        val id = redirectUrl.rawSegments.last().takeIf { it.isNotBlank() } ?: segments[segments.lastIndex - 1]
        return Triple(id, p, t)
    }

    suspend fun getPlayUrl(bvid: String, cid: Long): VideoStreamUrl {
        return client.get("https://api.bilibili.com/x/player/playurl") {
            parameter("bvid", bvid)
            parameter("cid", cid)
            parameter("platform", "html5")
        }.body()
    }

    suspend fun getVideoInfo(id: String): VideoInfo = cache.get(id) { _ ->
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

    suspend fun fetchStreamLength(url: String): Long {
        return client.head(url).headers[HttpHeaders.ContentLength]?.toLongOrNull()?:-1
    }


    @OptIn(ExperimentalStdlibApi::class)
    suspend fun handleVideo(id: String, p: Int, t: Float): Array<InputInlineQueryResult> {

        val videoInfo = getVideoInfo(id)

        val playCount = "${((videoInfo.data.stat.view / 10.0).roundToInt() / 100.0)}K 播放"
        val videoTitle = videoInfo.data.title.trim()
        val linkWithoutPage = "https://www.bilibili.com/video/${videoInfo.data.bvid}"

        val parameters = mutableListOf<String>()
        if (p > 0) parameters.add("p=$p")
        if (t > 0) parameters.add("t=$t")
        val parameterStr = parameters.joinToString("&")

        val rank =
            if (videoInfo.data.stat.nowRank == 0) "" else "/ ${videoInfo.data.stat.nowRank} 名 / 历史最高 ${videoInfo.data.stat.nowRank} 名"
        val createDate = LocalDateTime.ofEpochSecond(videoInfo.data.pubdate.toLong(), 0, ZoneOffset.ofHours(8))
            .format(InlineDispatcher.DATE_TIME_PATTERN)

        val results = ArrayList<InputInlineQueryResult>()
        for ((index, page) in videoInfo.data.pages.withIndex()) {
            val pageNum = index + 1
            if (p > 0 && p != pageNum) continue // 指定分P则只处理对应分P
            else if (t > 0 && pageNum != 1) continue  // 未指定分P的空降应该是空降在1P

            val streamInfo = getPlayUrl(videoInfo.data.bvid, page.cid)
            val pageTitle = page.part.trim()
            val duration = page.duration.seconds.formatToSeparateUnit()

            val builder = FormattedTextBuilder()

            val inlineTitle: String
            if (p == 0 && videoInfo.data.pages.size == 1) { // no specific page
                inlineTitle = videoTitle
                builder.appendLink(videoTitle, "$linkWithoutPage?$parameterStr")
            } else  {
                val pTitle = if (videoTitle.trim() == pageTitle.trim()) {
                    "P$p"
                } else {
                    removeOverlap(pageTitle, videoTitle).trim()
                }
                builder.appendLink(videoTitle, linkWithoutPage)
                    .appendText(" / ")
                    .appendLink(pTitle, "$linkWithoutPage?p=$pageNum&t=$t")

                inlineTitle = "${pTitle}_$videoTitle"
            }

            if (t > 0) {
                val timeStr = (t * 1000).toLong().milliseconds.formatToTime()
                builder.appendText(" / 跳转到 $timeStr")
            }

            val formattedText = builder
                .appendLine().appendLine()
                .appendText("UP: ")
                .appendLink(videoInfo.data.owner.name, "https://space.bilibili.com/${videoInfo.data.owner.mid}")
                .appendText(" / $playCount $rank / $duration")
                .appendLine()
                .appendText(createDate)
                .appendLine().appendLine()
                .wrapQuoteIfNeeded {
                    appendText(videoInfo.data.desc)
                }.build()

            val canShowVideo = fetchStreamLength(streamInfo.data!!.durl.first().url) in 1..12*1024*1024
            val id = "${videoInfo.data.bvid.substring(2)}${page.cid.toHexString(hexFormat)}"
            results.add(
                InputInlineQueryResultArticle().apply {
                    this.id = "A$id"
                    this.title = inlineTitle
                    this.description = "With Photo"
                    thumbnailUrl = videoInfo.data.pic
                    inputMessageContent = InputMessageText().apply {
                        this.text = formattedText.trimCaption()
                        this.linkPreviewOptions = LinkPreviewOptions().apply {
                            this.url = videoInfo.data.pic
                            this.showAboveText = true
                            this.forceLargeMedia = true
                        }
                    }
                })
            results.add(
                InputInlineQueryResultVideo().apply {
                    this.id = "V$id"
                    this.title = inlineTitle
                    this.description = "With Video"
                    if (!canShowVideo) this.description += " (May not be able to show)"
                    videoUrl = streamInfo.data.durl.first().url
                    thumbnailUrl = videoInfo.data.pic
                    mimeType = MimeTypes.Video.MP4
                    this.videoDuration = page.duration
                    this.videoWidth = page.dimension.width
                    this.videoHeight = page.dimension.height
                    inputMessageContent = InputMessageVideo().apply {
                        this.caption = formattedText.trimCaption()
                    }
                })
        }
        return results.toTypedArray()
    }

    suspend fun handleDynamic(id: String): Array<InputInlineQueryResultArticle> {
        val info = getDynamicDetail(id)
        val moduleDynamic = info.data.item.modules.moduleDynamic

        val content = moduleDynamic.major?.opus?.summary?.text ?: moduleDynamic.desc?.text ?: ""
        val pubTime = LocalDateTime.ofEpochSecond(info.data.item.modules.moduleAuthor.pubTs, 0, ZoneOffset.ofHours(8))
            .format(InlineDispatcher.DATE_TIME_PATTERN)

        val builder = FormattedTextBuilder()
        builder.appendBold(info.data.item.modules.moduleAuthor.name)
            .appendText(" - ${pubTime}:\n\n")
            .wrapQuoteIfNeeded {
                appendText(content)
            }

        info.data.item.orig?.let { orig ->
            val quoteContent = orig.modules.moduleDynamic.major?.opus?.summary?.text ?: ""
            val pubTime = LocalDateTime.ofEpochSecond(orig.modules.moduleAuthor.pubTs, 0, ZoneOffset.ofHours(8))
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            builder.wrapQuote {
                appendBold(orig.modules.moduleAuthor.name)
                appendText(" - ${pubTime}:\n\n$quoteContent\n\nhttps://t.bilibili.com/${orig.idStr}")
            }
        }
        val formattedText = builder.appendText("\nhttps://t.bilibili.com/${id}").build()

        return moduleDynamic.major?.opus?.pics?.mapIndexed { index, pic ->
            InputInlineQueryResultArticle().apply {
                this.id = index.toString()
                title =
                    "${info.data.item.modules.moduleAuthor.name} ${info.data.item.modules.moduleAuthor.pubTime}[$index]"
                thumbnailUrl = pic.url + "@240w_!web-dynamic.webp"
                inputMessageContent = InputMessageText().apply {
                    this.text = formattedText.trimMessage()
                    this.linkPreviewOptions = LinkPreviewOptions().apply {
                        this.url = pic.url + "@1920w_!web-dynamic.webp"
                        this.forceLargeMedia = true
                        this.showAboveText = true
                    }
                }
            }
        }?.toTypedArray()?.takeIf { it.isNotEmpty() }
            ?: arrayOf(InputInlineQueryResultArticle().apply {
                this.id = "dynamic$id"
                this.title =
                    "${info.data.item.modules.moduleAuthor.name} ${info.data.item.modules.moduleAuthor.pubTime}"
                this.inputMessageContent = InputMessageText().apply {
                    this.text = formattedText.trimMessage()
                }
            })
    }

}
