package moe.kurenai.bot.command

import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.message.Update
import reactor.core.publisher.Mono

abstract class InlineCommandHandler {

    abstract fun execute(update: Update, inlineQuery: InlineQuery, arg: String): Mono<*>

}