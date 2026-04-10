package moe.kurenai.bot.command.commands

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.Message
import it.tdlight.jni.TdApi.MessageSenderUser
import moe.kurenai.bot.TelegramBot
import moe.kurenai.bot.TelegramBot.sendAlbumPhoto
import moe.kurenai.bot.TelegramBot.sendPhoto
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.config.CookieFiles
import moe.kurenai.bot.service.BiliBiliService
import moe.kurenai.bot.service.bangumi.MicService
import moe.kurenai.bot.service.bangumi.SubjectService
import moe.kurenai.bot.service.bangumi.TokenService
import moe.kurenai.bot.util.TelegramUtil
import moe.kurenai.bot.util.TelegramUtil.asText
import java.time.LocalDate

@ContributesIntoSet(AppScope::class)
class Cookie : CommandHandler {

    override val command: String = "cookie"
    override val description: String = "更新cookie对应，不指定则默认为bilibili，不接cookie则进行查询。e.g. /cookie bilibili your_cookie; /cookie bilibili"

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        message.

        when (args.size) {
            0 -> {
            }
            else -> {}
        }

        val weekday = if (args.size == 1) args[0] else LocalDate.now().dayOfWeek.value

        with(TokenService.findById(sender.userId)) {
            val calendar = MicService.getCalendar().find { weekday == it.weekday?.id }
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

    private fun getCookie(cookieFile: CookieFiles, message: Message) {
        val cookie = when (cookieFile) {
            CookieFiles.BILIBILI -> BiliBiliService.cookieFlow.value
        }

        TelegramBot.send(TelegramUtil.messageText(message.chatId, ""))
    }
}
