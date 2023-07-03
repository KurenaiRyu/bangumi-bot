package moe.kurenai.bot.command

import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.kurenai.bot.TelegramBot
import moe.kurenai.bot.TelegramBot.getChat
import moe.kurenai.bot.TelegramBot.getUsername
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.commands.*
import moe.kurenai.bot.command.inlines.SearchByURI
import moe.kurenai.bot.util.TelegramUtil.answerInlineQueryEmpty
import moe.kurenai.bot.util.TelegramUtil.text
import moe.kurenai.bot.util.getLogger
import java.net.URI
import it.tdlight.client.Result as TdResult

object CommandDispatcher {

    private val log = getLogger()

    val commands: Map<String, CommandHandler> = listOf(
        Air(), Collections(), Start(), Status(), Watching()
    ).associateBy { handler ->
        handler.command.lowercase().also {
            log.debug("Load command: $it")
        }
    }

    val uriInlineCommandHandler = SearchByURI

    fun handle(update: Update) = CoroutineScope(Dispatchers.Default).launch {
        log.trace("Incoming update: {}", update.toString().trim())
        runCatching {
            when (update) {
                is UpdateNewInlineQuery -> {
                    if (log.isTraceEnabled.not()) log.debug(
                        "New inline query ({})[{}] from user {}, offset {}",
                        update.id,
                        update.query,
                        update.senderUserId,
                        update.offset
                    )
                    handleInlineQuery(update)
                }

                is UpdateNewMessage -> {
                    if (update.message.isOutgoing) {
                        if (log.isTraceEnabled.not()) log.debug(
                            "New message(out going) {} from chat {}",
                            update.message.id,
                            update.message.chatId
                        )
                        return@launch
                    } else {
                        if (log.isTraceEnabled.not()) log.debug(
                            "New message {} from chat {}",
                            update.message.id,
                            update.message.chatId
                        )
                    }
                    val content = update.message.content
                    val sender = update.message.senderId
                    if (content !is MessageText || sender !is MessageSenderUser) return@launch

                    val commandText =
                        content.text.entities.firstOrNull { it.type is TextEntityTypeBotCommand }
                            ?.text(content.text.text)
                            ?: return@launch
                    val index = commandText.indexOf("@")
                    val chat = send { GetChat(update.message.chatId) }

                    if (chat.type !is ChatTypePrivate && commandText.substring(index + 1) != getUsername()) return@launch

                    val command = if (index == -1) commandText.substring(1) else commandText.substring(1, index)
                    val text = content.text.text

                    commands[command.lowercase()]?.let {
                        log.info("Match command ${it::class.simpleName}")
                        send {
                            SendChatAction().apply {
                                this.chatId = update.message.chatId
                                this.action = ChatActionTyping()
                            }
                        }
                        it.execute(
                            update.message,
                            sender,
                            if (text == null || text.length <= commandText.length) emptyList() else text.substring(
                                commandText.length
                            ).trim().split(" ")
                        )
                    }
                }

                is UpdateMessageEdited -> {
                    if (log.isTraceEnabled.not()) log.debug(
                        "Edited message {} from chat {}",
                        update.messageId,
                        update.chatId
                    )
                }

                is UpdateDeleteMessages -> {
                    if (log.isTraceEnabled.not()) log.debug(
                        "Deleted messages {} from chat {}({})",
                        update.messageIds,
                        getChat(update.chatId).title,
                        update.chatId,
                    )
                }

                is UpdateMessageSendSucceeded -> {
                    if (log.isTraceEnabled.not()) log.debug(
                        "Sent message {} -> {} to chat {}({})",
                        update.oldMessageId,
                        update.message.id,
                        getChat(update.message.chatId).title,
                        update.message.chatId
                    )
                    TelegramBot.pendingMessage.getIfPresent(update.oldMessageId)?.let {
                        TelegramBot.pendingMessage.invalidate(update.oldMessageId)
                        it.resumeWith(Result.success(TdResult.of(update.message)))
                    }
                }

                is UpdateMessageSendFailed -> {
                    log.error(
                        "Sent message {} -> {} to chat {}({}) fail: {} {}",
                        update.oldMessageId,
                        update.message.id,
                        getChat(update.message.chatId).title,
                        update.message.chatId,
                        update.errorCode,
                        update.errorMessage
                    )
                    TelegramBot.pendingMessage.getIfPresent(update.oldMessageId)?.let {
                        TelegramBot.pendingMessage.invalidate(update.oldMessageId)
                        it.resumeWith(Result.failure(IllegalStateException("[${update.errorCode}] ${update.errorMessage}")))
                    }
                }

                is UpdateConnectionState -> {
                    if (log.isTraceEnabled.not()) log.debug("Update connection state: {}", update.state::class.java)
                }

                else -> {
                    if (log.isTraceEnabled.not()) log.debug("Not handle {}", update::class.java)
                }
            }
            update
        }.onFailure {
            log.error("Command handle error: ${it.message}", it)
        }
    }

    private suspend fun handleInlineQuery(update: UpdateNewInlineQuery) {
        if (update.query.isBlank()) return
        val query = update.query.trim()
        val offset = update.offset.takeIf { it.isNotBlank() }?.toInt() ?: 0
        if (offset < 0) return
        val args = query.split(" ", limit = 2)
        when (args.size) {
            0 -> return
            1 -> {
                handleUriInline(update, query)
            }

            2 -> {
                handleUriInline(update, query)
            }

            else -> {
                handleUriInline(update, query)
            }
        }
    }

    private suspend fun handleUriInline(inlineQuery: UpdateNewInlineQuery, query: String) {
        val index = query.indexOf("http")
        if (index != -1) {
            uriInlineCommandHandler.execute(inlineQuery, URI.create(query.substring(index)))
        } else {
            send { answerInlineQueryEmpty(inlineQuery.id) }
        }
    }

}
