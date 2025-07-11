package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi
import moe.kurenai.bangumi.models.SubjectCollectionType
import moe.kurenai.bangumi.models.SubjectType
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.service.bangumi.MicService
import moe.kurenai.bot.service.bangumi.SubjectService
import moe.kurenai.bot.service.bangumi.UserService
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import moe.kurenai.bot.util.TelegramUtil.messageText
import moe.kurenai.common.util.getLogger

class Watching : CommandHandler {
    override val command: String = "watching"
    override val description: String = "返回用户观看列表"

    companion object {
        private val log = getLogger()
    }

    override suspend fun execute(message: TdApi.Message, sender: TdApi.MessageSenderUser, args: List<String>) {
        val collections = UserService.getCollections(sender.userId, SubjectType.Anime, SubjectCollectionType.Doing)
        if (collections == null) {
            messageText(message.chatId, "未授权，请私聊机器人发送/start进行授权".asText())
            return
        }

        val subjects = SubjectService.findByIds(collections.data?.map { it.subjectId } ?: arrayListOf()).toList()
        val ids = MicService.getCalendar(sender.userId).flatMap { it.items ?: listOf() }.map { it.id }
        messageText(message.chatId,
            subjects.filter { s -> ids.contains(s.id) }
                .joinToString("\n\n") { "[${it.name.markdown()}](https://bgm.tv/subject/${it.id})" }.fmt()
        )
    }
}
