package moe.kurenai.bot.command

import com.elbekd.bot.types.Message
import com.elbekd.bot.types.UpdateMessage

interface CommandHandler {

    suspend fun execute(update: UpdateMessage, message: Message, args: List<String>)

}
