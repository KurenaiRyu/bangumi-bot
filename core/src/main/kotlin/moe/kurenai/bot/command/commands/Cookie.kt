package moe.kurenai.bot.command.commands

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.Message
import it.tdlight.jni.TdApi.MessageSenderUser
import moe.kurenai.bot.Config
import moe.kurenai.bot.TelegramBot
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.config.CookieVendor
import moe.kurenai.bot.service.BiliBiliService
import moe.kurenai.bot.util.FormattedTextBuilder
import moe.kurenai.bot.util.TelegramUtil
import moe.kurenai.bot.util.TelegramUtil.asText

@ContributesIntoSet(AppScope::class)
class Cookie : CommandHandler {

    override val command: String = "cookie"
    override val description: String = "更新cookie对应，不指定则默认为bilibili，不接cookie则进行查询。e.g. /cookie bilibili your_cookie; /cookie bilibili"

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        if (sender.userId != Config.CONFIG.telegram.masterId) return // Do no things
        if (TelegramBot.getChat(message.chatId)?.type?.constructor?.equals(TdApi.ChatTypePrivate.CONSTRUCTOR) ?: false) return

        when (args.size) {
            0 -> getCookie(CookieVendor.BILIBILI, message)
            1 -> {
                val cookieVendor = CookieVendor.ofName(args[0])
                if (cookieVendor == null) {
                    setCookie(CookieVendor.BILIBILI, message,args[0])
                } else {
                    getCookie(cookieVendor, message)
                }
            }
            2 -> {
                val cookieVendor = CookieVendor.ofName(args[0])

                if (cookieVendor == null) {
                    TelegramBot.send(
                        TelegramUtil.messageText(
                            message.chatId,
                            "Not support cookie vendor of name ${args[0]}".asText()
                        )
                    )
                    return
                }

                setCookie(cookieVendor, message, args[1])
            }
            else -> {
                TelegramBot.send(TelegramUtil.messageText(message.chatId, "Resolve argument error".asText()))
            }
        }
    }

    private suspend fun getCookie(cookieVendor: CookieVendor, message: Message) {
        val cookie = when (cookieVendor) {
            CookieVendor.BILIBILI -> BiliBiliService.cookieFlow.value
        }

        val msg = FormattedTextBuilder()
        msg.appendText("${cookieVendor.name} Cookie:")
            .appendLine()
            .appendCode(cookie)
        TelegramBot.sendMessage(message.chatId, msg.build())
    }

    private suspend fun setCookie(cookieVendor: CookieVendor, message: Message, cookie: String) {
        when (cookieVendor) {
            CookieVendor.BILIBILI -> {
                BiliBiliService.cookieFlow.emit(cookie)
            }
        }

        TelegramBot.sendMessage(message.chatId, "Update ${cookieVendor.name} cookie successfully".asText())
    }
}
