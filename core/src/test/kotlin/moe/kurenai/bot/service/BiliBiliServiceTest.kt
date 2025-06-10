package moe.kurenai.bot.service

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.URI


class BiliBiliServiceTest {
    @Test
    fun testDynamic(): Unit = runBlocking {
        BiliBiliService.getDynamicDetail("985442669424541701")
        BiliBiliService.getDynamicDetail("989248654678163458")
        BiliBiliService.getDynamicDetail("995795705281904648")
    }

    @Test
    fun testOfficialAccountDynamic(): Unit = runBlocking {
        BiliBiliService.getDynamicDetail("1076398325726445590")
    }

    @Test
    fun testRedirection() = runBlocking {
        println(BiliBiliService.getRedirectUrl(URI("https://b23.tv/1XUGxgS")))
    }
}
