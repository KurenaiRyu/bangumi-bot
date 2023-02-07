package moe.kurenai.bot.util

import kotlinx.serialization.json.Json
import moe.kurenai.tdlight.request.message.AnswerInlineQuery
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Kurenai
 * @since 2022/10/27 16:20
 */

fun getLogger(name: String = Thread.currentThread().stackTrace[2].className): Logger {
    return LoggerFactory.getLogger(name)
}

fun getEmptyAnswer(inlineId: String): AnswerInlineQuery = AnswerInlineQuery(inlineId).apply {
    inlineResults = emptyList()
    cacheTime = 0
    switchPmText = "搜索结果为空"
    switchPmParameter = "help"
}

val json = Json {
    encodeDefaults = false
    ignoreUnknownKeys = true
    isLenient = true
}
