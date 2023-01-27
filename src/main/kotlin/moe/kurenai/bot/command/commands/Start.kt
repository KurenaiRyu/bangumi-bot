package moe.kurenai.bot.command.commands

import moe.kurenai.bgm.exception.BgmException
import moe.kurenai.bgm.exception.UnauthorizedException
import moe.kurenai.bgm.request.user.GetMe
import moe.kurenai.bot.BangumiBot.bgmClient
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.BgmAuthServer
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.token
import moe.kurenai.bot.util.getLogger
import moe.kurenai.tdlight.model.ParseMode
import moe.kurenai.tdlight.model.media.InputFile
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.SendMessage
import moe.kurenai.tdlight.request.message.SendPhoto
import moe.kurenai.tdlight.util.MarkdownUtil.fm2md

@Command(command = "start")
class Start : CommandHandler {

    companion object {
        private val log = getLogger()
    }

    override suspend fun execute(update: Update, message: Message, args: List<String>) {
        val userId = message.from!!.id
        kotlin.runCatching {
            message.token()?.also { token ->
                val me = GetMe().apply { this.token = token.accessToken }.send()
                SendPhoto(message.chatId, InputFile(me.avatar.large)).apply {
                    caption = "已绑定`${me.nickname.fm2md()}`\\(`${me.username.takeIf { it.isNotBlank() } ?: me.id}`\\)"
                    parseMode = ParseMode.MARKDOWN_V2
                }.send()
            } ?: kotlin.run {
                doBind(userId, message)
            }
        }.recoverCatching {
            if (it is BgmException) {
                if (it is UnauthorizedException) {
                    doBind(userId, message)
                } else {
                    SendMessage(message.chatId, "请求bgm异常: [${it.code}] ${it.error} ${it.message ?: ""}").send()
                }
            } else {
                SendMessage(message.chatId, "Bot内部异常").send()
            }
        }.onFailure {
            log.error("Get me $userId error: ${it.message}")
        }
    }

    private suspend fun doBind(userId: Long, message: Message) {
        SendMessage(message.chatId, "请点击该[链接](${bgmClient.getOauthUrl(BgmAuthServer.genRandomCode(userId)).fm2md()})进行授权").apply {
            parseMode = ParseMode.MARKDOWN_V2
        }.send()
    }
}
