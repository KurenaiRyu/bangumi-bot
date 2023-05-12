package moe.kurenai.bot

import com.elbekd.bot.Bot
import com.elbekd.bot.types.User
import moe.kurenai.bgm.BgmClient
import moe.kurenai.bgm.request.Request
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.command.CommandDispatcher
import moe.kurenai.bot.util.getLogger
import org.slf4j.Logger

object BangumiBot {

    private val serverPort = System.getProperty("PORT")?.toInt() ?: 8080

    val bgmClient = BgmClient(
        CONFIG.bgm.appId,
        CONFIG.bgm.appSecret,
        CONFIG.bgm.redirectUrl,
        isDebugEnabled = CONFIG.debug
    ).coroutine()

    val telegram = Bot.createPolling(CONFIG.telegram.token) {
        baseUrl = CONFIG.telegram.baseUrl
        timeout = 60
    }

    lateinit var me: User

    private val log: Logger = getLogger()

    suspend fun start() {
        BgmAuthServer.serverPort = serverPort
        BgmAuthServer.start()
        telegram.onAnyUpdate { update ->
            log.debug("Received: {}", update)
            CommandDispatcher.handle(update)
        }
        me = telegram.getMe()
        telegram.start()
    }

    suspend fun <T> Request<T>.send(): T = kotlin.runCatching {
        bgmClient.send(this)
    }.onFailure {
        log.error("Bgm request error: ${it.message}", it)
    }.getOrThrow()
}
