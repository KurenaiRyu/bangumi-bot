package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi.Message
import it.tdlight.jni.TdApi.MessageSenderUser
import moe.kurenai.bot.TelegramBot.sendAlbumPhoto
import moe.kurenai.bot.TelegramBot.sendPhoto
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.service.bangumi.MicService
import moe.kurenai.bot.service.bangumi.SubjectService
import moe.kurenai.bot.util.TelegramUtil.asText
import java.time.LocalDate

class Air : CommandHandler {

    override val command: String = "air"
    override val description: String = "当日播放列表，/air N 则显示星期N的播放列表"

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        val weekday = if (args.size == 1) args[0] else LocalDate.now().dayOfWeek.value

        val calendar = MicService.getCalendar(sender.userId).find { weekday == it.weekday?.id }
            ?: throw IllegalArgumentException("找不到该星期[$weekday]")
        SubjectService.findByIds(calendar.items?.map { it.id ?: 0 } ?: listOf())
            .asSequence()
            .sortedBy { it.id }
            .map { sub ->
                (sub.images.large.takeIf { it.isNotBlank() }
                    ?: "https://bgm.tv/img/no_icon_subject.png") to "${sub.name}\n\n${sub.summary}".asText()
            }.chunked(10)
            .forEach { list ->
                if (list.size == 1) {
                    val (link, content) = list[0]
                    sendPhoto(message.chatId, link, content)
                } else {
                    sendAlbumPhoto(message.chatId, list)
                }
            }
    }
}
