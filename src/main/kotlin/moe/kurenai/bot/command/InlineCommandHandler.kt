package moe.kurenai.bot.command

import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.message.Update

interface InlineCommandHandler {

    fun execute(update: Update, inlineQuery: InlineQuery, arg: String)

}