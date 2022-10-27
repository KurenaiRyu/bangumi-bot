package moe.kurenai.bot.command.inlines

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import moe.kurenai.bgm.model.subject.getGrid
import moe.kurenai.bgm.model.subject.getMedium
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.BangumiBot.getCharacterContent
import moe.kurenai.bot.BangumiBot.getEmptyAnswer
import moe.kurenai.bot.BangumiBot.getPersonContent
import moe.kurenai.bot.BangumiBot.getSubjectContent
import moe.kurenai.bot.BangumiBot.redisson
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.tdlight.model.ParseMode
import moe.kurenai.tdlight.model.inline.*
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.AnswerInlineQuery
import moe.kurenai.tdlight.util.MarkdownUtil.fm2md
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import java.net.URI
import java.time.Duration

class SearchByURI {

    companion object {
        private val log = LogManager.getLogger()
    }

    suspend fun execute(update: Update, inlineQuery: InlineQuery, uri: URI) {
        if (uri.host == "www.sakugabooru.com") {
            handleSakuga(update, inlineQuery, uri)
        } else {
            val params = uri.path.split("/")
            if (params.size != 3) {
                fallback(inlineQuery)
            } else {
                val ids = listOf(params[2].toInt())
                when (params[1]) {
                    "subject" -> {
                        BangumiBot.getSubjects(ids).collect { sub ->
                            AnswerInlineQuery(inlineQuery.id).apply {
                                this.inlineResults = listOf(InlineQueryResultArticle("S${sub.id}", sub.name).apply {
                                    this.thumbUrl = sub.images.getMedium()
                                    val (content, entities) = getSubjectContent(sub, uri.toString())
                                    this.inputMessageContent = InputTextMessageContent(content).apply {
                                        this.entities = entities
                                    }
                                })
                            }.send()
                        }
                    }

                    "person" -> {
                        BangumiBot.getPersons(ids).collect { person ->
                            AnswerInlineQuery(inlineQuery.id).apply {
                                this.inlineResults = listOf(InlineQueryResultArticle("P${person.id}", person.name).apply {
                                    this.thumbUrl = person.images.getGrid()
                                    val (content, entities) = getPersonContent(person, uri.toString())
                                    this.inputMessageContent = InputTextMessageContent(content).apply {
                                        this.entities = entities
                                    }
                                })
                            }.send()
                        }
                    }

                    "character" -> {
                        BangumiBot.getCharacters(ids).collect { character ->
                            AnswerInlineQuery(inlineQuery.id).apply {
                                this.inlineResults = listOf(InlineQueryResultArticle("C${character.id}", character.name).apply {
                                    this.thumbUrl = character.images.getGrid()
                                    val (content, entities) = getCharacterContent(character, uri.toString())
                                    this.inputMessageContent = InputTextMessageContent(content).apply {
                                        this.entities = entities
                                    }
                                })
                            }.send()
                        }
                    }

                    else -> {
                        fallback(inlineQuery)
                    }
                }
            }
        }
    }

    private suspend fun handleSakuga(update: Update, inlineQuery: InlineQuery, uri: URI) {
        log.info("Handle Sakugabooru")
        val params = uri.path.split("/")
        if (params.size == 4) {
            if (params[1] == "post" && params[2] == "show") {
                val id = params[3]
                HttpClient().use { client ->
                    kotlin.runCatching {
                        val bucket = redisson.getBucket<List<InlineQueryResult>>("SAKUGA:POST:$id")
                        val results = bucket.get() ?: client.get(uri.toURL()).bodyAsText().let { html ->
                            val doc = Jsoup.parse(html)
                            val url = doc.select("video source").attr("src")
                            if (url.isBlank()) throw IllegalStateException("Cannot found video url")
                            val artist = doc.select(".tag-type-artist")
                                .joinToString("\n") { " \\- [${it.child(1).text().fm2md()}](${uri.host}${it.child(1).attr("href")}) ${it.child(2).text()}" }
                            val copyright = doc.select(".tag-type-copyright")
                                .joinToString("\n") { " \\- [${it.child(1).text().fm2md()}](${uri.host}${it.child(1).attr("href")}) ${it.child(2).text()}" }

                            listOf(InlineQueryResultVideo("SAKUGA-POST-$id", id).apply {
                                this.videoUrl = url
                                this.mimeType = MIMEType.MP4
                                this.thumbUrl = "https://www.sakugabooru.com/data/preview/" + url.substringAfterLast('/').substringBeforeLast('.') + ".jpg"
                                this.caption = "[$id](${uri})\nArtist\n$artist\nCopyright\n$copyright"
                                this.parseMode = ParseMode.MARKDOWN_V2
                            }).also {
                                bucket.set(it)
                                bucket.expire(Duration.ofDays(7))
                            }
                        }
                        AnswerInlineQuery(inlineQuery.id).apply {
                            this.inlineResults = results
                        }.send()
                    }.onFailure {
                        log.error(it.message, it)
                        fallback(inlineQuery)
                    }
                }
            }
        }
    }

    private suspend fun fallback(inlineQuery: InlineQuery): Boolean {
        return getEmptyAnswer(inlineQuery.id).send()
    }
}