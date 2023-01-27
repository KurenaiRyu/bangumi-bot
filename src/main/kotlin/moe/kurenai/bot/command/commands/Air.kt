package moe.kurenai.bot.command.commands

import moe.kurenai.bgm.request.subject.GetCalendar
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.SubjectRepository
import moe.kurenai.tdlight.model.media.InputFile
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.InputMediaPhoto
import moe.kurenai.tdlight.request.message.SendMediaGroup
import moe.kurenai.tdlight.request.message.SendPhoto
import java.time.LocalDate

@Command(command = "air")
class Air : CommandHandler {

    override suspend fun execute(update: Update, message: Message, args: List<String>) {
        val weekday = if (args.size == 1) args[0] else LocalDate.now().dayOfWeek.value

        val calendar = GetCalendar().send().find { it.weekday.id == weekday.toString() } ?: throw IllegalArgumentException("找不到该星期[$weekday]")
        SubjectRepository.findByIds(calendar.items.map { it.id })
            .asSequence()
            .sortedBy { it.id }
            .map { sub ->
                InputMediaPhoto(InputFile(sub.images?.large?.takeIf { it.isNotBlank() } ?: "https://bgm.tv/img/no_icon_subject.png")).apply {
                    caption = "${sub.name}\n\n${sub.summary}"
                }
            }.chunked(10)
            .forEach { list ->
                if (list.size == 1) {
                    SendPhoto(message.chatId, list[0].media).apply {
                        caption = list[0].caption
                    }.send()
                } else {
                    SendMediaGroup(message.chatId).apply {
                        media = list
                    }.send()
                }
            }
    }
}
