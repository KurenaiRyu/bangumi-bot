package moe.kurenai.bot.command

import it.tdlight.jni.TdApi.Ok
import it.tdlight.jni.TdApi.UpdateNewInlineQuery
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.util.TelegramUtil.answerInlineQueryEmpty
import java.net.URI

interface InlineHandler: Comparable<InlineHandler> {

    val order: Int get() = 0

    suspend fun handle(inlineQuery: UpdateNewInlineQuery, uri: URI): HandleResult

    override fun compareTo(other: InlineHandler): Int {
        val res = other.order.compareTo(order)
        if (res == 0) return other::class.qualifiedName!!.compareTo(this::class.qualifiedName!!)
        return res
    }
}
