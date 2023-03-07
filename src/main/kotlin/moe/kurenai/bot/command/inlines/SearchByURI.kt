package moe.kurenai.bot.command.inlines

import com.elbekd.bot.types.InlineQuery
import com.elbekd.bot.types.InlineQueryResultPhoto
import com.elbekd.bot.types.InlineQueryResultVideo
import com.elbekd.bot.types.ParseMode
import io.ktor.http.*
import moe.kurenai.bot.BangumiBot.telegram
import moe.kurenai.bot.repository.*
import moe.kurenai.bot.util.MimeTypes
import moe.kurenai.bot.util.TelegramUtil
import moe.kurenai.bot.util.TelegramUtil.fm2md
import moe.kurenai.bot.util.getLogger
import java.net.URI
import kotlin.math.roundToInt

object SearchByURI {

    private val log = getLogger()

    suspend fun execute(inlineQuery: InlineQuery, uri: URI) {
        when (uri.host) {
            "www.sakugabooru.com" -> handleSakugabooru(inlineQuery, uri)
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

    private suspend fun handleBgm(params: List<String>, inlineQuery: InlineQuery, uri: URI) {
        val id = params[2].toInt()
        val userId = inlineQuery.from.id
        val token = TokenRepository.findById(userId)?.accessToken
        when (params[1]) {
            "subject" -> {
                SubjectRepository.findById(id, token).let { sub ->
                    telegram.answerInlineQuery(
                        inlineQuery.id,
                        SubjectRepository.getContent(sub, uri.toString())
                    )
                }
            }

            "person" -> {
                PersonRepository.findById(id, token).let { person ->
                    telegram.answerInlineQuery(
                        inlineQuery.id,
                        PersonRepository.getContent(person, uri.toString())
                    )
                }
            }

            "character" -> {
                val character = CharacterRepository.findById(id, token)
                val persons = CharacterRepository.findPersons(id, token)
                telegram.answerInlineQuery(
                    inlineQuery.id,
                    CharacterRepository.getContent(character, uri.toString(), persons)
                )
            }

            else -> {
                fallback(inlineQuery)
            }
        }
    }

    private suspend fun handleSakugabooru(inlineQuery: InlineQuery, uri: URI) {
        log.info("Handle Sakugabooru")
        val params = uri.path.split("/")
        if (params.size == 4) {
            if (params[1] == "post" && params[2] == "show") {
                val id = params[3]
                kotlin.runCatching {
                    telegram.answerInlineQuery(
                        inlineQuery.id,
                        results = listOf(SakugabooruRepository.findOne(id, uri))
                    )
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
        val up = "UP: [${videoInfo.data.owner.name.fm2md()}](https://space.bilibili.com/${videoInfo.data.owner.mid})"
        val playCount = "${((videoInfo.data.stat.view / 10.0).roundToInt() / 100.0).toString().fm2md()}K 播放"
        val partTitle = if (videoInfo.data.pages.size == 1 || page.part == "1") "" else "/ ${page.part.fm2md()}"
        val rank = if (videoInfo.data.stat.nowRank == 0) "" else "/ ${videoInfo.data.stat.nowRank} 名 / 历史最高 ${videoInfo.data.stat.nowRank} 名"
        val content = "[${videoInfo.data.title.fm2md()}]($link) $partTitle" +
            "\n\n$up / $playCount $rank" +
            "\n\n${desc.fm2md()}"
        telegram.answerInlineQuery(
            inlineQuery.id,
            listOf(
                InlineQueryResultVideo(
                    id = "${videoInfo.data.bvid} - video",
                    title = videoInfo.data.title,
                    videoUrl = streamInfo.data.durl.first().url,
                    thumbUrl = videoInfo.data.pic,
                    mimeType = MimeTypes.Video.MP4,
                    caption = content,
                    parseMode = ParseMode.MarkdownV2
                ),
                InlineQueryResultPhoto(
                    id = "${videoInfo.data.bvid} - pic",
                    title = videoInfo.data.title,
                    photoUrl = videoInfo.data.pic,
                    thumbUrl = videoInfo.data.pic,
                    caption = content,
                    parseMode = ParseMode.MarkdownV2
                )
            )
        )
    }

    private suspend fun fallback(inlineQuery: InlineQuery): Boolean {
        return TelegramUtil.answerInlineQueryEmpty(inlineQuery)
    }
}
