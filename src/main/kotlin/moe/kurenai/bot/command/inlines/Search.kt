package moe.kurenai.bot.command.inlines

import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.message.Update
import reactor.core.publisher.Mono

class Search {

    fun execute(update: Update, inlineQuery: InlineQuery, query: String): Mono<*> {
        return Mono.empty<Any>()
    }


}