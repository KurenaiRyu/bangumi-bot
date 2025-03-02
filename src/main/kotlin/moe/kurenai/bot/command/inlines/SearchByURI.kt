package moe.kurenai.bot.command.inlines

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.repository.*
import moe.kurenai.bot.util.MimeTypes
import moe.kurenai.bot.util.TelegramUtil.answerInlineQuery
import moe.kurenai.bot.util.TelegramUtil.answerInlineQueryEmpty
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import moe.kurenai.bot.util.getLogger
import moe.kurenai.bot.util.trimString
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

object SearchByURI {

    private val log = getLogger()
    private val PUB_DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun execute(inlineQuery: UpdateNewInlineQuery, uri: URI) {

        val host = uri.host

        if (CONFIG.bilibili.shortLinkHost.contains(host)) {
            handleBiliBiliShortLink(inlineQuery, uri)
            return
        }

        when (host) {
            "www.sakugabooru.com",
            "sakugabooru.com" -> handleSakugabooru(inlineQuery, uri)

            "www.bilibili.com" -> handleBiliBili(inlineQuery, uri)
            "t.bilibili.com" -> handleBiliDynamic(inlineQuery, uri)
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

    private suspend fun handleBiliDynamic(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle bilibili dynamic: $uri")
        val id = uri.path.substringAfterLast("/").takeIf { it.isNotBlank() }?: run {
            fallback(inlineQuery)
            return
        }
        val info = BiliBiliRepository.getDynamicDetail(id)
        val moduleDynamic = info.data.item.modules.moduleDynamic

        val content = moduleDynamic.major?.opus?.summary?.text?: moduleDynamic.desc?.text?: ""
        val summary =
            "${info.data.item.modules.moduleAuthor.name} - ${info.data.item.modules.moduleAuthor.pubTime}:\n\n$content\n\nhttps://t.bilibili.com/${id}"

        val caption = summary.markdown().fmt()

        info.data.item.orig?.let { orig ->
            val quoteContent = orig.modules.moduleDynamic.major?.opus?.summary?.text ?: ""
            val quoteSummary =
                "${orig.modules.moduleAuthor.name} - ${orig.modules.moduleAuthor.pubTime}:\n\n$quoteContent\n\nhttps://t.bilibili.com/${orig.idStr}"
            val start = caption.text.length
            caption.entities +=  TextEntity().apply {
                this.offset = start
                this.length = quoteSummary.length
                this.type = TextEntityTypeBlockQuote()
            }
            caption.text += quoteSummary
        }

        val items = moduleDynamic.major?.opus?.pics?.mapIndexed { index, pic ->
            InputInlineQueryResultPhoto().apply {
                this.id = index.toString()
                title = "${info.data.item.modules.moduleAuthor.name} ${info.data.item.modules.moduleAuthor.pubTime}[$index]"
                thumbnailUrl = pic.url + "@240w_!web-dynamic.webp"
                photoUrl = pic.url + "@1920w_!web-dynamic.webp"
                photoWidth = pic.width
                photoHeight = pic.height
                inputMessageContent = InputMessagePhoto().apply {
                    this.caption = caption
                }
            }
        }?.toTypedArray()?.takeIf { it.isNotEmpty() }
            ?: arrayOf(InputInlineQueryResultArticle().apply {
            this.id = "dynamic$id"
            this.title = "${info.data.item.modules.moduleAuthor.name} ${info.data.item.modules.moduleAuthor.pubTime}"
                this.inputMessageContent = InputMessageText().apply {
                    this.text = caption
            }
        })

        send {
            answerInlineQuery(inlineQuery.id, items).apply {
                if ((moduleDynamic.major?.opus?.pics?.size ?: 0) > 1) {
                    this.button = InlineQueryResultsButton().apply {
                        this.text = "合并图片为一条消息"
                        this.type = InlineQueryResultsButtonTypeStartBot().apply {
                            parameter = "dynamic$id"
                        }
                    }
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
        if (uri.path.contains("opus")) {
            handleBiliDynamic(inlineQuery, uri)
            return
        }

        // https://www.bilibili.com/video/BV1Fx4y1w78G/?p=1
        val url = Url(uri)
        val segments = Url(uri).rawSegments
        val id = segments.last().takeIf { it.isNotBlank() } ?: segments[segments.lastIndex - 1]
        val p = url.parameters["p"]?.toLong() ?: 1L
        handleBiliBili(inlineQuery, id, p)
    }

    private suspend fun handleBiliBiliShortLink(inlineQuery: UpdateNewInlineQuery, uri: URI) {
        log.info("Handle BiliBili short link")
        val redirectUrl = BiliBiliRepository.getRedirectUrl(uri)

        if (redirectUrl.rawSegments.contains("opus") || redirectUrl.host == "t.bilibili.com") {
            handleBiliDynamic(inlineQuery, redirectUrl.toURI())
            return
        }

        val (id, p) = BiliBiliRepository.getIdAndPByShortLink(uri, redirectUrl)
        handleBiliBili(inlineQuery, id, p)
    }

    private suspend fun handleBiliBili(inlineQuery: UpdateNewInlineQuery, id: String, p: Long) {
        log.info("Handle bilibili: $id, $p")

        val videoInfo = BiliBiliRepository.getVideoInfo(id)
        val desc = videoInfo.data.desc.trimString()
        val page = videoInfo.data.pages.find { it.page == p } ?: run {
            fallback(inlineQuery)
            return
        }
        val streamInfo = BiliBiliRepository.getPlayUrl(videoInfo.data.bvid, page.cid)
        val link = "https://www.bilibili.com/video/${videoInfo.data.bvid}?p=$p"
        val up = "UP: [${videoInfo.data.owner.name.markdown()}](https://space.bilibili.com/${videoInfo.data.owner.mid})"
        val playCount = "${((videoInfo.data.stat.view / 10.0).roundToInt() / 100.0).toString().markdown()}K 播放"
        val contentTitle = if (page.part.contains(videoInfo.data.title)) {
            "[${page.part.markdown()}]($link)"
        } else {
            "[${videoInfo.data.title.markdown()}]($link) / ${page.part.markdown()}"
        }
        val rank =
            if (videoInfo.data.stat.nowRank == 0) "" else "/ ${videoInfo.data.stat.nowRank} 名 / 历史最高 ${videoInfo.data.stat.nowRank} 名"
        val createDate = LocalDateTime.ofEpochSecond(videoInfo.data.pubdate.toLong(), 0, ZoneOffset.ofHours(8))
            .format(PUB_DATE_PATTERN)
            .markdown()
        val duration = videoInfo.data.duration.seconds.toString(DurationUnit.MINUTES, 2).markdown()
        val content = (contentTitle +
            "\n\n$up / $playCount $rank / $createDate / $duration" +
            "\n\n${desc.markdown()}").fmt()

        val results = mutableListOf<InputInlineQueryResult>()
        results.add(InputInlineQueryResultPhoto().apply {
            this.id = "P_${videoInfo.data.bvid}_$p"
            this.title = "${page.part}/${videoInfo.data.title}"
            photoUrl = videoInfo.data.pic
            thumbnailUrl = videoInfo.data.pic
            inputMessageContent = InputMessagePhoto().apply {
                this.caption = content
            }
        })
        streamInfo.data?.run {
            results.add(InputInlineQueryResultVideo().apply {
                this.id = "V_${videoInfo.data.bvid}_$p"
                this.title = "${page.part}/${videoInfo.data.title}"
                videoUrl = streamInfo.data.durl!!.first().url
                thumbnailUrl = videoInfo.data.pic
                mimeType = MimeTypes.Video.MP4
                inputMessageContent = InputMessageVideo().apply {
                    this.caption = content
                }
            })
        }
        send {
//            TelegramUserBot.fetchRemoteFileIdByUrl(videoInfo.data.pic) ?: run {
//                log.warn("Fetch video image fail.")
//            }

            answerInlineQuery(inlineQuery.id, results.toTypedArray())
        }
    }

    private suspend fun fallback(inlineQuery: UpdateNewInlineQuery): Boolean {
        return send(answerInlineQueryEmpty(inlineQuery.id)) is Ok
    }
}
