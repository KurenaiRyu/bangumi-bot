package moe.kurenai.bot.command.commands

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import com.sksamuel.aedile.core.caffeineBuilder
import it.tdlight.jni.TdApi.Message
import it.tdlight.jni.TdApi.MessageSenderUser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.kurenai.bangumi.models.UserSubjectCollection
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.service.bangumi.TokenService
import moe.kurenai.bot.service.bangumi.UserService
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.TelegramUtil.messageDocument
import moe.kurenai.bot.util.TelegramUtil.messageText
import moe.kurenai.common.util.getLogger
import moe.kurenai.common.util.json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Duration.Companion.hours

class Collections : CommandHandler {
    override val command: String = "collections"
    override val description: String = "返回用户收藏列表json，CD时间1小时"

    private val lock = Mutex()
    private val doneUsers = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .asCache<Int, Int>()

    companion object {
        private val log = getLogger()
    }

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        kotlin.runCatching {
            val token = TokenService.findById(sender.userId)
            if (token == null) {
                send { messageText(message.chatId, "未授权，请私聊机器人发送/start进行授权".asText()) }
            } else {
                with(token) {
                    val zipFile = File("temp/collections-${userId}.zip").also {
                        it.parentFile.mkdirs()
                        if (!it.exists()) it.createNewFile()
                    }

                    lock.withLock(userId) {
                        if (!doneUsers.contains(userId)) {
                            var offset = 0
                            val limit = 50
                            val page = UserService.getCollections(limit = limit, offset = offset)
                            val total = arrayListOf<UserSubjectCollection>()
                            page.data?.let(total::addAll)
                            log.debug("Get Collections ${total.size}/${page.total}")
                            while ((page.total ?: 0) > offset + limit) {
                                offset += limit
                                UserService.getCollections(limit = limit, offset = offset).data?.let(
                                    total::addAll
                                )
                                log.debug("Get $userId Collections ${total.size}/${page.total}")
                            }
                            val bytes = json.encodeToString(total).toByteArray()
                            ZipOutputStream(zipFile.outputStream()).use { out ->
                                out.putNextEntry(
                                    ZipEntry(
                                        "collections-${userId}-${
                                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMhh-HHmm"))
                                        }.json"
                                    )
                                )
                                out.write(bytes)
                            }
                            send {
                                messageDocument(
                                    message.chatId,
                                    zipFile.path,
                                    "导出成功！由于性能问题，接下来1小时内将不再响应该命令。".asText()
                                )
                            }
                            doneUsers[userId] = 1
                        }
                    }
                    zipFile.delete()
                }
            }
        }.recover {
            log.error(it.message, it)
            send { messageText(message.chatId, "Bot内部异常".asText()) }
        }.getOrThrow()
    }
}
