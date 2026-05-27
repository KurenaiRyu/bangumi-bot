@file:Suppress("Unused")

package moe.kurenai.common.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import okio.FileSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.time.Duration

/**
 * @author Kurenai
 * @since 2022/10/27 16:20
 */

val FS = FileSystem.SYSTEM

val json = Json {
    encodeDefaults = false
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    serializersModule += SerializersModule {
        contextual(LocalDateTimeSerializer())
        contextual(LocalDateSerializer())
        contextual(OffsetDateTimeSerializer())
    }
}

fun getLogger(name: String = Thread.currentThread().stackTrace[2].className): Logger = LoggerFactory.getLogger(name)

fun buildKtorLogger(block: (String) -> Unit): io.ktor.client.plugins.logging.Logger {
    return object: io.ktor.client.plugins.logging.Logger {
        override fun log(message: String) {
            block(message)
        }
    }
}

val localProperties by lazy {
    val path = Path.of("local.properties")
    if (path.exists()) {
        runCatching {
            Properties().also { p ->
                path.inputStream().use(p::load)
            }
        }.getOrNull()
    } else null
}

fun getProp(key: String) = localProperties?.getProperty(key) ?: System.getProperty(key) ?: System.getenv(key)

fun String.trimString(size: Int = 100) = if (this.length > size + 20) this.substring(0, size) + "..." else this

fun Boolean.toInt() = if (this) 1 else 0

fun String.encodeUrl(): String = URLEncoder.encode(this, "utf-8")

fun Duration.formatToTime(): String {
    return this.toComponents { h, m, s, _ ->
        var res = "$m:$s"
        if (h > 0) res = "$h:$res"
        res
    }
}

fun Duration.formatToSeparateUnit(): String {
    return this.toComponents { h, m, s, _ ->
        var res = "${s}s"
        if (m > 0) res = "${m}m$res"
        if (h > 0) res = "${h}h$res"
        res
    }
}

fun removeOverlap(a: String, b: String): String {
    var maxLen = 0
    var startIndex = -1

    // 寻找最长公共子串
    for (i in a.indices) {
        for (j in b.indices) {
            var k = 0
            while (i + k < a.length && j + k < b.length && a[i + k] == b[j + k]) {
                k++
            }
            if (k > maxLen) {
                maxLen = k
                startIndex = i
            }
        }
    }

    // 如果有重叠部分则去掉
    return if (maxLen > 0) {
        a.removeRange(startIndex, startIndex + maxLen)
    } else a
}
