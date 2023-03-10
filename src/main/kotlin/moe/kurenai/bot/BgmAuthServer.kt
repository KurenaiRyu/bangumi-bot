package moe.kurenai.bot

import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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
        embeddedServer(Netty, port = serverPort, module = Application::authModule).start(false).also {
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
                            call.respondRedirect("https://t.me/${BangumiBot.telegram.getMe().username}?start=success")
                            authCache.invalidate(randomCode)
                        }.onFailure {
                            log.error(it.message, it)
                            call.respondText { "Error: ${it.message} \n????????????????????????????????????" }
                        }
                    } ?: kotlin.run {
                        call.respondText { "??????????????????????????????????????????????????????" }
                    }
                } else {
                    call.respondText { "Error: ??????????????????????????????????????????????????????????????????" }
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
