package moe.kurenai.bot.command

import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update

interface CommandHandler {

    suspend fun execute(update: Update, message: Message, args: List<String>)

}