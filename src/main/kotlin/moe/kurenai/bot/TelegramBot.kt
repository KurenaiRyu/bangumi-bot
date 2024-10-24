package moe.kurenai.bot

import com.sksamuel.aedile.core.caffeineBuilder
import it.tdlight.client.*
import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.*
import moe.kurenai.bot.TelegramUserBot.fetchRemoteFileIdByUrl
import moe.kurenai.bot.command.CommandDispatcher
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.getLogger
import java.nio.file.Paths
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import it.tdlight.client.Result as TdResult

/**
 * @author Kurenai
 * @since 2023/5/12 16:56
 */

object TelegramBot {

    val log = getLogger()

    private val apiToken: APIToken

    // Configure the client
    private val settings: TDLibSettings

    lateinit var client: SimpleTelegramClient

    val pendingMessage = caffeineBuilder<Long, CancellableContinuation<TdResult<Object>>> {
        maximumSize = 200
        expireAfterWrite = 5.minutes
    }.build()

    init {
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
    }

    fun start() {
        client = SimpleTelegramClientFactory().builder(settings)
            .build(AuthenticationSupplier.bot(Config.CONFIG.telegram.token))
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

    suspend fun sendPhoto(chatId: Long, photoUrl: String, msg: FormattedText): Message {
        val remoteFileId = fetchRemoteFileIdByUrl(photoUrl) ?: error("Fetch photo url ($photoUrl) fail!")

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
        val contents = pairs.mapNotNull { (url, msg) ->
            val remoteFileId = fetchRemoteFileIdByUrl(url) ?: run {
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

    @Suppress("UNCHECKED_CAST")
    suspend inline fun <R : Object> send(
        untilPersistent: Boolean = false,
        timeout: Duration = 5.seconds,
        crossinline block: suspend () -> TdApi.Function<R>
    ): R {
        val request = block.invoke()
        return withTimeout(timeout) {
            suspendCancellableCoroutine { con ->
                client.send(request) { result ->
                    if (untilPersistent && !result.isError) {
                        val obj = result.get()
                        if ((obj as? Message)?.sendingState?.constructor == MessageSendingStatePending.CONSTRUCTOR) {
                            pendingMessage[obj.id] = con as CancellableContinuation<TdResult<Object>>
                        } else {
                            con.resumeWith(Result.success(result.get()))
                        }
                    } else {
                        con.resumeWith(runCatching { result.get() })
                    }
                }
            }
        }
    }
}
