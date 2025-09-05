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

        for (tag in post.tags.split(" ")) {
            builder.appendText("- $tag")
            builder.appendLine()
        }

        val formattedText = builder.build()

        return InputInlineQueryResultVideo().apply {
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

}
