package moe.kurenai.bot.command.commands

import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandDispatcher
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.SendMessage
import reactor.core.publisher.Mono

@Command("help")
class Help : CommandHandler() {

    override fun execute(update: Update, message: Message, args: List<String>): Mono<*> {
        val commands = CommandDispatcher.commands.map { it.key }.joinToString("\n\n")
        val inlines = CommandDispatcher.inlineCommands.map { it.key }.joinToString("\n\n")
        return SendMessage(
            message.chatId, """
            ================= commands =================
            $commands
            ============== inline commands =============
            $inlines
        """.trimIndent()
        ).send()
    }
}