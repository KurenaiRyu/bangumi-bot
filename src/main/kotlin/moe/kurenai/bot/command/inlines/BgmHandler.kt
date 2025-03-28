package moe.kurenai.bot.command.inlines

import it.tdlight.jni.TdApi.UpdateNewInlineQuery
import moe.kurenai.bgm.util.getLogger
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.command.HANDLED
import moe.kurenai.bot.command.HandleResult
import moe.kurenai.bot.command.InlineDispatcher.fallback
import moe.kurenai.bot.command.InlineHandler
import moe.kurenai.bot.command.UNHANDLED
import moe.kurenai.bot.repository.CharacterRepository
import moe.kurenai.bot.repository.PersonRepository
import moe.kurenai.bot.repository.SubjectRepository
import moe.kurenai.bot.repository.TokenRepository
import moe.kurenai.bot.util.TelegramUtil.answerInlineQuery
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
        val token = TokenRepository.findById(userId)?.accessToken
        when (params[1]) {
            "subject" -> {
                send {
                    answerInlineQuery(
                        inlineQuery.id,
                        SubjectRepository.getContent(SubjectRepository.findById(id, token), uri.toString())
                    )
                }
            }

            "person" -> {
                send {
                    answerInlineQuery(
                        inlineQuery.id,
                        PersonRepository.getContent(PersonRepository.findById(id, token), uri.toString())
                    )
                }
            }

            "character" -> {
                val character = CharacterRepository.findById(id, token)
                val persons = CharacterRepository.findPersons(id, token)
                send {
                    answerInlineQuery(
                        inlineQuery.id,
                        CharacterRepository.getContent(character, uri.toString(), persons)
                    )
                }
            }

            else -> {
                fallback(inlineQuery)
            }
        }
    }


}
