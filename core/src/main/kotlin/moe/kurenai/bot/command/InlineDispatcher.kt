package moe.kurenai.bot.command

import dev.zacsweers.metro.Inject
import it.tdlight.jni.TdApi.Ok
import it.tdlight.jni.TdApi.UpdateNewInlineQuery
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.util.TelegramUtil.answerInlineQueryEmpty
import java.net.URI
import java.time.format.DateTimeFormatter

@Inject
class InlineDispatcher(
    internal val inlineHandlers: Set<InlineHandler>
) {

    companion object {
        internal val DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        suspend fun fallback(inlineQuery: UpdateNewInlineQuery): Boolean {
            return send(answerInlineQueryEmpty(inlineQuery.id)) is Ok
        }
    }

    suspend fun execute(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        for (handler in inlineHandlers) {
            if (handler.handle(inlineQuery, uri) == HANDLED) return
        }

        fallback(inlineQuery)
    }
}
