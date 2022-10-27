package moe.kurenai.bot.command

import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.message.Update

abstract class InlineCommandHandler {

    abstract fun execute(update: Update, inlineQuery: InlineQuery, arg: String)

}