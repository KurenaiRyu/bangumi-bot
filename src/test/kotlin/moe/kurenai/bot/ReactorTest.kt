package moe.kurenai.bot

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import moe.kurenai.bgm.model.auth.AccessToken
import moe.kurenai.bot.BangumiBot.send
import moe.kurenai.bot.config.JsonJacksonKotlinCodec
import moe.kurenai.bot.config.RecordNamingStrategyPatchModule
import moe.kurenai.tdlight.request.message.EditMessageText
import moe.kurenai.tdlight.request.message.GetMessageInfo
import org.junit.Test
import org.redisson.Redisson
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReactorTest {

    private val redisMapper = jacksonObjectMapper()
        .registerModules(Jdk8Module(), JavaTimeModule(), RecordNamingStrategyPatchModule())
        .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
        .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build(), ObjectMapper.DefaultTyping.EVERYTHING);

    @Test
    fun testRedisson() {
        val redisson = Redisson.create(org.redisson.config.Config().also {
            val codec = JsonJacksonKotlinCodec(redisMapper)
            it.codec = codec
            it.useSingleServer().setAddress("redis://${Config.CONFIG.redis.host}:${Config.CONFIG.redis.port}").setDatabase(Config.CONFIG.redis.database)
        })

        println(redisson.getMap<String, AccessToken>("TOKEN")["161589"])
    }

    @Test
    fun testMapper() {
        val json = redisMapper.writeValueAsString(AccessToken("1", LocalDate.now().toEpochDay(), "", refreshToken = "", userId = 0))
        println(json)
        println(redisMapper.readValue(json, Object::class.java))
    }

    @Test
    fun testUri() {
        println(URI.create("https://bgm.tv/subject/351973").path.split("/"))
    }

    @Test
    fun testTime() {
        println(LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond())
    }

    @Test
    suspend fun testUpdate() {
        File("123").outputStream()
        val msg = GetMessageInfo("-1001250114081", 97822).send()
        EditMessageText(msg.text!! + "\nedited").apply {
            this.messageId = msg.messageId
            this.chatId = msg.chatId
            this.entities = msg.entities
            this.replyMarkup = msg.replyMarkup
        }.send()

        File("test").outputStream().buffered()
        BufferedInputStream(FileInputStream(File("test")))


    }

    @Test
    fun testLine() {
        println("[{\"id\":\"C107195\",\"type\":\"photo\",\"title\":\"ハギ\",\"photo_url\":\"https://lain.bgm.tv/pic/crt/l/c1/ce/107195_crt_6T62M.jpg\",\"thumb_url\":\"https://lain.bgm.tv/pic/crt/m/c1/ce/107195_crt_6T62M.jpg\",\"input_message_content\":{\"message_text\":\"[ハギ](https://lain\\\\.bgm\\\\.tv/pic/crt/l/c1/ce/107195\\\\_crt\\\\_6T62M\\\\.jpg)\\n\\n简体中文名: 荻\\n别名: Hagi\\\\(罗马字\\\\)\\n性别: 女\\n所属: 子班\",\"parse_mode\":\"MarkdownV2\",\"disable_web_page_preview\":false}}]")
    }
}
