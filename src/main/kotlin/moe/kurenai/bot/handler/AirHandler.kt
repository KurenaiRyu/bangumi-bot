//package moe.kurenai.bot.handler
//
//import moe.kurenai.bgm.request.subject.GetCalendar
//import moe.kurenai.bot.BangumiBot.bgmClient
//import moe.kurenai.bot.BangumiBot.getSubjects
//import moe.kurenai.bot.BangumiBot.tdClient
//import moe.kurenai.tdlight.model.media.InputFile
//import moe.kurenai.tdlight.model.message.Message
//import moe.kurenai.tdlight.model.message.Update
//import moe.kurenai.tdlight.request.message.InputMediaPhoto
//import moe.kurenai.tdlight.request.message.SendMediaGroup
//import moe.kurenai.tdlight.request.message.SendPhoto
//import org.apache.logging.log4j.LogManager
//import java.time.LocalDate
//
//class AirHandler : AbstractHandler() {
//
//    companion object {
//        private val log = LogManager.getLogger()
//    }
//
//    override fun handle(update: Update, message: Message): String? {
//        val params = message.text?.split(" ")?.takeIf { it.size == 2 }?.get(1)
//        bgmClient.send(GetCalendar()).thenApply { calendars ->
//            val weekday = params?:LocalDate.now().dayOfWeek.value.toString()
//            calendars.firstOrNull { it.weekday.id == weekday }?.let { calendar ->
//                getSubjects(calendar.items.map { it.id }).thenApply { subjects ->
//                    subjects.sortedBy { it.id }.map { sub ->
//                        InputMediaPhoto(InputFile(sub.images?.large?.takeIf { it.isNotBlank() } ?: "https://bgm.tv/img/no_icon_subject.png")).apply {
//                            caption = "${sub.name}\n\n${sub.summary}"
//                        }
//                    }.chunked(10).forEach { list ->
//                        if (list.size == 1) {
//                            val input = list[0]
//                            tdClient.send(SendPhoto(message.chatId, input.media).apply {
//                                caption = input.caption
//                            })
//                        } else {
//                            tdClient.send(SendMediaGroup(message.chatId).apply {
//                                media = list
//                            })
//                        }
//                    }
//                }
//            }
//        }
//        return null
//    }
//}