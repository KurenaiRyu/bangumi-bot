package moe.kurenai.bot.command.commands

import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandDispatcher
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.SendMessage

@Command("help")
class Help : CommandHandler {

    override suspend fun execute(update: Update, message: Message, args: List<String>) {
        val commands = CommandDispatcher.commands.map { it.key }.joinToString("\n\n")
        val inlines = CommandDispatcher.inlineCommands.map { it.key }.joinToString("\n\n")
        SendMessage(
            message.chatId,
            "================= commands =================\n\n" +
                    commands +
                    "\n\n============== inline commands =============\n\n" +
                    inlines
        ).send()
    }
}