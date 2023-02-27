package moe.kurenai.bot.command.inlines

import io.ktor.http.*
import moe.kurenai.bgm.model.subject.getGrid
import moe.kurenai.bgm.model.subject.getMedium
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.repository.*
import moe.kurenai.bot.util.getEmptyAnswer
import moe.kurenai.bot.util.getLogger
import moe.kurenai.tdlight.model.ParseMode
import moe.kurenai.tdlight.model.inline.*
import moe.kurenai.tdlight.request.message.AnswerInlineQuery
import moe.kurenai.tdlight.util.MarkdownUtil.fm2md
import java.net.URI

object SearchByURI {

    private val log = getLogger()

    suspend fun execute(inlineQuery: InlineQuery, uri: URI) {
        when (uri.host) {
            "www.sakugabooru.com" -> handleSakuga(inlineQuery, uri)
            "b23.wtf" -> handleBiliBili(inlineQuery, URI.create(uri.toString().replace("b23.wtf", "b23.tv")))
            "b23.tv" -> handleBiliBiliShortLink(inlineQuery, uri)
            "www.bilibili.com" -> handleBiliBili(inlineQuery, uri)
            else -> {
                val params = uri.path.split("/")
                if (params.size != 3) {
                    fallback(inlineQuery)
                } else {
                    handleBgm(params, inlineQuery, uri)
                }
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

    private suspend fun handleBiliBili(inlineQuery: InlineQuery, uri: URI) {
        log.info("Handle BiliBili")
        // https://www.bilibili.com/video/BV1Fx4y1w78G/?p=1
        val url = Url(uri)
        val segments = Url(uri).pathSegments
        val id = segments.last().takeIf { it.isNotBlank() } ?: segments[segments.lastIndex - 1]
        val p = url.parameters["p"]?.toInt() ?: 1
        handleBiliBili(inlineQuery, id, p)
    }

    private suspend fun handleBiliBiliShortLink(inlineQuery: InlineQuery, uri: URI) {
        log.info("Handle BiliBili short link")
        val (id, p) = BiliBiliRepository.getIdAndPByShortLink(uri)
        handleBiliBili(inlineQuery, id, p)
    }

    private suspend fun handleBiliBili(inlineQuery: InlineQuery, id: String, p: Int) {
        val videoInfo = BiliBiliRepository.getVideoInfo(id)
        val desc = videoInfo.data.desc
        val page = videoInfo.data.pages.find { it.page == p } ?: run {
            fallback(inlineQuery)
            return
        }
        val streamInfo = BiliBiliRepository.getPlayUrl(videoInfo.data.bvid, page.cid)
        val link = "https://www.bilibili.com/video/${videoInfo.data.bvid}?p=$p"
        AnswerInlineQuery(inlineQuery.id).apply {
            this.inlineResults = listOf(InlineQueryResultVideo(videoInfo.data.bvid, videoInfo.data.title).apply {
                this.videoUrl = streamInfo.data.durl.first().url
                this.mimeType = MIMEType.MP4
                this.thumbUrl = videoInfo.data.pic
                this.caption =
                    "[${page.part.fm2md()}]($link)\nUP: [${videoInfo.data.owner.name.fm2md()}](https://space.bilibili.com/${videoInfo.data.owner.mid})\n\n${desc.fm2md()}"
                this.parseMode = ParseMode.MARKDOWN_V2
            })
        }.send()
    }

    private suspend fun fallback(inlineQuery: InlineQuery): Boolean {
        return getEmptyAnswer(inlineQuery.id).send()
    }
}
