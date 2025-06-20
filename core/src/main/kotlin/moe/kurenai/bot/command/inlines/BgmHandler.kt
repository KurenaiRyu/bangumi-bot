package moe.kurenai.bot.command.inlines

import it.tdlight.jni.TdApi.UpdateNewInlineQuery
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.HANDLED
import moe.kurenai.bot.command.HandleResult
import moe.kurenai.bot.command.InlineDispatcher.fallback
import moe.kurenai.bot.command.InlineHandler
import moe.kurenai.bot.command.UNHANDLED
import moe.kurenai.bot.service.bangumi.CharacterService
import moe.kurenai.bot.service.bangumi.PersonService
import moe.kurenai.bot.service.bangumi.SubjectService
import moe.kurenai.bot.service.bangumi.TokenService
import moe.kurenai.bot.util.TelegramUtil.answerInlineQuery
import moe.kurenai.common.util.getLogger
import java.net.URI

object BgmHandler : InlineHandler {

    private val log = getLogger()

    override suspend fun handle(inlineQuery: UpdateNewInlineQuery, uri: URI): HandleResult {
        val params = uri.path.split("/")
        if (params.size != 3) return UNHANDLED
        doHandle(params, inlineQuery, uri)
        return HANDLED
    }

    private suspend fun doHandle(params: List<String>, inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle Bangumi")
        val id = params[2].toInt()
        val userId = inlineQuery.senderUserId
        val token = TokenService.findById(userId)?.accessToken
        when (params[1]) {
            "subject" -> {
                send {
                    answerInlineQuery(
                        inlineQuery.id,
                        SubjectService.getContent(SubjectService.findById(id, token), uri.toString())
                    )
                }
            }

            "person" -> {
                send {
                    answerInlineQuery(
                        inlineQuery.id,
                        PersonService.getContent(PersonService.findById(id, token), uri.toString())
                    )
                }
            }

            "character" -> {
                val character = CharacterService.findById(id, token)
                val persons = CharacterService.findPersons(id, token)
                send {
                    answerInlineQuery(
                        inlineQuery.id,
                        CharacterService.getContent(character, uri.toString(), persons)
                    )
                }
            }

            else -> {
                fallback(inlineQuery)
            }
        }
    }


}
