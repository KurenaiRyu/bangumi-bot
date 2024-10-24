package moe.kurenai.bot.util

import it.tdlight.jni.TdApi
import moe.kurenai.bot.TelegramBot

/**
 * @author Kurenai
 * @since 2023/3/2 4:19
 */
object TelegramUtil {

    private val formatChar = "_*[]()~`>#+-=|{}.!".toCharArray()

    fun String.markdown(): String {
        var result = this
        for (c in formatChar) {
            result = result.replace(c.toString(), "\\$c")
        }
        return result
    }

    suspend inline fun String.fmt(mode: TdApi.TextParseModeMarkdown = TdApi.TextParseModeMarkdown(2)): TdApi.FormattedText =
        TelegramBot.send { TdApi.ParseTextEntities(this, mode) }

    fun String.asText() = TdApi.FormattedText().apply { text = this@asText }

    fun TdApi.TextEntity.text(text: String) = text.substring(this.offset, this.length)

    fun messageDocument(chatId: Long, url: String, msg: TdApi.FormattedText) = TdApi.SendMessage().apply {
        this.chatId = chatId
        inputMessageContent = TdApi.InputMessageDocument().apply {
            this.caption = msg
            this.document = TdApi.InputFileGenerated().apply {
                originalPath = url
            }
        }
    }

    fun messagePhoto(chatId: Long, photoPath: String, msg: TdApi.FormattedText) = TdApi.SendMessage().apply {
        this.chatId = chatId
        inputMessageContent = TdApi.InputMessagePhoto().apply {
            this.caption = msg
            this.photo = TdApi.InputFileLocal().apply {
                path = photoPath
            }
        }
    }

    fun messageAlbumPhoto(chatId: Long, photos: Map<String, String>) = TdApi.SendMessageAlbum().apply {
        this.chatId = chatId
        this.inputMessageContents = photos.map { (url, msg) ->
            TdApi.InputMessagePhoto().apply {
                this.caption = msg.asText()
                this.photo = TdApi.InputFileLocal().apply {
                    path = url
                }
            }
        }.toTypedArray()
    }

    fun messageAlbumPhoto(chatId: Long, pairs: List<Pair<String, String>>) = TdApi.SendMessageAlbum().apply {
        this.chatId = chatId
        this.inputMessageContents = pairs.map { (url, msg) ->
            TdApi.InputMessagePhoto().apply {
                this.caption = msg.asText()
                this.photo = TdApi.InputFileGenerated().apply {
                    originalPath = url
                }
            }
        }.toTypedArray()
    }

    fun <T : TdApi.InputInlineQueryResult> answerInlineQuery(
        id: Long,
        inlineQueryResults: Array<T>,
        cacheTime: Int = 60
    ) =
        TdApi.AnswerInlineQuery().apply {
            this.inlineQueryId = id
            this.results = inlineQueryResults
            this.cacheTime = cacheTime
        }

    fun answerInlineQueryEmpty(id: Long, cacheTime: Int = 60) = TdApi.AnswerInlineQuery().apply {
        this.inlineQueryId = id
        this.results = emptyArray()
        this.cacheTime = cacheTime
        this.button = TdApi.InlineQueryResultsButton().apply {
            this.text = "搜索结果为空"
            this.type = TdApi.InlineQueryResultsButtonTypeStartBot().apply {
                parameter = "help"
            }
        }
    }

    fun messageText(chatId: Long, msg: TdApi.FormattedText) = TdApi.SendMessage().apply {
        this.chatId = chatId
        inputMessageContent = TdApi.InputMessageText().apply {
            this.text = msg
        }
    }

    fun TdApi.Message.userSender(): TdApi.MessageSenderUser? = this.senderId as? TdApi.MessageSenderUser

    fun TdApi.MessageContent.file() = when (this) {
        is TdApi.MessageAnimation -> this.animation.animation
        is TdApi.MessageAudio -> this.audio.audio
        is TdApi.MessageDocument -> this.document.document
        is TdApi.MessagePhoto -> this.photo.sizes.maxBy { it.photo.size }.photo
        is TdApi.MessageSticker -> this.sticker.sticker
        is TdApi.MessageVideo -> this.video.video
        else -> null
    }

    fun TdApi.MessageContent.textOrCaption() = when (this) {
        is TdApi.MessageAnimation -> this.caption
        is TdApi.MessageAudio -> this.caption
        is TdApi.MessageDocument -> this.caption
        is TdApi.MessagePhoto -> this.caption
        is TdApi.MessageVideo -> this.caption
        is TdApi.MessageText -> this.text
        else -> null
    }

    fun TdApi.User.username() = this.usernames.activeUsernames.firstOrNull() ?: this.id.toString()

}
