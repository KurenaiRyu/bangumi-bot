package moe.kurenai.bot

import com.sksamuel.aedile.core.caffeineBuilder
import it.tdlight.client.*
import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.TelegramUtil.messageText
import moe.kurenai.bot.util.TelegramUtil.textOrCaption
import moe.kurenai.bot.util.TelegramUtil.userSender
import moe.kurenai.bot.util.getLogger
import moe.kurenai.bot.util.json
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.CountDownLatch
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import it.tdlight.client.Result as TdResult

/**
 * @author Kurenai
 * @since 2023/5/12 16:56
 */

object TelegramUserBot {

    val log = getLogger()

    val apiToken: APIToken

    // Configure the client
    val settings: TDLibSettings

    lateinit var client: SimpleTelegramClient

    val started = CountDownLatch(1)

    private val fetchRemoteFileSemaphore = Semaphore(5)

    val pendingMessage = caffeineBuilder<Long, CancellableContinuation<it.tdlight.client.Result<Object>>> {
        maximumSize = 200
        expireAfterWrite = 5.minutes
    }.build()


    private val remoteFileCache = caffeineBuilder<String, String> {
        maximumSize = 200
        expireAfterWrite = 7.days
    }.build()

    init {
        apiToken = APIToken(
            Config.CONFIG.telegram.apiId ?: 94575,
            Config.CONFIG.telegram.apiHash ?: "a3406de8d171bb422bb6ddf3bbd800e2"
        )
        settings = TDLibSettings.create(apiToken).apply {
            // Configure the session directory
            val sessionPath = Paths.get("tdlib").resolve("user")
            databaseDirectoryPath = sessionPath.resolve("data")
            downloadedFilesDirectoryPath = sessionPath.resolve("downloads")
            isFileDatabaseEnabled = true
            isChatInfoDatabaseEnabled = true
            isMessageDatabaseEnabled = true
        }
        CoroutineScope(Dispatchers.IO).launch {
            val path = Path.of("config/remote-file-cache.json")
            if (path.exists()) {
                runCatching {
                    json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), path.readText())
                        .forEach { (k, v) ->
                            remoteFileCache.put(k, v)
                        }
                }.onFailure { log.warn("Read remote file cache fail!", it) }
            } else {
                path.parent.createDirectories()
            }
            Runtime.getRuntime().addShutdownHook(Thread {
                runBlocking {
                    val content = json.encodeToString(
                        MapSerializer(String.serializer(), String.serializer()),
                        remoteFileCache.asMap()
                    )
                    path.writeText(
                        content,
                        options = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                    )
                }
            })
        }
    }

    fun start() {
        client = SimpleTelegramClientFactory().builder(settings).build(AuthenticationSupplier.qrCode())
        client.addUpdateHandler(UpdateAuthorizationState::class.java) { update ->
            if (update.authorizationState.constructor == AuthorizationStateReady.CONSTRUCTOR) {
                log.info("Telegram user bot started.")
                started.countDown()
                client.addUpdatesHandler {
                    handleUpdate(it)
                }
            }
        }
    }

    fun handleUpdate(update: Update) = CoroutineScope(Dispatchers.Default).launch {
        when (update) {
            is UpdateNewMessage -> {
//                handleMineralwater(update)
            }

            is UpdateMessageSendSucceeded -> {
                pendingMessage.getIfPresent(update.oldMessageId)?.let {
                    pendingMessage.invalidate(update.oldMessageId)
                    it.resumeWith(Result.success(TdResult.of(update.message)))
                }
            }

            else -> {

            }
        }
    }

    @Deprecated("Necessary any more")
    private suspend fun handleMineralwater(update: UpdateNewMessage) {
        update.message.userSender()?.let { sender ->
            if (sender.userId != 537662249L) return
            update.message.content.textOrCaption()?.let { text ->
                if (text.text.trim().startsWith("https://twitter.com").not()) return
                send {
                    messageText(
                        update.message.chatId,
                        text.text.replace("https://twitter.com", "https://fxtwitter.com").asText()
                    ).apply {
                        this.replyTo = MessageReplyToMessage().apply {
                            messageId = update.message.id
                            chatId = update.message.chatId
                        }
                    }
                }
            }
        }
    }

    suspend fun sendUrl(url: String) {
        val key = "${RemoteFileType.PHOTO}:$url"
        if (remoteFileCache.contains(key)) return
        val chatId = Config.CONFIG.telegram.linkPreviewGroup ?: return
        send {
            SendMessage().apply {
                this.chatId = chatId
                this.inputMessageContent = InputMessageText().apply {
                    this.text = url.asText()
                }
            }
        }
    }

    /**
     * Fetch remote file
     *
     * @param url
     * @param type
     * @return remote file id
     */
    suspend fun fetchRemoteFileIdByUrl(url: String, type: RemoteFileType = RemoteFileType.PHOTO): String? =
        fetchRemoteFileSemaphore.withPermit {
            val key = "$type:$url"
            val remoteFileId = remoteFileCache.getIfPresent(key)
            if (remoteFileId != null) return remoteFileId

            val remoteFile = fetchRemoteFileByUrl(url, type) ?: return null

            remoteFileCache.put(key, remoteFile.id)
            log.debug("Put remote file cache {}: {}", key, remoteFile.id)
            remoteFile.id
        }

    private suspend fun fetchRemoteFileByUrl(
        url: String,
        type: RemoteFileType,
        timeout: Duration = 5.seconds
    ): RemoteFile? {
        var file: File?
        val linkPreviewGroup = Config.CONFIG.telegram.linkPreviewGroup ?: return null
        val message = send(untilPersistent = true) {
            messageText(linkPreviewGroup, url.asText())
        }
        file = getFile(message, type)
        if (file?.remote?.isUploadingCompleted == true) return file.remote
        val end = System.currentTimeMillis() + timeout.inWholeMilliseconds
        var completed = false
        while ((file == null || !completed) && System.currentTimeMillis() < end) {
            delay(200)
            file = if (file == null) {
                send(untilPersistent = true) {
                    GetMessage(linkPreviewGroup, message.id)
                }?.let {
                    getFile(it, type)
                }
            } else {
                send(untilPersistent = true) { GetFile(file!!.id) }
            }?.also {
                log.debug("upload file [{}] {}", it.id, it.remote.uploadedSize)
                completed = it.remote?.isUploadingCompleted ?: false
            }

        }
        return file?.remote
    }

    private fun getFile(message: Message, type: RemoteFileType): File? {
        val content = message.content as? MessageText ?: return null
        val webpage = content.webPage ?: return null
        return when (type) {
            RemoteFileType.PHOTO -> webpage.photo?.sizes?.firstOrNull()?.photo
            RemoteFileType.AUDIO -> webpage.audio?.audio
            RemoteFileType.VIDEO -> webpage.video?.video
            RemoteFileType.ANIMATION -> webpage.animation?.animation
            RemoteFileType.DOCUMENT -> webpage.document?.document
        }
    }

    fun getMe(): User = client.me

    fun getUsername(): String = getMe().usernames.editableUsername

    @Suppress("UNCHECKED_CAST")
    suspend inline fun <R : Object> send(
        untilPersistent: Boolean = false,
        timeout: Duration = 5.seconds,
        crossinline block: suspend () -> TdApi.Function<R>
    ): R = suspendCancellableCoroutine<it.tdlight.client.Result<R>> { con ->
        CoroutineScope(Dispatchers.IO).launch {
            client.send(block.invoke()) { result ->
                if (untilPersistent && !result.isError) {
                    val obj = result.get()
                    if ((obj as? Message)?.sendingState?.constructor == MessageSendingStatePending.CONSTRUCTOR) {
                        pendingMessage[obj.id] = con as CancellableContinuation<it.tdlight.client.Result<Object>>
                    } else {
                        con.resumeWith(Result.success(result))
                    }
                } else {
                    con.resumeWith(Result.success(result))
                }
            }
            delay(timeout)
            if (con.isActive) con.resumeWith(Result.failure(error("Telegram client timeout in $timeout s")))
        }
    }.get()

    enum class RemoteFileType {
        PHOTO,
        AUDIO,
        VIDEO,
        ANIMATION,
        DOCUMENT,
    }
}
