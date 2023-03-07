package moe.kurenai.bot.util

import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Kurenai
 * @since 2022/10/27 16:20
 */

fun getLogger(name: String = Thread.currentThread().stackTrace[2].className): Logger {
    return LoggerFactory.getLogger(name)
}

val json = Json {
    encodeDefaults = false
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
}
