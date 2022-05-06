package moe.kurenai.bot.controller

import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.BangumiBot.tdClient
import moe.kurenai.bot.appendKey
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneId

@RestController
class BotController {

    companion object {
        private val log = LogManager.getLogger()
    }

    @GetMapping
    fun home(): Mono<ResponseEntity<String>> {
        return Mono.just(ResponseEntity.ok().body("</p>Bangumi Bot. power by kurenai</p>"))
    }

    @GetMapping("callback")
    fun callback(
        code: String,
        state: String?,
        serverHttpRequest: ServerHttpRequest,
        serverHttpResponse: ServerHttpResponse,
    ): Mono<ResponseEntity<String>> {
        log.info(serverHttpRequest.uri)

        if (state == null) return Mono.just(ResponseEntity.ok().body("<p>缺少必要的请求参数</p>"))

        val lock = BangumiBot.redisson.getLock(BangumiBot.AUTH_LOCK.appendKey(state))
        val userId = BangumiBot.redisson.getBucket<String>(BangumiBot.RANDOM_CODE.appendKey(state))

        return userId.get()
            .log()
            .defaultIfEmpty("")
            .flatMap { id ->
                if (id.isNotBlank()) {
                    lock.tryLock()
                        .flatMap { locked ->
                            if (locked) {
                                BangumiBot.bgmClient.getToken(code)
                                    .flatMap { token ->
                                        val ttl = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond() + token.expiresIn
                                        BangumiBot.tokenTTLList.add(ttl.toDouble(), id)
                                            .zipWith(BangumiBot.tokens.put(id, token))
                                    }.flatMap {
                                        userId.delete()
                                    }.flatMap {
                                        serverHttpResponse.statusCode = HttpStatus.PERMANENT_REDIRECT
                                        serverHttpResponse.headers.location = URI.create("https://t.me/${tdClient.me.username}?start=success")
                                        serverHttpResponse.setComplete()
                                    }.then(Mono.empty())
                            } else {
                                Mono.just(ResponseEntity.ok().body("<p>处理中，请稍后</p>"))
                            }
                        }
                } else {
                    Mono.just(ResponseEntity.ok().body("<p>请重新发送 /start 命令查看信息</p>"))
                }
            }.onErrorResume { ex ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("<p>Error: " + "${ex.message}</p>"))
            }
    }

}