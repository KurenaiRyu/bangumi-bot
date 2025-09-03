package moe.kurenai.bot.model.bilibili

import moe.kurenai.common.util.json
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.Test

class DynamicInfoTest {

    @Test
    fun test() {
        val info = json.decodeFromString<DynamicInfo>(DynamicInfoTest::class.java.getResource("/bilibili-dynamic-info-1.json").readText())
        println(info)
    }

}
