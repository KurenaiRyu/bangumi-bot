package moe.kurenai.bot.command.inlines

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bgm.util.getLogger
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.*
import moe.kurenai.bot.command.InlineDispatcher.PUB_DATE_PATTERN
import moe.kurenai.bot.command.InlineDispatcher.fallback
import moe.kurenai.bot.service.BiliBiliService
import moe.kurenai.bot.util.MimeTypes
import moe.kurenai.bot.util.TelegramUtil.answerInlineQuery
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import moe.kurenai.bot.util.formatToSeparateUnit
import moe.kurenai.bot.util.formatToTime
import moe.kurenai.bot.util.trimString
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object BilibiliHandler : InlineHandler {

    init {
        InlineDispatcher.registryHandler(this)
    }

    private val log = getLogger()

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
        val p = url.parameters["p"]?.toInt() ?: 1
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
        val summary =
            "${info.data.item.modules.moduleAuthor.name} - ${info.data.item.modules.moduleAuthor.pubTime}:\n\n$content\n\nhttps://t.bilibili.com/${id}"

        val caption = summary.markdown().fmt()

        info.data.item.orig?.let { orig ->
            val quoteContent = orig.modules.moduleDynamic.major?.opus?.summary?.text ?: ""
            val quoteSummary =
                "${orig.modules.moduleAuthor.name} - ${orig.modules.moduleAuthor.pubTime}:\n\n$quoteContent\n\nhttps://t.bilibili.com/${orig.idStr}"
            val start = caption.text.length
            caption.entities += TextEntity().apply {
                this.offset = start
                this.length = quoteSummary.length
                this.type = TextEntityTypeBlockQuote()
            }
            caption.text += quoteSummary
        }

        val items = moduleDynamic.major?.opus?.pics?.mapIndexed { index, pic ->
            InputInlineQueryResultPhoto().apply {
                this.id = index.toString()
                title =
                    "${info.data.item.modules.moduleAuthor.name} ${info.data.item.modules.moduleAuthor.pubTime}[$index]"
                thumbnailUrl = pic.url + "@240w_!web-dynamic.webp"
                photoUrl = pic.url + "@1920w_!web-dynamic.webp"
                photoWidth = pic.width
                photoHeight = pic.height
                inputMessageContent = InputMessagePhoto().apply {
                    this.caption = caption
                }
            }
        }?.toTypedArray()?.takeIf { it.isNotEmpty() }
            ?: arrayOf(InputInlineQueryResultArticle().apply {
                this.id = "dynamic$id"
                this.title =
                    "${info.data.item.modules.moduleAuthor.name} ${info.data.item.modules.moduleAuthor.pubTime}"
                this.inputMessageContent = InputMessageText().apply {
                    this.text = caption
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
        log.info("Handle bilibili: $id, $p, $t")

        val videoInfo = BiliBiliService.getVideoInfo(id)
        val desc = videoInfo.data.desc.trimString()
        val page = videoInfo.data.pages.find { it.page == p } ?: run {
            fallback(inlineQuery)
            return
        }
        val streamInfo = BiliBiliService.getPlayUrl(videoInfo.data.bvid, page.cid)
        var link = "https://www.bilibili.com/video/${videoInfo.data.bvid}"

        val parameters = mutableListOf<String>()
        if (p > 1) parameters.add("p=$p")
        if (t > 0) parameters.add("t=$t")
        if (parameters.isNotEmpty()) {
            val paramStr = parameters.joinToString("&")
            link += "?$paramStr"
        }

        val up = "UP: [${videoInfo.data.owner.name.markdown()}](https://space.bilibili.com/${videoInfo.data.owner.mid})"
        val playCount = "${((videoInfo.data.stat.view / 10.0).roundToInt() / 100.0).toString().markdown()}K 播放"
        var contentTitle = if (page.part.contains(videoInfo.data.title)) {
            "[${page.part.markdown()}]($link)"
        } else {
            "[${videoInfo.data.title.markdown()}]($link) / ${page.part.markdown()}"
        }

        if (t > 0) {
            val timeStr = (t * 1000).toLong().milliseconds.formatToTime().markdown()
            contentTitle += " / 跳转到 $timeStr"
        }

        val rank =
            if (videoInfo.data.stat.nowRank == 0) "" else "/ ${videoInfo.data.stat.nowRank} 名 / 历史最高 ${videoInfo.data.stat.nowRank} 名"
        val createDate = LocalDateTime.ofEpochSecond(videoInfo.data.pubdate.toLong(), 0, ZoneOffset.ofHours(8))
            .format(PUB_DATE_PATTERN)
            .markdown()
        val duration = page.duration.seconds.formatToSeparateUnit().markdown()
        val content = (contentTitle +
            "\n\n$up / $playCount $rank / $duration" +
            "\n发布时间: $createDate" +
            "\n\n${desc.markdown()}").fmt()

        val results = mutableListOf<InputInlineQueryResult>()
        results.add(InputInlineQueryResultPhoto().apply {
            this.id = "P_${videoInfo.data.bvid}_$p"
            this.title = "${page.part}/${videoInfo.data.title}"
            photoUrl = videoInfo.data.pic
            thumbnailUrl = videoInfo.data.pic
            inputMessageContent = InputMessagePhoto().apply {
                this.caption = content
            }
        })
        streamInfo.data?.run {
            results.add(InputInlineQueryResultVideo().apply {
                this.id = "V_${videoInfo.data.bvid}_$p"
                this.title = "${page.part}/${videoInfo.data.title}"
                videoUrl = streamInfo.data.durl!!.first().url
                thumbnailUrl = videoInfo.data.pic
                mimeType = MimeTypes.Video.MP4
                inputMessageContent = InputMessageVideo().apply {
                    this.caption = content
                }
            })
        }
        send {
//            TelegramUserBot.fetchRemoteFileIdByUrl(videoInfo.data.pic) ?: run {
//                log.warn("Fetch video image fail.")
//            }

            answerInlineQuery(inlineQuery.id, results.toTypedArray())
        }
    }

}
