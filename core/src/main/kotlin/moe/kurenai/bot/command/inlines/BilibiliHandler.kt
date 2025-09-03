package moe.kurenai.bot.command.inlines

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.*
import moe.kurenai.bot.command.InlineDispatcher.PUB_DATE_PATTERN
import moe.kurenai.bot.command.InlineDispatcher.fallback
import moe.kurenai.bot.service.BiliBiliService
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.TelegramUtil.answerInlineQuery
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import moe.kurenai.bot.util.TelegramUtil.trimCaption
import moe.kurenai.bot.util.TelegramUtil.trimMessage
import moe.kurenai.common.util.*
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object BilibiliHandler : InlineHandler {

    init {
        InlineDispatcher.registryHandler(this)
    }

    private val log = getLogger()
    @OptIn(ExperimentalStdlibApi::class)
    private val hexFormat = HexFormat{
        number {
            removeLeadingZeros = true
        }
    }

    override suspend fun handle(inlineQuery: UpdateNewInlineQuery, uri: URI): HandleResult {
        val host = uri.host
        if (CONFIG.bilibili.shortLinkHost.contains(host)) {
            handleShortLink(inlineQuery, uri)
            return HANDLED
        }

        when (host) {
            "www.bilibili.com" -> handleNormal(inlineQuery, uri)
            "t.bilibili.com" -> handleDynamic(inlineQuery, uri)
            else -> return UNHANDLED
        }

        return HANDLED
    }

    private suspend fun handleNormal(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle BiliBili")
        if (uri.path.contains("opus")) {
            handleDynamic(inlineQuery, uri)
            return
        }

        // https://www.bilibili.com/video/BV1Fx4y1w78G/?p=1
        val url = Url(uri)
        val segments = Url(uri).rawSegments
        val id = segments.last().takeIf { it.isNotBlank() } ?: segments[segments.lastIndex - 1]
        val p = url.parameters["p"]?.toInt() ?: 0
        val t = url.parameters["t"]?.toFloat() ?: 0F
        doHandle(inlineQuery, id, p, t)
    }

    private suspend fun handleShortLink(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle BiliBili short link")
        val redirectUrl = BiliBiliService.getRedirectUrl(uri)

        if (redirectUrl.rawSegments.contains("opus") || redirectUrl.host == "t.bilibili.com") {
            handleDynamic(inlineQuery, redirectUrl.toURI())
            return
        }

        val (id, p, t) = BiliBiliService.getIdAndPByShortLink(uri, redirectUrl)
        doHandle(inlineQuery, id, p, t)
    }

    private suspend fun handleDynamic(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle bilibili dynamic: $uri")
        val id = uri.path.substringAfterLast("/").takeIf { it.isNotBlank() } ?: run {
            fallback(inlineQuery)
            return
        }
        val info = BiliBiliService.getDynamicDetail(id)
        val moduleDynamic = info.data.item.modules.moduleDynamic

        val content = moduleDynamic.major?.opus?.summary?.text ?: moduleDynamic.desc?.text ?: ""
        val pubTime = LocalDateTime.ofEpochSecond(info.data.item.modules.moduleAuthor.pubTs, 0, ZoneOffset.ofHours(8))
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

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

        val items = moduleDynamic.major?.opus?.pics?.mapIndexed { index, pic ->
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

        send {
            answerInlineQuery(inlineQuery.id, items).apply {
                if ((moduleDynamic.major?.opus?.pics?.size ?: 0) > 1) {
                    this.button = InlineQueryResultsButton().apply {
                        this.text = "合并图片为一条消息"
                        this.type = InlineQueryResultsButtonTypeStartBot().apply {
                            parameter = "dynamic$id"
                        }
                    }
                }
            }
        }

    }

    private suspend fun doHandle(inlineQuery: UpdateNewInlineQuery, id: String, p: Int, t: Float) {
        log.info("Handle bilibili: $id, p=$p, t=$t")
        val results = doHandle(id, p, t)?: run {
            fallback(inlineQuery)
            return
        }

        send(untilPersistent = true) {
            answerInlineQuery(inlineQuery.id, results).also {
                log.trace("Handle bilibili: {}", it)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    internal suspend fun doHandle(id: String, p: Int, t: Float): Array<InputInlineQueryResult>? {

        val videoInfo = BiliBiliService.getVideoInfo(id)
        val page = videoInfo.data.pages.find { it.page == p.coerceAtLeast(1) } ?: return null
        val streamInfo = BiliBiliService.getPlayUrl(videoInfo.data.bvid, page.cid)
        var link = "https://www.bilibili.com/video/${videoInfo.data.bvid}"

        val parameters = mutableListOf<String>()
        if (p > 0) parameters.add("p=$p")
        if (t > 0) parameters.add("t=$t")
        if (parameters.isNotEmpty()) {
            val paramStr = parameters.joinToString("&")
            link += "?$paramStr"
        }

        val playCount = "${((videoInfo.data.stat.view / 10.0).roundToInt() / 100.0)}K 播放"
        val videoTitle = videoInfo.data.title.trim()
        val partTitle = page.part.trim()
        val inlineTitle: String

        val rank =
            if (videoInfo.data.stat.nowRank == 0) "" else "/ ${videoInfo.data.stat.nowRank} 名 / 历史最高 ${videoInfo.data.stat.nowRank} 名"
        val createDate = LocalDateTime.ofEpochSecond(videoInfo.data.pubdate.toLong(), 0, ZoneOffset.ofHours(8))
            .format(PUB_DATE_PATTERN)
        val duration = page.duration.seconds.formatToSeparateUnit()

        val builder = FormattedTextBuilder()

        if (p == 0 || videoInfo.data.pages.size == 1 || videoTitle.contains(partTitle)) {
            inlineTitle = videoTitle
            builder.appendLink(videoTitle, link)
        } else if (partTitle.startsWith(videoTitle) || partTitle.endsWith(videoTitle)) {
            val partTitleCp = partTitle.replace(videoTitle, "").trim()
            inlineTitle = "$videoTitle / $partTitleCp"
            builder.appendLink(videoTitle, link)
                .appendText(" / $partTitleCp")
        } else {
            inlineTitle = "$videoTitle / $partTitle"
            builder.appendLink(videoTitle, link)
                .appendText(" / $partTitle")
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

        val canShowVideo = BiliBiliService.fetchStreamLength(streamInfo.data!!.durl.first().url) in 1..12*1024*1024
        val id = "${videoInfo.data.bvid.substring(2)}${videoInfo.data.cid.toHexString(hexFormat)}"
        return arrayOf(
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
            },
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
            }
        )
    }

}
