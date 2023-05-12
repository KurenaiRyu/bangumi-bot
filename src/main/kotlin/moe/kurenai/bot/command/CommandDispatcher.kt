package moe.kurenai.bot.command

import com.elbekd.bot.types.InlineQuery
import com.elbekd.bot.types.MessageEntity
import com.elbekd.bot.types.Update
import com.elbekd.bot.types.UpdateInlineQuery
import com.elbekd.bot.types.UpdateMessage
import moe.kurenai.bot.command.commands.Air
import moe.kurenai.bot.command.commands.Collections
import moe.kurenai.bot.command.commands.Help
import moe.kurenai.bot.command.commands.Start
import moe.kurenai.bot.command.commands.Status
import moe.kurenai.bot.command.commands.Watching
import moe.kurenai.bot.command.inlines.Search
import moe.kurenai.bot.command.inlines.SearchByURI
import moe.kurenai.bot.util.TelegramUtil
import moe.kurenai.bot.util.getLogger
import java.net.URI

object CommandDispatcher {

    private val log = getLogger()

    val commands = listOf(
        Air(), Collections(), Help(), Start(), Status(), Watching()
    ).associateBy { handler ->
        handler.javaClass.simpleName.uppercase().also {
            log.debug("Load command: $it")
        }
    }

    //    val inlineCommands = HashMap<String, InlineCommandHandler>()
    val defaultInlineCommandHandler = Search()
    val uriInlineCommandHandler = SearchByURI

    init {
//        for (reflection in Reflections("moe.kurenai.bot.command.commands").getSubTypesOf(CommandHandler::class.java)) {
//            val annotation = reflection.getAnnotation(Command::class.java)
//            if (annotation != null) {
//                val instance = reflection.getConstructor().newInstance()
//                commands[annotation.command.uppercase()] = instance
//
//                for (alias in annotation.aliases) {
//                    commands[alias.uppercase()] = instance
//                }
//                log.debug("Load command: ${instance.javaClass.name}")
//            }
//        }
//        for (reflection in Reflections("moe.kurenai.bot.command.inlines").getSubTypesOf(InlineCommandHandler::class.java)) {
//            val annotation = reflection.getAnnotation(Command::class.java)
//            if (annotation != null) {
//                val instance = reflection.getConstructor().newInstance()
//                inlineCommands[annotation.command.uppercase()] = instance
//                log.debug("Load inline command: ${instance.javaClass.name}")
//            }
//        }
    }

    suspend fun handle(update: Update) {
        val result = when (update) {
            is UpdateInlineQuery ->
                kotlin.runCatching {
                    handleInlineQuery(update, update.inlineQuery)
                }

            is UpdateMessage -> {
                val entity = update.message.entities.firstOrNull { it.type == MessageEntity.Type.BOT_COMMAND } ?: return
                val commandText = update.message.text?.substring(entity.offset, entity.length) ?: return
                val index = commandText.indexOf("@")
                val command = if (index == -1) commandText.substring(1) else commandText.substring(1, index)
                val text = update.message.text?.trim()

                commands[command.uppercase()]?.let {
                    log.info("Match command ${it::class.simpleName}")
                    kotlin.runCatching {
                        it.execute(
                            update,
                            update.message,
                            if (text == null || text.length <= commandText.length) emptyList() else text.substring(commandText.length).trim().split(" ")
                        )
                    }
                } ?: return
            }

            else -> {
                Result.success("")
            }
        }

        result.onFailure {
            log.error("Command handle error: ${it.message}", it)
        }
    }

    private suspend fun handleInlineQuery(update: UpdateInlineQuery, inlineQuery: InlineQuery) {
        if (inlineQuery.query.isBlank()) return
        val query = inlineQuery.query.trim()
        val offset = inlineQuery.offset.takeIf { it.isNotBlank() }?.toInt() ?: 0
        if (offset < 0) return
        val args = query.split(" ", limit = 2)
        when (args.size) {
            0 -> return
            1 -> {
                handleUriInline(inlineQuery, query)
            }

            2 -> {
//                val handler = inlineCommands[args[0]]
//                if (handler != null) {
//                    log.info("Match command ${handler.javaClass.name}")
//                    handler.execute(update, inlineQuery, args[1])
//                } else {
                handleUriInline(inlineQuery, query)
//                }
            }

            else -> {
                handleUriInline(inlineQuery, query)
            }
        }
    }

    private suspend fun handleUriInline(inlineQuery: InlineQuery, query: String) {
        val index = query.indexOf("http")
        if (index != -1) {
            uriInlineCommandHandler.execute(inlineQuery, URI.create(query.substring(index)))
        } else {
            TelegramUtil.answerInlineQueryEmpty(inlineQuery)
        }
    }

}
