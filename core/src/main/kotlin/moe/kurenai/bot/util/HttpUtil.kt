package moe.kurenai.bot.util

import io.ktor.client.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import moe.kurenai.common.util.getLogger
import moe.kurenai.common.util.json
import org.jsoup.Jsoup
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.days

/**
 * @author Kurenai
 * @since 2023/2/28 5:02
 */

object HttpUtil {

    private val log = getLogger()
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("HttpUtil") + SupervisorJob())
    private val uaRef = AtomicReference("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")

    val USER_AGENT
        get() = uaRef.get()

    val DYNAMIC_USER_AGENT = createClientPlugin("DynamicUserAgent") {
        onRequest { req, _ ->
            req.header(HttpHeaders.UserAgent, uaRef.get())
        }
    }

    init {
        scope.launch {
            while (true) {
                runCatching {
                    uaRef.set(getLatestUA())
                    log.info("Update UA: {}", uaRef.get())
                }.onFailure {
                    log.error("Update ua error", it)
                }
                delay(1.days)
            }
        }
        runCatching {
            uaRef.set(runBlocking { getLatestUA() })
        }.onFailure {
            log.error("Update ua error", it)
        }
    }

    private suspend fun getLatestUA(): String {
        return json.decodeFromString<List<String>>(
            client.get("https://jnrbsn.github.io/user-agents/user-agents.json").bodyAsText()
        ).last { it.contains("Windows NT") && it.contains("Chrome") }
    }

    suspend fun getOgImageUrl(url: Url, client: HttpClient = this.client): List<String> {
        val doc = Jsoup.parse(client.get(url).bodyAsText())
        return doc.select("meta[property='og:image']").mapNotNull {
            it.attr("content")
        }
    }

}
