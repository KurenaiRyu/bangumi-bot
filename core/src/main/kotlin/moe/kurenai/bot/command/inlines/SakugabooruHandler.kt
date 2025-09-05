package moe.kurenai.bot.command.inlines

import it.tdlight.jni.TdApi.UpdateNewInlineQuery
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.*
import moe.kurenai.bot.command.InlineDispatcher.fallback
import moe.kurenai.bot.service.SakugabooruService
import moe.kurenai.bot.util.TelegramUtil.answerInlineQuery
import moe.kurenai.bot.util.TelegramUtil.answerInlineQueryEmpty
import moe.kurenai.common.util.getLogger
import java.net.URI

object SakugabooruHandler : InlineHandler {

    init {
        InlineDispatcher.registryHandler(this)
    }

    val log = getLogger()

    override suspend fun handle(inlineQuery: UpdateNewInlineQuery, uri: URI): HandleResult {
        when (uri.host) {
            "www.sakugabooru.com", "sakugabooru.com" -> doHandle(inlineQuery, uri)
            else -> return UNHANDLED
        }
        return HANDLED
    }

    private suspend fun doHandle(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle Sakugabooru [$uri]")
        val params = uri.path.split("/")
        if (params.size == 4) {
            if (params[1] == "post" && params[2] == "show") {
                val id = params[3]
                kotlin.runCatching {
                    SakugabooruService.findPostById(id)?.let {
                        send { answerInlineQuery(inlineQuery.id, arrayOf(it), cacheTime = 3600) }
                    }?: run {
                        fallback(inlineQuery)
                    }
                }.onFailure {
                    log.error(it.message, it)
                    fallback(inlineQuery)
                }
            }
        }
    }

}
