package moe.kurenai.bot.command

import moe.kurenai.bot.command.inlines.Search
import moe.kurenai.bot.command.inlines.SearchByURI
import moe.kurenai.tdlight.model.MessageEntityType
import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.message.Update
import org.apache.logging.log4j.LogManager
import org.reflections.Reflections
import reactor.core.publisher.Mono
import java.net.URI

object CommandDispatcher {

    private val log = LogManager.getLogger()

    val commands = HashMap<String, CommandHandler>()
    val inlineCommands = HashMap<String, InlineCommandHandler>()
    val defaultInlineCommandHandler = Search()
    val uriInlineCommandHandler = SearchByURI()

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

    fun handle(update: Update) {
        val inlineQuery = update.inlineQuery
        val result = if (inlineQuery != null) {
            handleInlineQuery(update, inlineQuery)
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
                    log.info("Match command ${javaClass.name}")
                    it.execute(
                        update,
                        message,
                        if (text == null || text.length <= commandText.length) emptyList() else text.substring(commandText.length).split(" ")
                    )
                } ?: return
            } else return
        }

        result.log()
            .onErrorResume {
                log.error("Command handle error: ${it.message}", it)
                Mono.empty()
            }.subscribe()

    }

    private fun handleInlineQuery(update: Update, inlineQuery: InlineQuery): Mono<*> {
        if (inlineQuery.query.isBlank()) return Mono.empty<Any>()
        val query = inlineQuery.query.trim()
        val offset = inlineQuery.offset.takeIf { it.isNotBlank() }?.toInt() ?: 0
        if (offset < 0) return Mono.empty<Any>()
        val args = query.split(" ", limit = 2)
        when (args.size) {
            0 -> return Mono.empty<Any>()
            1 -> {
                if (query.contains("bgm.tv") || query.contains("bangumi.tv")) {
                    return uriInlineCommandHandler.execute(update, inlineQuery, URI.create(query))
                }
            }
            2 -> {
                val handler = inlineCommands[args[0]]
                if (handler != null) {
                    log.info("Match command ${handler.javaClass.name}")
                    return handler.execute(update, inlineQuery, args[1])
                }
            }
        }
        log.info("Match command ${defaultInlineCommandHandler.javaClass.name}")
        return defaultInlineCommandHandler.execute(update, inlineQuery, query)
    }

}