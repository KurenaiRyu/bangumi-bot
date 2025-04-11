package moe.kurenai.bot.repository

import kotlinx.coroutines.runBlocking
import org.junit.Test


class BiliBiliRepositoryTest {
    @Test
    fun testDynamic(): Unit = runBlocking {
        BiliBiliRepository.getDynamicDetail("985442669424541701")
        BiliBiliRepository.getDynamicDetail("989248654678163458")
        BiliBiliRepository.getDynamicDetail("995795705281904648")
    }
}
