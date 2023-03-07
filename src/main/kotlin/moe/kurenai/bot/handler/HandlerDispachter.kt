package moe.kurenai.bot.handler

import com.elbekd.bot.types.Message
import com.elbekd.bot.types.MessageEntity
import com.elbekd.bot.types.Update
import com.elbekd.bot.types.UpdateMessage
import com.elbekd.bot.util.isCommand
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.BangumiBot.telegram
import moe.kurenai.bot.util.TelegramUtil.chatId
import moe.kurenai.bot.util.TelegramUtil.text
import moe.kurenai.bot.util.getLogger
import org.reflections.Reflections
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object HandlerDispatcher {

    private val privateHandler = HashMap<Long, Consumer<Update>>()
    private val removeSchedules = HashMap<Long, ScheduledFuture<*>>()
    private val log = getLogger()
    private val handlers = ArrayList<AbstractHandler>()
    private val executorService = Executors.newSingleThreadScheduledExecutor()

    init {
        val reflections = Reflections("moe.kurenai.bot.handler")
        for (reflection in reflections.getSubTypesOf(AbstractHandler::class.java)) {
            handlers.add(reflection.getConstructor().newInstance())
        }
    }

    suspend fun handle(update: UpdateMessage) {

        if (update.message.chat.type.equals("private", true)) {
            privateHandler.remove(update.message.from?.id)?.let {
                it.accept(update)
                return
            }
        }

        if (update.message.isCommand(BangumiBot.me.username)) {
            val command = update.message
                .entities.firstOrNull { it.type == MessageEntity.Type.BOT_COMMAND }?.text(update.message)?.replace("/", "")?.substringBefore("@")
                ?: return

            if (command == "help") {
                handleHelp(update.message)
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
            telegram.sendMessage(update.message.chatId(), msg)
        }
    }

    fun addPrivateHandler(userId: Long, consumer: Consumer<Update>) {
        privateHandler[userId] = consumer
        removeSchedules[userId]?.cancel(false)
        removeSchedules[userId] = executorService.schedule({ privateHandler.remove(userId) }, 30, TimeUnit.SECONDS)
    }

    private suspend fun handleHelp(message: Message) {
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
        telegram.sendMessage(message.chatId(), sb.toString())
    }

}
