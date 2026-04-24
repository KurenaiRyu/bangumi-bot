package moe.kurenai.bot.config

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import moe.kurenai.bot.command.CommandDispatcher
import moe.kurenai.bot.command.InlineDispatcher
import moe.kurenai.bot.command.InlineHandler

@DependencyGraph(AppScope::class)
interface AppGraph {
    val commandDispatcher: CommandDispatcher
    val inlineDispatcher: InlineDispatcher
}
