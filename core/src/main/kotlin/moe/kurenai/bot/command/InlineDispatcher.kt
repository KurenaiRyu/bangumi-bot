package moe.kurenai.bot.command

import it.tdlight.jni.TdApi.Ok
import it.tdlight.jni.TdApi.UpdateNewInlineQuery
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.inlines.BgmHandler
import moe.kurenai.bot.util.TelegramUtil.answerInlineQueryEmpty
import java.net.URI
import java.time.format.DateTimeFormatter
import java.util.*


object InlineDispatcher {

    internal val DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val registeredHandlers: TreeSet<HandlerAware> = sortedSetOf()

    suspend fun execute(inlineHandler: UpdateNewInlineQuery, input: String) {

    }

    suspend fun execute(inlineQuery: UpdateNewInlineQuery, uri: URI) {

        for (handler in registeredHandlers) {
            if (handler.handle(inlineQuery, uri) == HANDLED) return
        }

        // default handle bgm.tv
        if (BgmHandler.handle(inlineQuery, uri) == UNHANDLED) {
            fallback(inlineQuery)
        }
    }

    fun registryHandler(handler: InlineHandler, specificOrder: Int = 0) {
        synchronized(this) {
            registeredHandlers.add(HandlerAware(handler, specificOrder))
        }
    }

    internal suspend fun fallback(inlineQuery: UpdateNewInlineQuery): Boolean {
        return send(answerInlineQueryEmpty(inlineQuery.id)) is Ok
    }

    data class HandlerAware(
        val delegate: InlineHandler,
        val order: Int
    ) : InlineHandler by delegate, Comparable<HandlerAware> {
        override fun compareTo(other: HandlerAware): Int {
            val orderRes = order.compareTo(other.order)
            return if (orderRes == 0) this.delegate.javaClass.name.compareTo(other.delegate.javaClass.name)
            else orderRes
        }
    }
}
