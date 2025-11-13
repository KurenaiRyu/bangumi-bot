package moe.kurenai.bot.command.inlines

import it.tdlight.jni.TdApi.UpdateNewInlineQuery
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.HANDLED
import moe.kurenai.bot.command.HandleResult
import moe.kurenai.bot.command.InlineDispatcher.fallback
import moe.kurenai.bot.command.InlineHandler
import moe.kurenai.bot.command.UNHANDLED
import moe.kurenai.bot.service.bangumi.CharacterService
import moe.kurenai.bot.service.bangumi.EpisodeService
import moe.kurenai.bot.service.bangumi.PersonService
import moe.kurenai.bot.service.bangumi.SubjectService
import moe.kurenai.bot.service.bangumi.TokenService
import moe.kurenai.bot.util.TelegramUtil.answerInlineQuery
import moe.kurenai.common.util.getLogger
import java.net.URI

object BgmHandler : InlineHandler {

    private val log = getLogger()

    override suspend fun handle(inlineQuery: UpdateNewInlineQuery, uri: URI): HandleResult {
        // https://bgm.tv/ep/1561086
        // https://bgm.tv/subject/504054
        // https://bgm.tv/character/169543
        // https://bgm.tv/person/11523
        val params = uri.path.split("/")
        if (params.size != 3) return UNHANDLED
        doHandle(params, inlineQuery, uri)
        return HANDLED
    }

    private suspend fun doHandle(params: List<String>, inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle Bangumi")
        val id = params[2].toInt()
        val userId = inlineQuery.senderUserId
        with(TokenService.findById(userId)) {
            when (params[1]) {
                "subject" -> {
                    send(answerInlineQuery(
                        inlineQuery.id,
                        SubjectService.getContent(SubjectService.findById(id), uri.toString())
                    ))
                }

                "person" -> {
                    send(answerInlineQuery(
                        inlineQuery.id,
                        PersonService.getContent(PersonService.findById(id), uri.toString())
                    ))
                }

                "character" -> {
                    val character = CharacterService.findById(id)
                    val persons = CharacterService.findPersons(id)
                    send(answerInlineQuery(
                            inlineQuery.id,
                            CharacterService.getContent(character, uri.toString(), persons)
                        ))
                }

                "ep" -> {
                    val episode = EpisodeService.findById(id)
                    val subject = SubjectService.findById(episode.subjectId)
                    send(answerInlineQuery(
                        inlineQuery.id,
                        EpisodeService.getContent(episode, subject, uri.toString())
                    ))
                }

                else -> {
                    fallback(inlineQuery)
                }
            }
        }
    }


}
