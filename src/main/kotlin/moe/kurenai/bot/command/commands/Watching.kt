package moe.kurenai.bot.command.commands

import com.elbekd.bot.types.Message
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.UpdateMessage
import moe.kurenai.bgm.model.CollectionType
import moe.kurenai.bgm.model.SubjectType
import moe.kurenai.bgm.request.subject.GetCalendar
import moe.kurenai.bgm.request.user.GetCollections
import moe.kurenai.bgm.request.user.GetMe
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.BangumiBot.telegram
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.SubjectRepository
import moe.kurenai.bot.repository.token
import moe.kurenai.bot.util.TelegramUtil.chatId
import moe.kurenai.bot.util.TelegramUtil.fm2md
import moe.kurenai.bot.util.getLogger

@Command(command = "watching")
class Watching : CommandHandler {

    companion object {
        private val log = getLogger()
    }

    override suspend fun execute(update: UpdateMessage, message: Message, args: List<String>) {
        message.token()?.let { token ->
            val me = GetMe().apply {
                this.token = token.accessToken
            }.send()
            val collections = GetCollections(me.username).apply {
                subjectType = SubjectType.ANIME
                type = CollectionType.DOING
            }.send()
            val subjects = SubjectRepository.findByIds(collections.data.map { it.subjectId }).toList()
            val ids = GetCalendar().send().flatMap { it.items }.map { it.id }
            telegram.sendMessage(
                message.chatId(),
                subjects.filter { ids.contains(it.id) }
                    .joinToString("\n\n") { "[${it.name.fm2md()}](https://bgm.tv/subject/${it.id})" },
                parseMode = ParseMode.MarkdownV2
            )
        } ?: kotlin.run {
            telegram.sendMessage(message.chatId(), "未授权，请私聊机器人发送/start进行授权")
        }
    }
}
