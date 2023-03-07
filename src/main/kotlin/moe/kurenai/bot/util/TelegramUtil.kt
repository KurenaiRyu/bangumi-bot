package moe.kurenai.bot.util

import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.InlineQuery
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.MessageEntity
import moe.kurenai.bot.BangumiBot

/**
 * @author Kurenai
 * @since 2023/3/2 4:19
 */
object TelegramUtil {

    private val formatChar = "_*[]()~`>#+-=|{}.!".toCharArray()

    fun String.fm2md(): String {
        var result = this
        for (c in formatChar) {
            result = result.replace(c.toString(), "\\$c")
        }
        return result
    }

    fun Message.chatId() = this.chat.id.toChatId()

    suspend fun answerInlineQueryEmpty(inlineQuery: InlineQuery): Boolean {
        return BangumiBot.telegram.answerInlineQuery(
            inlineQuery.id, emptyList(),
            switchPmText = "搜索结果为空",
            switchPmParameter = "help"
        )
    }

    fun MessageEntity.text(message: Message) = message.text?.let { text(it) }

    fun MessageEntity.text(text: String) = text.substring(this.offset, this.offset + this.length)

}
