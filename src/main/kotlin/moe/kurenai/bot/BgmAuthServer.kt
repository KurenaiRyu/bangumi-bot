package moe.kurenai.bot

import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import moe.kurenai.bgm.exception.BgmException
import moe.kurenai.bot.BgmAuthServer.authCache
import moe.kurenai.bot.TelegramBot.getUsername
import moe.kurenai.bot.repository.TokenRepository
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyStore
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask
import kotlin.time.Duration.Companion.minutes


/**
 * @author Kurenai
 * @since 2023/1/26 19:56
 */

object BgmAuthServer {

    private val log = LoggerFactory.getLogger(BgmAuthServer::class.java)

    private val serverPort: Int = Config.CONFIG.bgm.server.port
    private val keyStorePw: String = Config.CONFIG.bgm.server.keyStorePw

    internal val authCache = caffeineBuilder<String, Long> {
        expireAfterWrite = 10.minutes
    }.build()

    internal val timer = Timer("DDOS-Task", true)

    private lateinit var server: EmbeddedServer<ApplicationEngine, out ApplicationEngine.Configuration>

    fun start(sslEnabled: Boolean = true) {
        kotlin.runCatching {
            if (::server.isInitialized.not()) {
                server = embeddedServer(
                    factory = Netty,
                    environment = applicationEnvironment { log = LoggerFactory.getLogger("ktor.application") },
                    configure = {
                        val pw = keyStorePw.toCharArray()
                        val keyStoreFile = File("./config/keystore.jks")
                        if (sslEnabled && pw.isNotEmpty() && keyStoreFile.exists()) {
                            sslConnector(
                                keyStore = KeyStore.getInstance(keyStoreFile, pw),
                                keyAlias = "bgm.kurenai.moe",
                                keyStorePassword = { pw },
                                privateKeyPassword = { pw }) {
                                keyStorePath = keyStoreFile
                                if (serverPort > 0) {
                                    port = serverPort
                                }
                            }
                        } else {
                            connector {
                                if (serverPort > 0) {
                                    host = "0.0.0.0"
                                    port = serverPort
                                }
                            }
                        }
                    },
                    module = Application::authModule
                )
            }
            server.start(false)
        }.onFailure {
            log.error("Start web server error", it)
        }
    }

    fun genRandomCode(userId: Long): String {
        val randomCode = UUID.randomUUID().toString().replace("-", "")
        log.info("Generate random code: $randomCode")
        authCache.put(randomCode, userId)
        return randomCode
    }

    //TODO: Avoid hard code
    @Deprecated("There is no public ip now")
    private fun runDdosTask() {
        timer.scheduleAtFixedRate(timerTask {
            runBlocking {
                val ipResult = HttpClient().get {
                    url("https://api-ipv4.ip.sb/ip ")
                    userAgent("Mozilla")
                }
                val json = """
                {
                  "content": "${ipResult.bodyAsText().trim()}"
                }
            """.trimIndent()
                HttpClient().patch {
                    url("https://api.cloudflare.com/client/v4/zones/3317d4133b44b844384f76f48f5d804e/dns_records/e69c33d3136591b0adae05d8bbd6ef83")
                    setBody(json)
                    contentType(ContentType.Application.Json)
                    header("X-Auth-Email", "kurenai233@yahoo.com")
                    header("X-Auth-Key", "41bf28cdc2ae64002bc38c0efaf0e489fc0c6")
                }
            }
        }, 5000L, TimeUnit.HOURS.toMillis(1))
    }

}

fun Application.authModule() {
    val log = this.log
    routing {
        route("/callback") {
            get {
                log.info("${call.request.local.localHost}: ${call.request.uri}")
                val code = call.parameters["code"]
                val randomCode = call.parameters["state"]
                if (randomCode != null && code != null) {
                    authCache.getIfPresent(randomCode)?.also { userId ->
                        kotlin.runCatching {
                            log.info("Attempt bind user: $userId")
                            val token = BangumiBot.bgmClient.getToken(code)
                            TokenRepository.put(userId, token)
                            log.info("Bind telegram id $userId to bangumi id ${token.userId}")
                            call.respondRedirect("https://t.me/${getUsername()}?start=success")
                            authCache.invalidate(randomCode)
                        }.onFailure {
                            val message = if (it is BgmException) {
                                val error = it.error
                                "${error.error} ${error.errorDescription}".ifBlank { it.message }
                            } else it.message
                            log.error(message, it)
                            call.respondText { "Error: $message \n请从新发送指令进行绑定！" }
                        }
                    } ?: kotlin.run {
                        call.respondText { "随机码失效，请从新发送指令进行绑定！" }
                    }
                } else {
                    call.respondText { "Error: 缺少必要的请求参数，请从新发送指令进行绑定！" }
                }
            }
        }
        route("/") {
            get {
                call.respondText { "Bangumi Bot. power by Kurenai" }
            }
        }
    }
}
