package moe.kurenai.bot.command

import com.elbekd.bot.types.InlineQuery
import com.elbekd.bot.types.UpdateInlineQuery

interface InlineCommandHandler {

    fun execute(update: UpdateInlineQuery, inlineQuery: InlineQuery, arg: String)

}
