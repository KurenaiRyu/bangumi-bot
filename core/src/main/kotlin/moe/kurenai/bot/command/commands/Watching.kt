package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi
import moe.kurenai.bgm.model.CollectionType
import moe.kurenai.bgm.model.SubjectType
import moe.kurenai.bgm.request.subject.GetCalendar
import moe.kurenai.bgm.request.user.GetCollections
import moe.kurenai.bgm.request.user.GetMe
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.SubjectRepository
import moe.kurenai.bot.repository.TokenRepository
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import moe.kurenai.bot.util.TelegramUtil.messageText
import moe.kurenai.bot.util.getLogger

class Watching : CommandHandler {
    override val command: String = "watching"
    override val description: String = "返回用户观看列表"

    companion object {
        private val log = getLogger()
    }

    override suspend fun execute(message: TdApi.Message, sender: TdApi.MessageSenderUser, args: List<String>) {
        TokenRepository.findById(sender.userId)?.let { token ->
            val me = GetMe().apply {
                this.token = token.accessToken
            }.send()
            val collections = GetCollections(me.username).apply {
                subjectType = SubjectType.ANIME
                type = CollectionType.DOING
            }.send()
            val subjects = SubjectRepository.findByIds(collections.data.map { it.subjectId }).toList()
            val ids = GetCalendar().send().flatMap { it.items }.map { it.id }
            messageText(message.chatId,
                subjects.filter { ids.contains(it.id) }
                    .joinToString("\n\n") { "[${it.name.markdown()}](https://bgm.tv/subject/${it.id})" }.fmt()
            )
        } ?: kotlin.run {
            messageText(message.chatId, "未授权，请私聊机器人发送/start进行授权".asText())
        }
    }
}
