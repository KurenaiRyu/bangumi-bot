package moe.kurenai.bot

import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.kurenai.bgm.exception.BgmException
import moe.kurenai.bot.BgmAuthServer.authCache
import moe.kurenai.bot.TelegramBot.getUsername
import moe.kurenai.bot.repository.TokenRepository
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration.Companion.minutes

/**
 * @author Kurenai
 * @since 2023/1/26 19:56
 */

object BgmAuthServer {

    private val log = LoggerFactory.getLogger(BgmAuthServer::class.java)

    private val serverPort: Int = Config.CONFIG.bgm.server.port

    internal val authCache = caffeineBuilder<String, Long> {
        expireAfterWrite = 10.minutes
    }.build()

    private var server: BaseApplicationEngine =
        embeddedServer(Netty, port = serverPort, module = Application::authModule)

    fun start() {
        kotlin.runCatching {
            server.start(false).also {
                log.info("Web server listen to $serverPort")
            }
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
                                "${it.error} ${it.errorDescription}".ifBlank { it.message }
                            } else it.message
                            log.error(message, it)
                            call.respondText { "Error: ${message} \n请从新发送指令进行绑定！" }
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
