package moe.kurenai.bot.model.bilibili

import kotlinx.coroutines.runBlocking
import moe.kurenai.bot.service.BiliBiliService
import moe.kurenai.common.util.json
import kotlin.test.Test

class DynamicInfoTest {

    @Test
    fun test() {
        val info = json.decodeFromString<DynamicInfo>(DynamicInfoTest::class.java.getResource("/bilibili-dynamic-info-1.json").readText())
        println(info)
    }

    @org.junit.Test
    fun testUrl(): Unit = runBlocking {
        println(BiliBiliService.getDynamicDetail("1156842985854337026"))
    }
}
