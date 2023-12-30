package moe.kurenai.bot

import com.sksamuel.aedile.core.caffeineBuilder
import it.tdlight.client.*
import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
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
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
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

    private val listenerLock = Mutex()
    private val listeners: MutableList<Listener<out Object?, out Update>> = mutableListOf()


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

//        log.debug("User bot update: ${update::class.java.name}")

        handleListener(update)

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

    @Suppress("UNCHECKED_CAST")
    private suspend fun handleListener(update: Update) = listenerLock.withLock {
        listeners.removeIf {
            val result = it.timeLimit?.let { System.currentTimeMillis() > it } ?: false
            if (!result) it.con.cancel(TimeoutException("Listener time out: ${it.timeout}"))
            result
        }
        listeners.removeIf { listener ->
            listener as Listener<Object?, Update>
            if (listener.match(update)) {
                val res = listener.onEvent(update)
                if (res.complete) {
                    listener.con.resume(res.result)
                    return@removeIf true
                }
            }
            false
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
                        this.replyTo = InputMessageReplyToMessage().apply {
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
        timeout: Duration = 10.seconds
    ): RemoteFile? {
        val linkPreviewGroup = Config.CONFIG.telegram.linkPreviewGroup ?: return null
        val message = send(untilPersistent = true) {
            messageText(linkPreviewGroup, url.asText())
        }
        var f = getFile(message.content, type)

        log.debug("Get file: {}", f)

        if (f == null) {
            val contentDefer =
                addListener(timeout) { event: UpdateMessageContent ->
                    if (event.chatId == message.chatId && event.messageId == message.id) {
                        log.debug("Update message content: {}", event)
                        val file = getFile(event.newContent, type)
                        if (file == null) ListenerResult.unComplete()
                        else ListenerResult.complete(file)
                    } else ListenerResult.unComplete()
                }
            f = runCatching {
                contentDefer.await()
            }.getOrNull()
        } else if (f.remote?.isUploadingCompleted == true) return f.remote

        val file = f ?: return null

        log.debug("Get file: {}", file)

        if (file.remote?.isUploadingCompleted == true) return file.remote

        val fileId = file.id
        val deferred: Deferred<File?> = addListener(timeout) { event: UpdateFile ->
            if (event.file.id == fileId && event.file.remote.isUploadingCompleted) {
                ListenerResult.complete(event.file)
            } else {
                ListenerResult.unComplete<File>()
            }
        }

        return runCatching {
            deferred.await()?.remote
        }.getOrNull()
    }

    private inline fun <R : Object?, reified Event : Update> addListener(
        timeout: Duration? = 5L.seconds,
        noinline matchBlock: ((Update) -> Boolean) = { update ->
            update is Event
        },
        noinline handleBlock: (Event) -> ListenerResult<R>
    ): Deferred<R> {
        return CoroutineScope(Dispatchers.IO).async {
            return@async suspendCancellableCoroutine { con ->
                val listener = Listener(con, timeout, matchBlock, handleBlock)
                listeners.add(listener)
                timeout?.let {
                    launch {
                        delay(timeout)
                        con.cancel(TimeoutException("Listener time out: $timeout"))
                    }
                }
            }
        }
    }

//    private suspend fun fetchRemoteFileByUrl(
//        url: String,
//        type: RemoteFileType,
//        timeout: Duration = 5.seconds
//    ): RemoteFile? {
//        var file: File?
//        val linkPreviewGroup = Config.CONFIG.telegram.linkPreviewGroup ?: return null
//        val message = send(untilPersistent = true) {
//            messageText(linkPreviewGroup, url.asText())
//        }
//        file = getFile(message, type)
//        if (file?.remote?.isUploadingCompleted == true) return file.remote
//        val end = System.currentTimeMillis() + timeout.inWholeMilliseconds
//        var completed = false
//        while ((file == null || !completed) && System.currentTimeMillis() < end) {
//            delay(200)
//            file = if (file == null) {
//                send(untilPersistent = true) {
//                    GetMessage(linkPreviewGroup, message.id)
//                }?.let {
//                    getFile(it, type)
//                }
//            } else {
//                send(untilPersistent = true) { GetFile(file!!.id) }
//            }?.also {
//                log.debug("upload file [{}] {}", it.id, it.remote.uploadedSize)
//                completed = it.remote?.isUploadingCompleted ?: false
//            }
//
//        }
//        return file?.remote
//    }

    private fun getFile(messageContent: MessageContent, type: RemoteFileType): File? {
        val content = messageContent as? MessageText ?: return null
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

    private data class Listener<R : Object?, Event : Update>(
        val con: CancellableContinuation<R>,
        val timeout: Duration? = 5L.seconds,
        private val matchBlock: ((Update) -> Boolean),
        private val handleBlock: (Event) -> ListenerResult<R>
    ) {

        val timeLimit = timeout?.inWholeMilliseconds?.let { it + System.currentTimeMillis() }

        fun match(event: Update): Boolean {
            return matchBlock.invoke(event)
        }

        @Suppress("UNCHECKED_CAST")
        fun onEvent(event: Update): ListenerResult<R> {

            return if ((event as? Event) == null) ListenerResult.unComplete<Event>() as ListenerResult<R>
            else handleBlock(event)
        }

    }

    data class ListenerResult<R : Object?>(
        val result: R? = null,
        val complete: Boolean
    ) {
        companion object {
            fun <R : Object> complete(obj: R) = ListenerResult(obj, true)
            fun <R : Object> unComplete(): ListenerResult<R> = ListenerResult(complete = false)
            fun <R : Object> fail(): ListenerResult<R> = ListenerResult(complete = true)
        }
    }
}
