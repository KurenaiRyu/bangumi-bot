package moe.kurenai.bot.command.commands

import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.InputMediaPhoto
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.UpdateMessage
import com.elbekd.bot.util.SendingString
import moe.kurenai.bgm.request.subject.GetCalendar
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.BangumiBot.telegram
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.SubjectRepository
import java.time.LocalDate

@Command(command = "air")
class Air : CommandHandler {

    override suspend fun execute(update: UpdateMessage, message: Message, args: List<String>) {
        val weekday = if (args.size == 1) args[0] else LocalDate.now().dayOfWeek.value

        val calendar = GetCalendar().send().find { it.weekday.id == weekday.toString() } ?: throw IllegalArgumentException("找不到该星期[$weekday]")
        SubjectRepository.findByIds(calendar.items.map { it.id })
            .asSequence()
            .sortedBy { it.id }
            .map { sub ->
                (sub.images?.large?.takeIf { it.isNotBlank() } ?: "https://bgm.tv/img/no_icon_subject.png") to "${sub.name}\n\n${sub.summary}"
            }.chunked(10)
            .forEach { list ->
                if (list.size == 1) {
                    val (link, content) = list[0]
                    telegram.sendPhoto(message.chat.id.toChatId(), SendingString(link), caption = content)
                } else {
                    val inputs = list.map { (link, content) ->
                        InputMediaPhoto(link, caption = content)
                    }
                    telegram.sendMediaGroup(message.chat.id.toChatId(), inputs)
                }
            }
    }
}
