package moe.kurenai.bot.command.inlines

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.TelegramUserBot
import moe.kurenai.bot.repository.*
import moe.kurenai.bot.util.MimeTypes
import moe.kurenai.bot.util.TelegramUtil.answerInlineQuery
import moe.kurenai.bot.util.TelegramUtil.answerInlineQueryEmpty
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import moe.kurenai.bot.util.getLogger
import moe.kurenai.bot.util.limit
import java.net.URI
import kotlin.math.roundToInt

object SearchByURI {

    private val log = getLogger()

    suspend fun execute(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        when (uri.host) {
            "www.sakugabooru.com",
            "sakugabooru.com" -> handleSakugabooru(inlineQuery, uri)

            "b23.wtf" -> handleBiliBiliShortLink(inlineQuery, URI.create(uri.toString().replace("b23.wtf", "b23.tv")))
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

    private suspend fun handleBgm(params: List<String>, inlineQuery: UpdateNewInlineQuery, uri: URI) {
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

    private suspend fun handleSakugabooru(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle Sakugabooru")
        val params = uri.path.split("/")
        if (params.size == 4) {
            if (params[1] == "post" && params[2] == "show") {
                val id = params[3]
                kotlin.runCatching {
                    send { answerInlineQuery(inlineQuery.id, arrayOf(SakugabooruRepository.findOne(id, uri))) }
                }.onFailure {
                    log.error(it.message, it)
                    fallback(inlineQuery)
                }
            }
        }
    }

    private suspend fun handleBiliBili(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle BiliBili")
        // https://www.bilibili.com/video/BV1Fx4y1w78G/?p=1
        val url = Url(uri)
        val segments = Url(uri).pathSegments
        val id = segments.last().takeIf { it.isNotBlank() } ?: segments[segments.lastIndex - 1]
        val p = url.parameters["p"]?.toInt() ?: 1
        handleBiliBili(inlineQuery, id, p)
    }

    private suspend fun handleBiliBiliShortLink(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle BiliBili short link")
        val (id, p) = BiliBiliRepository.getIdAndPByShortLink(uri)
        handleBiliBili(inlineQuery, id, p)
    }

    private suspend fun handleBiliBili(inlineQuery: UpdateNewInlineQuery, id: String, p: Int) {
        val videoInfo = BiliBiliRepository.getVideoInfo(id)
        val desc = videoInfo.data.desc.limit()
        val page = videoInfo.data.pages.find { it.page == p } ?: run {
            fallback(inlineQuery)
            return
        }
        val streamInfo = BiliBiliRepository.getPlayUrl(videoInfo.data.bvid, page.cid)
        val link = "https://www.bilibili.com/video/${videoInfo.data.bvid}?p=$p"
        val up = "UP: [${videoInfo.data.owner.name.markdown()}](https://space.bilibili.com/${videoInfo.data.owner.mid})"
        val playCount = "${((videoInfo.data.stat.view / 10.0).roundToInt() / 100.0).toString().markdown()}K 播放"
        val partTitle = if (videoInfo.data.pages.size == 1 || page.part == "1") "" else "/ ${page.part.markdown()}"
        val rank =
            if (videoInfo.data.stat.nowRank == 0) "" else "/ ${videoInfo.data.stat.nowRank} 名 / 历史最高 ${videoInfo.data.stat.nowRank} 名"
        val content = ("[${videoInfo.data.title.markdown()}]($link) $partTitle" +
            "\n\n$up / $playCount $rank" +
            "\n\n${desc.markdown()}").fmt()
        send {
            TelegramUserBot.fetchRemoteFile(videoInfo.data.pic) ?: run {
                log.warn("Fetch video image fail.")
            }
            answerInlineQuery(inlineQuery.id, arrayOf(
                InputInlineQueryResultVideo().apply {
                    this.id = "${videoInfo.data.bvid} - video"
                    title = videoInfo.data.title
                    videoUrl = streamInfo.data.durl.first().url
                    thumbnailUrl = videoInfo.data.pic
                    mimeType = MimeTypes.Video.MP4
                    inputMessageContent = InputMessageVideo().apply {
                        this.caption = content
                    }
                },
                InputInlineQueryResultPhoto().apply {
                    this.id = "${videoInfo.data.bvid} - photo"
                    title = videoInfo.data.title
                    photoUrl = videoInfo.data.pic
                    thumbnailUrl = videoInfo.data.pic
                    inputMessageContent = InputMessagePhoto().apply {
                        this.caption = content
                    }
                }
            ))
        }
    }

    private suspend fun fallback(inlineQuery: UpdateNewInlineQuery): Boolean {
        return send(answerInlineQueryEmpty(inlineQuery.id)) is Ok
    }
}
