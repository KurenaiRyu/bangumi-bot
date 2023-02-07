package moe.kurenai.bot.command.commands

import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.command.Command
import moe.kurenai.bot.command.CommandHandler
import moe.kurenai.bot.repository.CharacterRepository
import moe.kurenai.bot.repository.PersonRepository
import moe.kurenai.bot.repository.SakugabooruRepository
import moe.kurenai.bot.repository.SubjectRepository
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.Update


@Command("status")
class Status : CommandHandler {

    override suspend fun execute(update: Update, message: Message, args: List<String>) {
        val runtime = Runtime.getRuntime()
        val arr = arrayOf(runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory(), runtime.maxMemory(), runtime.freeMemory(), runtime.totalMemory())
            .map { it / 1024 / 1024 }
            .map { "${it}m" }
        val msg = """
            总可用内存: ${arr[0]}/${arr[1]}
            剩余可用分配内存: ${arr[2]}/${arr[3]}
            --------------------------------------
            SubjectCache: ${SubjectRepository.cacheStats.snapshot().hitCount()} / ${SubjectRepository.cacheStats.snapshot().loadCount()} (${
            SubjectRepository.cacheStats.snapshot().hitRate()
        })
            PersonCache: ${PersonRepository.cacheStats.snapshot().hitCount()} / ${PersonRepository.cacheStats.snapshot().loadCount()} (${
            PersonRepository.cacheStats.snapshot().hitRate()
        })
            CharacterCache: ${CharacterRepository.cacheStats.snapshot().hitCount()} / ${
            CharacterRepository.cacheStats.snapshot().loadCount()
        } (${CharacterRepository.cacheStats.snapshot().hitRate()})
            SakugabooruCache: ${SakugabooruRepository.cacheStats.snapshot().hitCount()} / ${
            SakugabooruRepository.cacheStats.snapshot().loadCount()
        } (${SakugabooruRepository.cacheStats.snapshot().hitRate()})
        """.trimIndent()

        send(message.chatId, msg)
    }
}
