package moe.kurenai.bot.command.commands

import moe.kurenai.bgm.request.subject.GetCalendar
import moe.kurenai.bot.BangumiBot.getSubjects
import moe.kurenai.bot.BangumiBot.intRegex
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.tdlight.model.media.InputFile
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.InputMediaPhoto
import moe.kurenai.tdlight.request.message.SendMediaGroup
import moe.kurenai.tdlight.request.message.SendPhoto
import org.apache.logging.log4j.LogManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate

@Command(command = "air")
class Air : CommandHandler() {

    companion object {
        private val log = LogManager.getLogger()
    }

    override fun execute(update: Update, message: Message, args: List<String>): Mono<*> {
        val weekday = if (args.size == 1 && args[0].matches(intRegex)) args[0] else LocalDate.now().dayOfWeek.value

        return GetCalendar().send()
            .log()
            .flatMapMany { calendars ->
                Flux.fromIterable(calendars)
                    .filter { it.weekday.id == weekday.toString() }
            }.flatMap { calendar ->
                getSubjects(calendar.items.map { it.id })
                    .map { sub ->
                        InputMediaPhoto(InputFile(sub.images?.large?.takeIf { it.isNotBlank() } ?: "https://bgm.tv/img/no_icon_subject.png")).apply {
                            caption = "${sub.name}\n\n${sub.summary}"
                        }
                    }.collectList()
                    .map { list ->
                        list.chunked(10)
                    }
            }.flatMap { chunkedList ->
                Flux.fromIterable(chunkedList)
                    .flatMap { list ->
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
            }.collectList()
    }
}