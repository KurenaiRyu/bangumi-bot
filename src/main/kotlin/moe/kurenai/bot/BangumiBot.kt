package moe.kurenai.bot

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import moe.kurenai.bgm.BgmClient
import moe.kurenai.bgm.request.Request
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.util.getLogger
import moe.kurenai.tdlight.LongPollingCoroutineTelegramBot
import moe.kurenai.tdlight.client.TDLightCoroutineClient
import moe.kurenai.tdlight.model.ResponseWrapper
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.request.message.SendMessage
import org.slf4j.Logger
import moe.kurenai.tdlight.request.Request as TDRequest

object BangumiBot {
    val MAPPER = jacksonObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT)

    private val serverPort = System.getProperty("PORT")?.toInt() ?: 8080

    val bgmClient = BgmClient(
        CONFIG.bgm.appId,
        CONFIG.bgm.appSecret,
        CONFIG.bgm.redirectUrl,
        isDebugEnabled = CONFIG.debug
    ).coroutine()

    val tdClient = TDLightCoroutineClient(
        CONFIG.telegram.baseUrl,
        CONFIG.telegram.token,
        CONFIG.telegram.userMode,
        isDebugEnabled = CONFIG.debug,
        updateBaseUrl = CONFIG.telegram.updateBaseUrl
    )
    private lateinit var tgBot: LongPollingCoroutineTelegramBot

    private val log: Logger = getLogger()

    suspend fun start() {
        BgmAuthServer.serverPort = serverPort
        BgmAuthServer.start()
        tgBot = LongPollingCoroutineTelegramBot(listOf(UpdateSubscribe()), tdClient)
        tgBot.start()
    }

    suspend fun send(chatId: String, msg: String): Message {
        return tdClient.send(SendMessage(chatId, msg))
    }

    suspend fun <T> Request<T>.send(): T = kotlin.runCatching {
        bgmClient.send(this)
    }.onFailure {
        log.error("Bgm request error: ${it.message}", it)
    }.getOrThrow()

    suspend fun <T> TDRequest<ResponseWrapper<T>>.send(): T = tdClient.send(this)
}
