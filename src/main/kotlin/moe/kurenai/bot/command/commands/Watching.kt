package moe.kurenai.bot.command.commands

import moe.kurenai.bgm.model.CollectionType
import moe.kurenai.bgm.model.SubjectType
import moe.kurenai.bgm.request.subject.GetCalendar
import moe.kurenai.bgm.request.user.GetCollections
import moe.kurenai.bgm.request.user.GetMe
import moe.kurenai.bot.BangumiBot.getSubjects
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.BangumiBot.token
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.tdlight.model.ParseMode
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.SendMessage
import moe.kurenai.tdlight.util.MarkdownUtil.fm2md
import org.apache.logging.log4j.LogManager
import reactor.core.publisher.Mono

@Command(command = "watching")
class Watching : CommandHandler() {

    companion object {
        private val log = LogManager.getLogger()
    }

    override fun execute(update: Update, message: Message, args: List<String>): Mono<*> {
        return message.token()
            .log()
            .flatMap { token ->
                GetMe().apply {
                    this.token = token.accessToken
                }.send()
            }.flatMap { me ->
                GetCollections(me.username).apply {
                    subjectType = SubjectType.ANIME
                    type = CollectionType.DOING
                }.send()
            }.flatMapMany { page ->
                getSubjects(page.data.map { it.subjectId })
            }.collectSortedList { a, b ->
                a.id - b.id
            }.flatMap { subjects ->
                GetCalendar().send()
                    .flatMap { calendars ->
                        val ids = calendars.map { calendar -> calendar.items.map { it.id }.toMutableList() }.reduceRight { l, r ->
                            r.addAll(l)
                            r
                        }
                        SendMessage(message.chatId, subjects.filter { ids.contains(it.id) }.joinToString("\n\n") { "[${it.name.fm2md()}](https://bgm.tv/subject/${it.id})" }).apply {
                            parseMode = ParseMode.MARKDOWN_V2
                        }.send()
                    }
            }.switchIfEmpty(Mono.defer {
                send(message.chatId, "未授权，请私聊机器人发送/start进行授权")
            })
    }
}