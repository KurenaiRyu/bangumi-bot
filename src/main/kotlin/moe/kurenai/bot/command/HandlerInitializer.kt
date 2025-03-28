package moe.kurenai.bot.command

import moe.kurenai.bot.command.inlines.BgmHandler
import moe.kurenai.bot.command.inlines.BilibiliHandler
import moe.kurenai.bot.command.inlines.SakugabooruHandler

object HandlerInitializer {

    var initialized = false
        private set

    fun initialize() {
        if (initialized) return

        // access handler for initialization
        SakugabooruHandler
        BilibiliHandler
        BgmHandler
    }

}
