package moe.kurenai.bot.command.commands

import moe.kurenai.bgm.exception.BgmException
import moe.kurenai.bgm.exception.UnauthorizedException
import moe.kurenai.bgm.request.user.GetMe
import moe.kurenai.bot.BangumiBot.RANDOM_CODE
import moe.kurenai.bot.BangumiBot.bgmClient
import moe.kurenai.bot.BangumiBot.redisson
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.BangumiBot.tokens
import moe.kurenai.bot.appendKey
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.tdlight.model.ParseMode
import moe.kurenai.tdlight.model.media.InputFile
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.SendMessage
import moe.kurenai.tdlight.request.message.SendPhoto
import moe.kurenai.tdlight.util.MarkdownUtil.fm2md
import org.apache.logging.log4j.LogManager
import java.time.Duration
import java.util.*

@Command(command = "start")
class Start : CommandHandler() {

    companion object {
        private val log = LogManager.getLogger()
    }

    override suspend fun execute(update: Update, message: Message, args: List<String>) {
        val userId = message.from!!.id
        kotlin.runCatching {
            tokens[userId]?.also { token ->
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
        val randomCode = UUID.randomUUID().toString().replace("-", "")
        log.info("AUTH_RANDOM_CODE: $randomCode")
        val bucket = redisson.getBucket<Long>(RANDOM_CODE.appendKey(randomCode))
        bucket.set(userId)
        bucket.expire(Duration.ofMinutes(10))
        SendMessage(message.chatId, "请点击该[链接](${bgmClient.getOauthUrl(randomCode).fm2md()})进行授权").apply {
            parseMode = ParseMode.MARKDOWN_V2
        }.send()
    }
}