package moe.kurenai.bot.command.inlines

import moe.kurenai.bgm.model.subject.getGrid
import moe.kurenai.bgm.model.subject.getMedium
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.repository.*
import moe.kurenai.bot.util.getEmptyAnswer
import moe.kurenai.bot.util.getLogger
import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.inline.InlineQueryResultArticle
import moe.kurenai.tdlight.model.inline.InputTextMessageContent
import moe.kurenai.tdlight.request.message.AnswerInlineQuery
import java.net.URI

object SearchByURI {

    private val log = getLogger()

    suspend fun execute(inlineQuery: InlineQuery, uri: URI) {
        if (uri.host == "www.sakugabooru.com") {
            handleSakuga(inlineQuery, uri)
        } else {
            val params = uri.path.split("/")
            if (params.size != 3) {
                fallback(inlineQuery)
            } else {
                handleBgm(params, inlineQuery, uri)
            }
        }
    }

    private suspend fun handleBgm(params: List<String>, inlineQuery: InlineQuery, uri: URI) {
        val id = params[2].toInt()
        val userId = inlineQuery.from.id
        val token = TokenRepository.findById(userId)?.accessToken
        when (params[1]) {
            "subject" -> {
                SubjectRepository.findById(id, token).let { sub ->
                    AnswerInlineQuery(inlineQuery.id).apply {
                        this.inlineResults = listOf(InlineQueryResultArticle("S${sub.id}", sub.name).apply {
                            this.thumbUrl = sub.images.getMedium()
                            val (content, entities) = SubjectRepository.getContent(sub, uri.toString())
                            this.inputMessageContent = InputTextMessageContent(content).apply {
                                this.entities = entities
                            }
                        })
                    }.send()
                }
            }

            "person" -> {
                PersonRepository.findById(id, token).let { person ->
                    AnswerInlineQuery(inlineQuery.id).apply {
                        this.inlineResults = listOf(InlineQueryResultArticle("P${person.id}", person.name).apply {
                            this.thumbUrl = person.images.getGrid()
                            val (content, entities) = PersonRepository.getContent(person, uri.toString())
                            this.inputMessageContent = InputTextMessageContent(content).apply {
                                this.entities = entities
                            }
                        })
                    }.send()
                }
            }

            "character" -> {
                val character = CharacterRepository.findById(id, token)
                val persons = CharacterRepository.findPersons(id, token)
                val (content, entities) = CharacterRepository.getContent(character, uri.toString(), persons)
                AnswerInlineQuery(inlineQuery.id).apply {
                    this.inlineResults = listOf(
                        InlineQueryResultArticle("C${character.id}", character.name).apply {
                            this.thumbUrl = character.images.getGrid()
                            this.inputMessageContent = InputTextMessageContent(content).apply {
                                this.entities = entities
                            }
                        }
                    )
                }.send()
            }

            else -> {
                fallback(inlineQuery)
            }
        }
    }

    private suspend fun handleSakuga(inlineQuery: InlineQuery, uri: URI) {
        log.info("Handle Sakugabooru")
        val params = uri.path.split("/")
        if (params.size == 4) {
            if (params[1] == "post" && params[2] == "show") {
                val id = params[3]
                kotlin.runCatching {
                    AnswerInlineQuery(inlineQuery.id).apply {
                        this.inlineResults = listOf(SakugabooruRepository.findOne(id, uri))
                    }.send()
                }.onFailure {
                    log.error(it.message, it)
                    fallback(inlineQuery)
                }
            }
        }
    }

    private suspend fun fallback(inlineQuery: InlineQuery): Boolean {
        return getEmptyAnswer(inlineQuery.id).send()
    }
}
