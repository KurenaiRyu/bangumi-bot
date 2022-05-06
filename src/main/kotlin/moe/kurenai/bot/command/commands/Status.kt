package moe.kurenai.bot.command.commands

import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import reactor.core.publisher.Mono


@Command("status")
class Status : CommandHandler() {

    override fun execute(update: Update, message: Message, args: List<String>): Mono<*> {
        val runtime = Runtime.getRuntime()
        val arr = arrayOf(runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory(), runtime.maxMemory(), runtime.freeMemory(), runtime.totalMemory())
            .map { it / 1024 / 1024 }
            .map { "${it}m" }
        val msg = """
            总可用内存: ${arr[0]}/${arr[1]}
            剩余可用分配内存: ${arr[2]}/${arr[3]}
        """.trimIndent()

        return send(message.chatId, msg)
    }
}