package moe.kurenai.bot.command.commands

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
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@Command(command = "start")
class Start : CommandHandler() {

    companion object {
        private val log = LogManager.getLogger()
    }

    override fun execute(update: Update, message: Message, args: List<String>): Mono<*> {
        val userId = message.from!!.id
        return tokens[userId.toString()].flatMap { token ->
            GetMe().apply { this.token = token.accessToken }.send()
        }.flatMap { me ->
            SendPhoto(message.chatId, InputFile(me.avatar.large)).apply {
                caption = "已绑定`${me.nickname.fm2md()}`\\(`${me.username.takeIf { it.isNotBlank() } ?: me.id}`\\)"
                parseMode = ParseMode.MARKDOWN_V2
            }.send()
        }.switchIfEmpty(Mono.defer {
            val randomCode = UUID.randomUUID().toString().replace("-", "")
            log.info("AUTH_RANDOM_CODE: $randomCode")
            val bucket = redisson.getBucket<String>(RANDOM_CODE.appendKey(randomCode))
            bucket
                .set(userId.toString())
                .then(bucket.expire(Duration.ofMinutes(10)))
                .flatMap {
                    SendMessage(message.chatId, "请点击该[链接](${bgmClient.getOauthUrl(randomCode).fm2md()})进行授权").apply {
                        parseMode = ParseMode.MARKDOWN_V2
                    }.send()
                }.switchIfEmpty(Mono.defer {
                    log.info("redis null")
                    Mono.empty()
                })
        }).doOnError {
            log.error("Get me $userId error: ${it.message}")
        }
    }
}