package moe.kurenai.bot.command.inlines

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.*
import moe.kurenai.bot.command.InlineDispatcher.fallback
import moe.kurenai.bot.service.BiliBiliService
import moe.kurenai.bot.util.TelegramUtil.answerInlineQuery
import moe.kurenai.common.util.getLogger
import java.net.URI

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

        val (id, p, t) = BiliBiliService.getIdPartNumAndTime(uri, redirectUrl)
        doHandle(inlineQuery, id, p, t)
    }

    private suspend fun handleDynamic(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle bilibili dynamic: $uri")
        val id = uri.path.substringAfterLast("/").takeIf { it.isNotBlank() } ?: run {
            fallback(inlineQuery)
            return
        }
        val items = BiliBiliService.handleDynamic(id)

        send(answerInlineQuery(inlineQuery.id, items).apply {
            if (items.size > 1) {
                this.button = InlineQueryResultsButton().apply {
                    this.text = "合并图片为一条消息"
                    this.type = InlineQueryResultsButtonTypeStartBot().apply {
                        parameter = "dynamic$id"
                    }
                }
            }
        })

    }

    private suspend fun doHandle(inlineQuery: UpdateNewInlineQuery, id: String, p: Int, t: Float) {
        log.info("Handle bilibili: $id, p=$p, t=$t")
        val results = BiliBiliService.handleVideo(id, p, t)

        send(answerInlineQuery(inlineQuery.id, results).also {
            log.trace("Handle bilibili: {}", it)
        })
    }

}
