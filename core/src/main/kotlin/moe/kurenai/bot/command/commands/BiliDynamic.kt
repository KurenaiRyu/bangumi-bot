package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi.*
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.TelegramBot.sendAlbumPhoto
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.service.BiliBiliService
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.TelegramUtil.trimCaption

class BiliDynamic : CommandHandler {

    override val command: String = "dynamic"
    override val description: String = "Bilibili 动态"

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        val url = args.firstOrNull() ?: return
        val id = url.substringAfterLast("/")
        val info = BiliBiliService.getDynamicDetail(id)

        val modules = if (info.data.item.orig != null) info.data.item.orig.modules else info.data.item.modules
        val summary = modules.moduleDynamic.major!!.opus.summary.text


        val builder = FormattedTextBuilder()
        builder.appendBold(info.data.item.modules.moduleAuthor.name)
            .appendText(" - ${info.data.item.modules.moduleAuthor.pubTime}:\nhttps://t.bilibili.com/${id}\n\n")
            .wrapQuoteIfNeeded {
                appendText(summary)
            }

        info.data.item.orig?.let { orig ->
            val quoteContent = orig.modules.moduleDynamic.major?.opus?.summary?.text ?: ""
            builder.appendLine().appendLine()
                .appendBold("Reply\n")
                .appendText("https://t.bilibili.com/${orig.idStr}\n")
                .wrapQuote {
                    appendBold(orig.modules.moduleAuthor.name)
                    appendText(" - ${orig.modules.moduleAuthor.pubTime}:\n\n$quoteContent")
                }
        }

        val caption = builder.build()

        if (modules.moduleDynamic.major.opus.pics.isNotEmpty()) {
            val list = modules.moduleDynamic.major.opus.pics.mapIndexed { index, pic ->
                pic.url to caption.takeIf { index == 0 }
            }
            sendAlbumPhoto(message.chatId, list)
        } else {
            send {
                SendMessage().apply {
                    this.chatId = chatId
                    this.inputMessageContent = InputMessageText().apply {
                        this.text = caption.trimCaption()
                        this.linkPreviewOptions = LinkPreviewOptions().apply {
                            this.isDisabled = true
                        }
                    }
                }
            }
        }
    }
}
