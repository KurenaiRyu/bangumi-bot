package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.Message
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.service.SakugabooruService
import moe.kurenai.bot.service.bangumi.BangumiApi
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.TelegramUtil.messageText


class Status : CommandHandler {
    override val command: String = "status"
    override val description: String = "机器人状态（并不一定准确）"

    override suspend fun execute(message: Message, sender: TdApi.MessageSenderUser, args: List<String>) {
        val runtime = Runtime.getRuntime()
        val arr = arrayOf(
            runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory(),
            runtime.maxMemory(),
            runtime.freeMemory(),
            runtime.totalMemory()
        )
            .map { it / 1024 / 1024 }
            .map { "${it}m" }
        val msg = """
            总可用内存: ${arr[0]}/${arr[1]}
            剩余可用分配内存: ${arr[2]}/${arr[3]}
            --------------------------------------
            SakugabooruCache: ${
            SakugabooruService.cacheStats.snapshot().hitCount()
        } / ${SakugabooruService.cacheStats.snapshot().loadCount()} (${
            SakugabooruService.cacheStats.snapshot().hitRate()
        })
        """.trimIndent()

        send(messageText(message.chatId, msg.asText()))
    }
}
