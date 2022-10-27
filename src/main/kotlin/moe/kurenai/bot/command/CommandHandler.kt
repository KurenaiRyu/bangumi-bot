package moe.kurenai.bot.command

import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update

abstract class CommandHandler {

    abstract suspend fun execute(update: Update, message: Message, args: List<String>)

}