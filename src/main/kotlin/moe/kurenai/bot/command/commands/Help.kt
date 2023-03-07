package moe.kurenai.bot.command.commands

import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.UpdateMessage
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandDispatcher
import moe.kurenai.bot.command.CommandHandler

@Command("help")
class Help : CommandHandler {

    override suspend fun execute(update: UpdateMessage, message: Message, args: List<String>) {
        val commands = CommandDispatcher.commands.map { it.key }.joinToString("\n\n")
        val inlines = CommandDispatcher.inlineCommands.map { it.key }.joinToString("\n\n")

        BangumiBot.telegram.sendMessage(
            message.chat.id.toChatId(),
            "================= commands =================\n\n" +
                commands +
                "\n\n============== inline commands =============\n\n" +
                inlines
        )
    }
}
