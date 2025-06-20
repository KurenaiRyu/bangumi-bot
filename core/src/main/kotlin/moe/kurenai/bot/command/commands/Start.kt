package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi.Message
import it.tdlight.jni.TdApi.MessageSenderUser
import moe.kurenai.bot.BgmAuthServer
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.TelegramBot.sendPhoto
import moe.kurenai.bot.command.CommandDispatcher
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.service.bangumi.TokenService
import moe.kurenai.bot.service.bangumi.UserService
import moe.kurenai.bot.util.BgmUtil
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import moe.kurenai.bot.util.TelegramUtil.messageText
import moe.kurenai.common.util.getLogger

class Start : CommandHandler {
    override val command: String = "start"
    override val description: String = "绑定用户"

    companion object {
        private val log = getLogger()
    }

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        if (args.isNotEmpty()) {
            val param = args.first()
            CommandDispatcher.commands.values.firstOrNull { c ->
                param.startsWith(c.command)
            }?.let { c ->
                c.execute(message, sender, listOf(param.removePrefix(c.command)))
                return
            }
        }

        val userId = sender.userId
        kotlin.runCatching {
            TokenService.findById(userId)?.also { token ->
                val me = UserService.getMe(token.accessToken)
                sendPhoto(
                    message.chatId,
                    me.avatar.large,
                    "已绑定`${me.nickname.markdown()}`\\(`${me.username.takeIf { it.isNotBlank() } ?: me.id}`\\)".fmt())
            } ?: kotlin.run {
                doBind(userId, message)
            }
        }.recover {
            log.error(it.message, it)
            send { messageText(message.chatId, "Bot内部异常".asText()) }
        }.onFailure {
            log.error("Get me $userId error: ${it.message}")
        }
    }

    private suspend fun doBind(userId: Long, message: Message) {
        send {
            messageText(
                message.chatId,
                "请点击该[链接](${BgmUtil.buildOauthUrl(BgmAuthServer.genRandomCode(userId)).markdown()})进行授权".fmt()
            )
        }
    }
}
