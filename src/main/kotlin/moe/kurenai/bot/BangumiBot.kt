package moe.kurenai.bot

import moe.kurenai.bgm.BgmClient
import moe.kurenai.bgm.request.Request
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.util.getLogger
import org.slf4j.Logger

object BangumiBot {

    private val serverPort = CONFIG.bgm.server.port

    val bgmClient = BgmClient(
        CONFIG.bgm.appId,
        CONFIG.bgm.appSecret,
        CONFIG.bgm.redirectUrl,
        isDebugEnabled = CONFIG.debug
    ).coroutine()

    private val log: Logger = getLogger()

    fun start() {
        BgmAuthServer.start()
    }

    suspend fun <T> Request<T>.send(): T = kotlin.runCatching {
        bgmClient.send(this)
    }.onFailure {
        log.error("Bgm request error: ${it.message}", it)
    }.getOrThrow()
}
