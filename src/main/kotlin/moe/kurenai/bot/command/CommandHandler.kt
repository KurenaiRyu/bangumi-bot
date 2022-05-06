package moe.kurenai.bot.command

import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import reactor.core.publisher.Mono

abstract class CommandHandler {

    abstract fun execute(update: Update, message: Message, args: List<String>): Mono<*>

}