package moe.kurenai.bot.command

sealed interface HandleResult

data object HANDLED : HandleResult
data object UNHANDLED : HandleResult


