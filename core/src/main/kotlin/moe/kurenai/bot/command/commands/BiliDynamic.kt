package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi.*
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.TelegramBot.sendAlbumPhoto
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.service.BiliBiliService
import moe.kurenai.bot.util.TelegramUtil.asText

class BiliDynamic : CommandHandler {

    override val command: String = "dynamic"
    override val description: String = "Bilibili 动态"

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        val url = args.firstOrNull() ?: return
        val id = url.substringAfterLast("/")
        val info = BiliBiliService.getDynamicDetail(id)

        val modules = if (info.data.item.orig != null) info.data.item.orig.modules else info.data.item.modules
        val summary = modules.moduleDynamic.major!!.opus.summary.text

        val caption =
            "${modules.moduleAuthor.name} - ${modules.moduleAuthor.pubTime}:\n\n$summary\n\nhttps://t.bilibili.com/${id}".asText()

        info.data.item.orig?.let { orig ->
            val quoteContent = orig.modules.moduleDynamic.major?.opus?.summary?.text ?: ""
            val quoteSummary =
                "${orig.modules.moduleAuthor.name} - ${orig.modules.moduleAuthor.pubTime}:\n\n$quoteContent\n\nhttps://t.bilibili.com/${orig.idStr}"
            val start = caption.text.length
            caption.entities += TextEntity().apply {
                this.offset = start
                this.length = quoteSummary.length
                this.type = TextEntityTypeBlockQuote()
            }
            caption.text += quoteSummary
        }

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
                        this.text = caption
                        this.linkPreviewOptions = LinkPreviewOptions().apply {
                            this.isDisabled = true
                        }
                    }
                }
            }
        }
    }
}
