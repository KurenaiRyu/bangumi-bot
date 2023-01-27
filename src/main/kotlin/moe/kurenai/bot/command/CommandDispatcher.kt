package moe.kurenai.bot.command

import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.command.inlines.Search
import moe.kurenai.bot.command.inlines.SearchByURI
import moe.kurenai.bot.util.getEmptyAnswer
import moe.kurenai.bot.util.getLogger
import moe.kurenai.tdlight.model.MessageEntityType
import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.message.Update
import org.reflections.Reflections
import java.net.URI

object CommandDispatcher {

    private val log = getLogger()

    val commands = HashMap<String, CommandHandler>()
    val inlineCommands = HashMap<String, InlineCommandHandler>()
    val defaultInlineCommandHandler = Search()
    val uriInlineCommandHandler = SearchByURI

    init {
        for (reflection in Reflections("moe.kurenai.bot.command.commands").getSubTypesOf(CommandHandler::class.java)) {
            val annotation = reflection.getAnnotation(Command::class.java)
            if (annotation != null) {
                val instance = reflection.getConstructor().newInstance()
                commands[annotation.command.uppercase()] = instance

                for (alias in annotation.aliases) {
                    commands[alias.uppercase()] = instance
                }
                log.debug("Load command: ${instance.javaClass.name}")
            }
        }
        for (reflection in Reflections("moe.kurenai.bot.command.inlines").getSubTypesOf(InlineCommandHandler::class.java)) {
            val annotation = reflection.getAnnotation(Command::class.java)
            if (annotation != null) {
                val instance = reflection.getConstructor().newInstance()
                inlineCommands[annotation.command.uppercase()] = instance
                log.debug("Load inline command: ${instance.javaClass.name}")
            }
        }
    }

    suspend fun handle(update: Update) {
        val inlineQuery = update.inlineQuery
        val result = if (inlineQuery != null) {
            kotlin.runCatching {
                handleInlineQuery(update, inlineQuery)
            }
        } else {
            if (commands.isEmpty()) return
            val message = update.message
            val messageEntity = message?.entities?.firstOrNull { it.type == MessageEntityType.BOT_COMMAND }
            if (messageEntity?.text != null) {
                val commandText = messageEntity.text!!
                val index = commandText.indexOf("@")
                val command = if (index == -1) commandText.substring(1) else commandText.substring(1, index)
                val text = message.text?.trim()

                commands[command.uppercase()]?.let {
                    log.info("Match command ${it::class.simpleName}")
                    kotlin.runCatching {
                        it.execute(
                            update,
                            message,
                            if (text == null || text.length <= commandText.length) emptyList() else text.substring(commandText.length).trim().split(" ")
                        )
                    }
                } ?: return
            } else return
        }

        result.onFailure {
            log.error("Command handle error: ${it.message}", it)
        }
    }

    private suspend fun handleInlineQuery(update: Update, inlineQuery: InlineQuery) {
        if (inlineQuery.query.isBlank()) return
        val query = inlineQuery.query.trim()
        val offset = inlineQuery.offset.takeIf { it.isNotBlank() }?.toInt() ?: 0
        if (offset < 0) return
        val args = query.split(" ", limit = 2)
        when (args.size) {
            0 -> return
            1 -> {
                if (query.contains("https") || query.contains("http")) {
                    uriInlineCommandHandler.execute(inlineQuery, URI.create(query))
                } else {
                    getEmptyAnswer(inlineQuery.id).send()
                }
            }
            2 -> {
                val handler = inlineCommands[args[0]]
                if (handler != null) {
                    log.info("Match command ${handler.javaClass.name}")
                    handler.execute(update, inlineQuery, args[1])
                } else {
                    getEmptyAnswer(inlineQuery.id).send()
                }
            }
            else -> {
                getEmptyAnswer(inlineQuery.id).send()
            }
        }
    }

}
