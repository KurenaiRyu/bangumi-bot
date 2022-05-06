package moe.kurenai.bot.command

import moe.kurenai.bot.util.StringPool.EMPTY

annotation class Command(
    val command: String,
    val aliases: Array<String> = [],
    val description: String = EMPTY
)
