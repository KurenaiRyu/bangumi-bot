package moe.kurenai.bot

import com.sksamuel.aedile.core.caffeineBuilder
import it.tdlight.client.APIToken
import it.tdlight.client.AuthenticationData
import it.tdlight.client.SimpleTelegramClient
import it.tdlight.client.TDLibSettings
import it.tdlight.common.Init
import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.*
import moe.kurenai.bot.TelegramUserBot.fetchRemoteFile
import moe.kurenai.bot.command.CommandDispatcher
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.getLogger
import java.nio.file.Paths
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * @author Kurenai
 * @since 2023/5/12 16:56
 */

object TelegramBot {

    val log = getLogger()

    val apiToken: APIToken

    // Configure the client
    val settings: TDLibSettings

    val client: SimpleTelegramClient

    private val sentMessageCache = caffeineBuilder<Long, Message> {
        maximumSize = 200
        expireAfterWrite = 5.minutes
    }.build()

    init {
        Init.start()
        apiToken = APIToken(
            Config.CONFIG.telegram.apiId ?: 94575,
            Config.CONFIG.telegram.apiHash ?: "a3406de8d171bb422bb6ddf3bbd800e2"
        )
        settings = TDLibSettings.create(apiToken).apply {
            // Configure the session directory
            val sessionPath = Paths.get("tdlib").resolve(Config.CONFIG.telegram.token.substringBefore(":"))
            databaseDirectoryPath = sessionPath.resolve("data")
            downloadedFilesDirectoryPath = sessionPath.resolve("downloads")
            isFileDatabaseEnabled = true
            isChatInfoDatabaseEnabled = true
            isMessageDatabaseEnabled = true
        }
        client = SimpleTelegramClient(settings)
    }

    fun start() {
        client.start(AuthenticationData.bot(Config.CONFIG.telegram.token))
        client.addUpdateHandler(UpdateAuthorizationState::class.java) { update ->
            if (update.authorizationState.constructor == AuthorizationStateReady.CONSTRUCTOR) {
                log.info("Telegram bot started.")
                CoroutineScope(Dispatchers.Default).launch {
                    client.addUpdatesHandler(CommandDispatcher::handle)
                    send {
                        SetCommands().apply {
                            this.scope = BotCommandScopeAllPrivateChats()
                            this.commands = CommandDispatcher.commands.map { cmd ->
                                BotCommand().apply {
                                    this.command = cmd.key.lowercase()
                                    this.description = cmd.value.description
                                }
                            }.toTypedArray()
                        }
                    }
                }
            }
        }
    }

    fun cacheSentMessage(update: UpdateMessageSendSucceeded) {
        sentMessageCache.put(update.oldMessageId, update.message)
    }

    suspend fun getSentMessage(oldMsgId: Long) = sentMessageCache.getIfPresent(oldMsgId)

    suspend fun sendPhoto(chatId: Long, photoUrl: String, msg: FormattedText): Message {
        val remoteFileId = fetchRemoteFile(photoUrl) ?: error("Fetch photo url ($photoUrl) fail!")

        return send {
            SendMessage().apply {
                this.chatId = chatId
                inputMessageContent = InputMessagePhoto().apply {
                    this.caption = msg
                    this.photo = InputFileRemote(remoteFileId)
                }
            }
        }
    }

    suspend fun sendAlbumPhoto(chatId: Long, pairs: Map<String, String>): Messages {
        val contents = pairs.entries.mapNotNull { (url, msg) ->
            val remoteFileId = fetchRemoteFile(url) ?: run {
                log.warn("Fetch photo url ({}) fail!", url)
                return@mapNotNull null
            }
            InputMessagePhoto().apply {
                this.caption = msg.asText()
                this.photo = InputFileRemote(remoteFileId)
            }
        }.toTypedArray()



        return send {
            SendMessageAlbum().apply {
                this.chatId = chatId
                this.inputMessageContents = contents
            }
        }
    }

    fun getMe(): User = client.me

    suspend inline fun getChat(chatId: Long) = send {
        GetChat(chatId)
    }

    fun getUsername(): String = client.me.usernames.editableUsername

    suspend inline fun <R : Object, Fun : TdApi.Function<R>> send(
        func: Fun,
        untilPersistent: Boolean = false,
        timeout: Duration = 5.seconds
    ): R =
        send(untilPersistent, timeout) { func }

    suspend inline fun <R : Object> send(
        untilPersistent: Boolean = false,
        timeout: Duration = 5.seconds,
        crossinline block: suspend () -> TdApi.Function<R>
    ): R =
        suspendCancellableCoroutine { con ->
            CoroutineScope(Dispatchers.IO).launch {
                withTimeout(20.seconds) {
                    client.send(block.invoke()) { result ->
                        var handled = false
                        if (untilPersistent && !result.isError) {
                            val res = result.get()
                            if (res is Message && (MessageSendingStatePending.CONSTRUCTOR == res.sendingState?.constructor || null == res.sendingState?.constructor)) {
                                handled = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    log.debug("Try to fetch persistent message by {} from chat {}", res.id, res.chatId)
                                    runCatching {
                                        withTimeoutOrNull(timeout) {
                                            var msg: Message? = getSentMessage(res.id)
                                            while (isActive && msg == null) {
                                                delay(1000)
                                                msg = getSentMessage(res.id)
                                            }
                                            log.debug(
                                                "Fetched persistent message {} by {} from chat {}",
                                                msg?.id,
                                                res.id,
                                                res.chatId
                                            )
                                            it.tdlight.client.Result.of(msg)
                                        } ?: result
                                    }.let(con::resumeWith)
                                }
                            }
                        }
                        if (handled.not()) con.resumeWith(Result.success(result))
                    }
                }
            }
        }.get()
}
