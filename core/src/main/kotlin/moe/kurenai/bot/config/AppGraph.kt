package moe.kurenai.bot.config

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import moe.kurenai.bot.command.CommandDispatcher

@DependencyGraph(AppScope::class)
interface AppGraph {
    val commandDispatcher: CommandDispatcher
}
