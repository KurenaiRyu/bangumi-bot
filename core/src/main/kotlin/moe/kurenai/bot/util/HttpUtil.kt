package moe.kurenai.bot.util

import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import jdk.dynalink.StandardOperation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import moe.kurenai.common.util.getLogger
import moe.kurenai.common.util.json
import org.jsoup.Jsoup
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.io.path.writer
import kotlin.time.Duration.Companion.days

/**
 * @author Kurenai
 * @since 2023/2/28 5:02
 */

object HttpUtil {

    private val log = getLogger()
    private val uaPath = Path("config/UA.txt")
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("HttpUtil") + SupervisorJob())
    private val uaUpdateFlow = MutableSharedFlow<String>()

    @Volatile
    var UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        private set

    val DYNAMIC_USER_AGENT = createClientPlugin("DynamicUserAgent") {
        onRequest { req, _ ->
            req.header(HttpHeaders.UserAgent, UA)
        }
    }

    init {
        scope.launch {
            uaUpdateFlow.collect {
                runCatching {
                    if (UA != it) {
                        UA = it
                        uaPath.writeText(it, Charsets.UTF_8, StandardOpenOption.APPEND)
                        log.info("Update UA: {}", it)
                    }
                }.onFailure { ex ->
                    log.error("Update UA error", ex)
                }
            }
        }
        scope.launch {
            runCatching {
                if (uaPath.exists()) {
                    uaUpdateFlow.emit(uaPath.readText())
                }
            }.onFailure {
                log.error("Read UA file error", it)
            }

            while (true) {
                runCatching {
                    val ua = getLatestUA()
                    uaUpdateFlow.emit(ua)
                }.onFailure {
                    log.error("Fetch latest UA error", it)
                }
                delay(1.days)
            }
        }
    }

    private suspend fun getLatestUA(): String {
        return json.decodeFromString<List<String>>(
            client.get("https://jnrbsn.github.io/user-agents/user-agents.json")
                .bodyAsText()
        ).last { it.contains("Windows NT") && it.contains("Chrome") }
    }

    suspend fun getOgImageInfo(url: Url, client: HttpClient = this.client): Pair<String, List<String>>? {
        val doc = Jsoup.parse(client.get(url).bodyAsText())
        val title = doc.select("meta[property='og:site:name']").attr("content").takeIf{ it.isNotEmpty() }?: doc.title()
        val imgList = doc.select("meta[property='og:image']").mapNotNull {
            it.attr("content").takeIf { it.isNotEmpty() }
        }

        if (imgList.isEmpty()) return null

        return title to imgList
    }

}
