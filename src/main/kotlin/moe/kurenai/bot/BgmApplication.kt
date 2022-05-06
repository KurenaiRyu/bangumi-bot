package moe.kurenai.bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@SpringBootApplication
@EnableWebFlux
class BgmApplication

fun main(args: Array<String>) {
    BangumiBot.start()
    runApplication<BgmApplication>(*args)
}