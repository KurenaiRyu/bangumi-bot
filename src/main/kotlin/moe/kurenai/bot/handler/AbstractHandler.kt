package moe.kurenai.bot.handler

import com.elbekd.bot.types.Message
import com.elbekd.bot.types.Update

abstract class AbstractHandler {

    open val command: String = this.javaClass.simpleName.lowercase().replace("handler", "")
    open val help: String = "No help information."
    open val name: String = this.javaClass.simpleName

    abstract fun handle(update: Update, message: Message): String?


}
