package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi.*
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.TelegramBot.sendAlbumPhoto
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.service.BiliBiliService
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.TelegramUtil.trimCaption
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class BiliDynamic : CommandHandler {

    override val command: String = "dynamic"
    override val description: String = "Bilibili 动态"

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        val url = args.firstOrNull() ?: return
        val id = url.substringAfterLast("/")
        val info = BiliBiliService.getDynamicDetail(id)
        val modules = info.data.item.modules
        val moduleDynamic = modules.moduleDynamic

        val content = moduleDynamic.major?.opus?.summary?.text ?: moduleDynamic.desc?.text ?: ""
        val pubTime = LocalDateTime.ofEpochSecond(modules.moduleAuthor.pubTs, 0, ZoneOffset.ofHours(8))
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val builder = FormattedTextBuilder()
        builder.appendBold(modules.moduleAuthor.name)
            .appendText(" - ${pubTime}:\n\n")
            .wrapQuoteIfNeeded {
                appendText(content)
            }

        info.data.item.orig?.let { orig ->
            val quoteContent = orig.modules.moduleDynamic.major?.opus?.summary?.text ?: ""
            val pubTime = LocalDateTime.ofEpochSecond(orig.modules.moduleAuthor.pubTs, 0, ZoneOffset.ofHours(8))
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            builder.wrapQuote {
                appendBold(orig.modules.moduleAuthor.name)
                appendText(" - ${pubTime}:\n\n$quoteContent\n\nhttps://t.bilibili.com/${orig.idStr}")
            }
        }
        val formattedText = builder.appendText("\nhttps://t.bilibili.com/${id}").build()

        if (moduleDynamic.major!!.opus.pics.isNotEmpty()) {
            val list = modules.moduleDynamic.major.opus.pics.mapIndexed { index, pic ->
                pic.url to formattedText.takeIf { index == 0 }
            }
            sendAlbumPhoto(message.chatId, list)
        } else {
            send {
                SendMessage().apply {
                    this.chatId = chatId
                    this.inputMessageContent = InputMessageText().apply {
                        this.text = formattedText.trimCaption()
                        this.linkPreviewOptions = LinkPreviewOptions().apply {
                            this.isDisabled = true
                        }
                    }
                }
            }
        }
    }
}
