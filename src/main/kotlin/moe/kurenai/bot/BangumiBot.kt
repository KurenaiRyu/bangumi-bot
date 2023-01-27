package moe.kurenai.bot

import com.elbekd.bot.Bot
import com.elbekd.bot.model.toChatId
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import moe.kurenai.bgm.BgmClient
import moe.kurenai.bgm.request.Request
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.util.getLogger
import moe.kurenai.tdlight.client.TDLightCoroutineClient
import moe.kurenai.tdlight.model.ResponseWrapper
import org.slf4j.Logger
import java.time.Duration
import moe.kurenai.tdlight.request.Request as TDRequest

object BangumiBot {
    val MAPPER = jacksonObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT)

    private val CACHE_TTL = Duration.ofMinutes(10)
    private val serverPort = System.getProperty("PORT")?.toInt() ?: 8080

    private val redisMapper = jacksonObjectMapper()
        .registerModules(Jdk8Module(), JavaTimeModule())
        .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build(),
            ObjectMapper.DefaultTyping.EVERYTHING
        )

    val bgmClient = BgmClient(
        CONFIG.bgm.appId,
        CONFIG.bgm.appSecret,
        CONFIG.bgm.redirectUrl,
        isDebugEnabled = CONFIG.debug
    ).coroutine()

    val tgBot = Bot.createPolling(CONFIG.telegram.token) {
        baseUrl = CONFIG.telegram.baseUrl
    }

    val tdClient = TDLightCoroutineClient(
        CONFIG.telegram.baseUrl,
        CONFIG.telegram.token,
        CONFIG.telegram.userMode,
        isDebugEnabled = CONFIG.debug,
        updateBaseUrl = CONFIG.telegram.updateBaseUrl
    )
//    private lateinit var tgBot: LongPollingCoroutineTelegramBot

    private val log: Logger = getLogger()

    suspend fun start() {
        BgmAuthServer.serverPort = serverPort
        BgmAuthServer.start()
//        startRefreshTask()
//        tgBot = LongPollingCoroutineTelegramBot(listOf(UpdateSubscribe()), tdClient)
        tgBot.start()
    }

    suspend fun send(chatId: String, msg: String): com.elbekd.bot.types.Message {
        return tgBot.sendMessage(chatId.toChatId(), msg)
    }

    suspend fun <T> Request<T>.send(): T = kotlin.runCatching {
        bgmClient.send(this)
    }.onFailure {
        log.error("Bgm request error: ${it.message}", it)
    }.getOrThrow()

    suspend fun <T> TDRequest<ResponseWrapper<T>>.send(): T = tdClient.send(this)
}
