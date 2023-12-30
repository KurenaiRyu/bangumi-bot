package moe.kurenai.bot.command.commands

import it.tdlight.jni.TdApi.Message
import it.tdlight.jni.TdApi.MessageSenderUser
import moe.kurenai.bgm.exception.BgmException
import moe.kurenai.bgm.model.error.UnauthorizedError
import moe.kurenai.bgm.request.user.GetMe
import moe.kurenai.bot.BangumiBot.bgmClient
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.BgmAuthServer
import moe.kurenai.bot.TelegramBot.send
import moe.kurenai.bot.TelegramBot.sendPhoto
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.TokenRepository
import moe.kurenai.bot.util.TelegramUtil.asText
import moe.kurenai.bot.util.TelegramUtil.fmt
import moe.kurenai.bot.util.TelegramUtil.markdown
import moe.kurenai.bot.util.TelegramUtil.messageText
import moe.kurenai.bot.util.getLogger

class Start : CommandHandler {
    override val command: String = "start"
    override val description: String = "绑定用户"

    companion object {
        private val log = getLogger()
    }

    override suspend fun execute(message: Message, sender: MessageSenderUser, args: List<String>) {
        val userId = sender.userId
        kotlin.runCatching {
            TokenRepository.findById(userId)?.also { token ->
                val me = GetMe().apply { this.token = token.accessToken }.send()
                sendPhoto(
                    message.chatId,
                    me.avatar.large,
                    "已绑定`${me.nickname.markdown()}`\\(`${me.username.takeIf { it.isNotBlank() } ?: me.id}`\\)".fmt())
            } ?: kotlin.run {
                doBind(userId, message)
            }
        }.recover {
            log.error(it.message, it)
            if (it is BgmException) {
                val error = it.error
                if (error is UnauthorizedError) {
                    doBind(userId, message)
                } else {
                    send {
                        messageText(
                            message.chatId,
                            "请求bgm异常: [${error.code}] ${error.error} ${it.message ?: ""}".asText()
                        )
                    }
                }
            } else {
                send { messageText(message.chatId, "Bot内部异常".asText()) }
            }
        }.onFailure {
            log.error("Get me $userId error: ${it.message}")
        }
    }

    private suspend fun doBind(userId: Long, message: Message) {
        send {
            messageText(
                message.chatId,
                "请点击该[链接](${bgmClient.getOauthUrl(BgmAuthServer.genRandomCode(userId)).markdown()})进行授权".fmt()
            )
        }
    }
}
