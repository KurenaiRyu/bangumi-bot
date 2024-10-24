package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi.*
import moe.kurenai.bgm.request.subject.GetCalendar
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.TelegramBot.sendAlbumPhoto
import moe.kurenai.bot.TelegramBot.sendPhoto
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.BiliBiliRepository
import moe.kurenai.bot.repository.SubjectRepository
import moe.kurenai.bot.util.TelegramUtil.asText
import java.time.LocalDate

class BiliDynamic : CommandHandler {

    override val command: String = "dynamic"
    override val description: String = "Bilibili 动态"

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        val url = args.firstOrNull() ?: return
        val id = url.substringAfterLast("/")
        val info = BiliBiliRepository.getDynamicDetail(id)

        val modules = if (info.data.item.orig != null) info.data.item.orig.modules else info.data.item.modules
        val summary = modules.moduleDynamic.major!!.opus.summary.text

        val caption = "${modules.moduleAuthor.name} - ${modules.moduleAuthor.pubTime}:\n\n$summary\n\n${url}"
        sendAlbumPhoto(message.chatId, modules.moduleDynamic.major.opus.pics.map { pic ->
            pic.url to caption
        }.toMap())
    }
}
