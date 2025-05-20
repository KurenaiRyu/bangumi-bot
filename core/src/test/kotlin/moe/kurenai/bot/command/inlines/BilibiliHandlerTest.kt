package moe.kurenai.bot.command.inlines

import kotlinx.coroutines.runBlocking
import org.junit.Test

class BilibiliHandlerTest {

    @Test
    fun testHandle() = runBlocking{
        println(BilibiliHandler.doHandle("BV1sKJNzpEXs", 2, 0F))
    }
}
