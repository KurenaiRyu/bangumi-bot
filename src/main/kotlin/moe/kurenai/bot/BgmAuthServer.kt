package moe.kurenai.bot

import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.kurenai.bot.BgmAuthServer.authCache
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

    internal var serverPort: Int = 8080

    internal val authCache = caffeineBuilder<String, Long> {
        expireAfterWrite = 10.minutes
    }.build()

    fun start() {
        embeddedServer(CIO, port = serverPort, module = Application::authModule).start(false).also {
            log.info("Web server listen to $serverPort")
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
                            call.respondRedirect("https://t.me/${BangumiBot.tdClient.getMe().username}?start=success")
                            authCache.invalidate(randomCode)
                        }.onFailure {
                            log.error(it.message, it)
                            call.respondText { "Error: ${it.message} \n请从新发送指令进行绑定！" }
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
