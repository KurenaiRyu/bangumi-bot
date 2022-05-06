package moe.kurenai.bot.handler

import moe.kurenai.bot.BangumiBot.tdClient
import moe.kurenai.tdlight.model.MessageEntityType
import moe.kurenai.tdlight.model.chat.ChatType
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.SendMessage
import org.apache.logging.log4j.LogManager
import org.reflections.Reflections
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object DelegatingHandler {

    private val privateHandler = HashMap<Long, Consumer<Update>>()
    private val removeSchedules = HashMap<Long, ScheduledFuture<*>>()
    private val log = LogManager.getLogger()
    private val handlers = ArrayList<AbstractHandler>()
    private val executorService = Executors.newSingleThreadScheduledExecutor()

    init {
        val reflections = Reflections("moe.kurenai.bot.handler")
        for (reflection in reflections.getSubTypesOf(AbstractHandler::class.java)) {
            handlers.add(reflection.getConstructor().newInstance())
        }
    }

    fun handle(update: Update) {

        if (update.message?.chat?.type == ChatType.PRIVATE) {
            privateHandler.remove(update.message?.from?.id)?.let {
                it.accept(update)
                return
            }
        }

        if (update.message?.isCommand() == true) {
            val command = update.message
                ?.takeIf { it.isCommand() }
                ?.entities?.firstOrNull { it.type == MessageEntityType.BOT_COMMAND }?.text?.replace("/", "")?.substringBefore("@")
                ?: return

            if (command == "help") {
                handleHelp(update.message!!)
            }

            val msg = handlers.firstOrNull { it.command == command }?.let {
                try {
                    log.info("Match ${it.name}")
                    it.handle(update, update.message!!)
                } catch (e: Throwable) {
                    log.error("Handle message error", e)
                    e.message
                }
            } ?: return
            tdClient.send(SendMessage(update.message!!.chat.id.toString(), msg))
        }
    }

    fun addPrivateHandler(userId: Long, consumer: Consumer<Update>) {
        privateHandler[userId] = consumer
        removeSchedules[userId]?.cancel(false)
        removeSchedules[userId] = executorService.schedule({ privateHandler.remove(userId) }, 30, TimeUnit.SECONDS)
    }

    private fun handleHelp(message: Message) {
        val sb = StringBuilder("Command list")
        sb.append("\n----------------\n")
        sb.append("Inline mode:\n")
        sb.append("使用 /A <keyword> 来搜索动画(其中M-音乐, B-书籍, G-游戏, R-三次元)\n")
        sb.append("使用 /subject-character <subject_id> [keyword] 搜索关联项\n")
        for (handler in handlers) {
            sb.append("\n----------------\n")
            sb.append("/${handler.command} ${handler.name}\n")
            sb.append(handler.help)
        }
        tdClient.send(SendMessage(message.chat.id.toString(), sb.toString()))
    }

}