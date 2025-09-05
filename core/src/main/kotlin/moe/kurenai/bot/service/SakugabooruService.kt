package moe.kurenai.bot.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bot.command.InlineDispatcher
import moe.kurenai.bot.model.sakugabooru.Note
import moe.kurenai.bot.model.sakugabooru.Post
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.TelegramUtil.trimCaption
import moe.kurenai.common.util.MimeTypes
import moe.kurenai.common.util.json
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.ArrayList

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
internal object SakugabooruService {

    val client = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Cookie }
        }

    }

    suspend fun findPostById(id: String): InputInlineQueryResult? {
        val bodyText = client.get("https://www.sakugabooru.com/post.json?tags=id:$id").bodyAsText()

        val post = json.decodeFromString<Array<Post>>(bodyText)
            .firstOrNull()?:return null

        val dateTime = LocalDateTime.ofEpochSecond(post.updatedAt, 0, ZoneOffset.ofHours(8))
            .format(InlineDispatcher.DATE_TIME_PATTERN)

        val builder = FormattedTextBuilder()
            .appendLink(id, "https://www.sakugabooru.com/post/show/$id")
            .appendText(" / ${post.author} / $dateTime")
            .appendLine().appendLine()

        val notes: Array<Note> = json.decodeFromString(client.get("https://www.sakugabooru.com/note.json?post_id=$id").bodyAsText())
        if (notes.isNotEmpty()) {
            val noteContent = notes.joinToString("\n\n") { it.body }
            builder.appendText(noteContent)
            builder.appendLine().appendLine()
        }

        if (post.source.isNotBlank()) {
            builder.appendBold("source")
                .appendText(": ${post.source}")
        }

        builder.wrapQuote {
            for (tag in post.tags.split(" ")) {
                appendText("- ")
                appendLink(tag, "https://www.sakugabooru.com/post?tags=$tag")
                appendLine()
            }
        }

        val formattedText = builder.build()

        return when (post.fileExt) {
            "mp4" -> {
                InputInlineQueryResultVideo().apply {
                    this.id = "SAKUGA-POST-$id"
                    title = id
                    videoUrl = post.fileUrl
                    videoWidth = post.width
                    videoHeight = post.height
                    mimeType = MimeTypes.Video.MP4
                    thumbnailUrl = post.previewUrl
                    inputMessageContent = InputMessageVideo().apply {
                        this.caption = formattedText.trimCaption()
                    }
                }
            }
            "jpg" -> {
                InputInlineQueryResultPhoto().apply {
                    this.id = "SAKUGA-POST-$id"
                    title = id
                    photoUrl = post.fileUrl
                    photoWidth = post.width
                    photoHeight = post.height
                    thumbnailUrl = post.previewUrl
                    inputMessageContent = InputMessageVideo().apply {
                        this.caption = formattedText.trimCaption()
                    }
                }
            }
            else -> null
        }
    }

}
