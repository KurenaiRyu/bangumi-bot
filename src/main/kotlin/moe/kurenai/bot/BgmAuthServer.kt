package moe.kurenai.bot

import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.kurenai.bot.BgmAuthServer.authCache
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
        expireAfterWrite = 1.minutes
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

private fun Application.authModule() {
    val log = this.log
    routing {
        route("/callback") {
            get {
                log.info("${call.request.local.host}: ${call.request.uri}")
                val code = call.parameters["code"]
                val randomCode = call.parameters["state"]
                if (randomCode != null && code != null) {
                    authCache.getIfPresent(randomCode).let { userId ->
                        kotlin.runCatching {
                            log.debug("Attempt bind user: $userId")
                            val token = BangumiBot.bgmClient.getToken(code)
                            log.debug("Bind success: $userId : ${token.userId}")
                            call.respondRedirect("https://t.me/${BangumiBot.tdClient.getMe().username}?start=success")
                            authCache.invalidate(randomCode)
                        }.onFailure {
                            log.error(it.message, it)
                            call.respondText { "Error: ${it.message}" }
                        }
                    }
                } else {
                    call.respondText { "Error: 缺少必要的请求参数" }
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
