package moe.kurenai.bot.command

import it.tdlight.jni.TdApi.Message
import it.tdlight.jni.TdApi.MessageSenderUser

interface CommandHandler {

    val command: String
    val description: String

    suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>)

}
