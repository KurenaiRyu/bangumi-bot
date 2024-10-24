package moe.kurenai.bot.util

import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import kotlin.io.path.inputStream

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

val localProperties by lazy {
    Properties().also { p ->
        Path.of("local.properties").inputStream().use(p::load)
    }
}

fun getProp(key: String) = localProperties.getProperty(key) ?: System.getProperty(key) ?: System.getenv(key)

fun String.trimString(size: Int = 100) = if (this.length > size + 20) this.substring(0, size) + "..." else this

fun Boolean.toInt() = if (this) 1 else 0

fun String.urlBase64() = Base64.getUrlEncoder().encodeToString(this.toByteArray())
