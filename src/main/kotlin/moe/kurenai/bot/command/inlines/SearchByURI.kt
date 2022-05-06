package moe.kurenai.bot.command.inlines

import moe.kurenai.bgm.model.subject.getGrid
import moe.kurenai.bgm.model.subject.getMedium
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.BangumiBot.getCharacterContent
import moe.kurenai.bot.BangumiBot.getEmptyAnswer
import moe.kurenai.bot.BangumiBot.getPersonContent
import moe.kurenai.bot.BangumiBot.getSubjectContent
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.tdlight.model.ParseMode
import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.inline.InlineQueryResultArticle
import moe.kurenai.tdlight.model.inline.InputTextMessageContent
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.AnswerInlineQuery
import reactor.core.publisher.Mono
import java.net.URI

class SearchByURI {

    fun execute(update: Update, inlineQuery: InlineQuery, uri: URI): Mono<*> {
        val params = uri.path.split("/")
        return if (params.size != 3) {
            fallback(inlineQuery)
        } else {
            val ids = listOf(params[2].toInt())
            when (params[1]) {
                "subject" -> {
                    BangumiBot.getSubjects(ids).log()
                        .flatMap { sub ->
                            AnswerInlineQuery(inlineQuery.id).apply {
                                this.inlineResults = listOf(InlineQueryResultArticle("S${sub.id}", sub.name).apply {
                                    this.thumbUrl = sub.images.getMedium()
                                    this.inputMessageContent = InputTextMessageContent(getSubjectContent(sub)).apply {
                                        this.parseMode = ParseMode.MARKDOWN_V2
                                    }
                                })
                            }.send()
                        }.collectList()
                }
                "person" -> {
                    BangumiBot.getPersons(ids).log()
                        .flatMap { person ->
                            AnswerInlineQuery(inlineQuery.id).apply {
                                this.inlineResults = listOf(InlineQueryResultArticle("P${person.id}", person.name).apply {
                                    this.thumbUrl = person.images.getGrid()
                                    this.inputMessageContent = InputTextMessageContent(getPersonContent(person)).apply {
                                        this.parseMode = ParseMode.MARKDOWN_V2
                                    }
                                })
                            }.send()
                        }.collectList()
                }
                "character" -> {
                    BangumiBot.getCharacters(ids).log()
                        .flatMap { character ->
                            AnswerInlineQuery(inlineQuery.id).apply {
                                this.inlineResults = listOf(InlineQueryResultArticle("C${character.id}", character.name).apply {
                                    this.thumbUrl = character.images.getGrid()
                                    this.inputMessageContent = InputTextMessageContent(getCharacterContent(character)).apply {
                                        this.parseMode = ParseMode.MARKDOWN_V2
                                    }
                                })
                            }.send()
                        }.collectList()
                }
                else -> {
                    fallback(inlineQuery)
                }
            }
        }

    }

    private fun fallback(inlineQuery: InlineQuery): Mono<Boolean> {
        return getEmptyAnswer(inlineQuery.id).send()
    }
}