package moe.kurenai.bot.command

import moe.kurenai.bot.command.inlines.BgmHandler
import moe.kurenai.bot.command.inlines.BilibiliHandler
import moe.kurenai.bot.command.inlines.SakugabooruHandler

object HandlerInitializer {

    private var initialized = false

    fun initialize() {
        if (initialized) return

        // access handler for initialization
        SakugabooruHandler
        BilibiliHandler
        BgmHandler

        initialized = true
    }

}
