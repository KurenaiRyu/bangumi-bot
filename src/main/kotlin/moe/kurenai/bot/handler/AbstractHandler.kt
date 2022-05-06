package moe.kurenai.bot.handler

import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update

abstract class AbstractHandler {

    open val command: String = this.javaClass.simpleName.lowercase().replace("handler", "")
    open val help: String = "No help information."
    open val name: String = this.javaClass.simpleName

    abstract fun handle(update: Update, message: Message): String?


}