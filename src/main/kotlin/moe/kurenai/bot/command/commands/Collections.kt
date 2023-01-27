package moe.kurenai.bot.command.commands

import com.sksamuel.aedile.core.caffeineBuilder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.kurenai.bgm.exception.BgmException
import moe.kurenai.bgm.model.user.UserCollection
import moe.kurenai.bgm.request.user.GetCollections
import moe.kurenai.bgm.request.user.GetMe
import moe.kurenai.bot.BangumiBot.MAPPER
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.token
import moe.kurenai.bot.util.getLogger
import moe.kurenai.tdlight.model.media.InputFile
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.SendDocument
import moe.kurenai.tdlight.request.message.SendMessage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Duration.Companion.hours

@Command(command = "collections", aliases = ["collect", "coll"])
class Collections : CommandHandler {

    private val lock = Mutex()
    private val doneUsers = caffeineBuilder<Long, Int> {
        expireAfterWrite = 1.hours
    }.build()

    companion object {
        private val log = getLogger()
    }

    override suspend fun execute(update: Update, message: Message, args: List<String>) {
        kotlin.runCatching {
            message.token()?.also { token ->
                val zipFile = File("temp/collections-${token.userId}.zip").also {
                    it.parentFile.mkdirs()
                    if (!it.exists()) it.createNewFile()
                }

                lock.withLock(token.userId) {
                    if (!doneUsers.contains(token.userId)) {
                        val me = GetMe().apply {
                            this.token = token.accessToken
                        }.send()
                        var offset = 0
                        val limit = 50
                        val page = GetCollections(me.username).apply {
                            this.limit = limit
                            this.offset = offset
                            this.token = token.accessToken
                        }.send()
                        val total = arrayListOf<UserCollection>()
                        total.addAll(page.data)
                        log.debug("Get Collections ${total.size}/${page.total}")
                        while (page.total > offset + limit) {
                            offset += limit
                            GetCollections(me.username).apply {
                                this.limit = limit
                                this.offset = offset
                                this.token = token.accessToken
                            }.send().also {
                                total.addAll(it.data)
                                log.debug("Get ${token.userId} Collections ${total.size}/${page.total}")
                            }
                        }
                        val bytes = MAPPER.writeValueAsBytes(total)
                        ZipOutputStream(zipFile.outputStream()).use { out ->
                            out.putNextEntry(ZipEntry("collections-${token.userId}-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMhh-HHmm"))}.json"))
                            out.write(bytes)
                        }
                        SendDocument(message.chatId, InputFile(zipFile)).apply {
                            caption = "导出成功！由于性能问题，接下来1小时内将不再响应该命令。"
                        }.send()
                        doneUsers[token.userId] = 1
                    }
                }
                zipFile.delete()
            } ?: kotlin.run {
                send(message.chatId, "未授权，请私聊机器人发送/start进行授权")
            }
        }.recover {
            log.error(it.message, it)
            if (it is BgmException) {
                SendMessage(message.chatId, "请求Bgm异常: [${it.code}] ${it.error} ${it.message ?: ""}")
            } else {
                SendMessage(message.chatId, "Bot内部异常")
            }.send()
        }.getOrThrow()
    }
}
