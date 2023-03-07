package moe.kurenai.bot.command.commands

import com.elbekd.bot.types.Message
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.UpdateMessage
import com.elbekd.bot.util.SendingString
import moe.kurenai.bgm.exception.BgmException
import moe.kurenai.bgm.exception.UnauthorizedException
import moe.kurenai.bgm.request.user.GetMe
import moe.kurenai.bot.BangumiBot.bgmClient
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.BangumiBot.telegram
import moe.kurenai.bot.BgmAuthServer
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.token
import moe.kurenai.bot.util.TelegramUtil.chatId
import moe.kurenai.bot.util.TelegramUtil.fm2md
import moe.kurenai.bot.util.getLogger

@Command(command = "start")
class Start : CommandHandler {

    companion object {
        private val log = getLogger()
    }

    override suspend fun execute(update: UpdateMessage, message: Message, args: List<String>) {
        val userId = message.from!!.id
        kotlin.runCatching {
            message.token()?.also { token ->
                val me = GetMe().apply { this.token = token.accessToken }.send()
                telegram.sendPhoto(message.chatId(), SendingString(me.avatar.large),
                    caption = "已绑定`${me.nickname.fm2md()}`\\(`${me.username.takeIf { it.isNotBlank() } ?: me.id}`\\)",
                    parseMode = ParseMode.MarkdownV2
                )
            } ?: kotlin.run {
                doBind(userId, message)
            }
        }.recoverCatching {
            if (it is BgmException) {
                if (it is UnauthorizedException) {
                    doBind(userId, message)
                } else {
                    telegram.sendMessage(message.chatId(), "请求bgm异常: [${it.code}] ${it.error} ${it.message ?: ""}")
                }
            } else {
                telegram.sendMessage(message.chatId(), "Bot内部异常")
            }
        }.onFailure {
            log.error("Get me $userId error: ${it.message}")
        }
    }

    private suspend fun doBind(userId: Long, message: Message) {
        telegram.sendMessage(message.chatId(), "请点击该[链接](${bgmClient.getOauthUrl(BgmAuthServer.genRandomCode(userId)).fm2md()})进行授权", parseMode = ParseMode.MarkdownV2)
    }
}
