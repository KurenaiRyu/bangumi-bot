package moe.kurenai.bot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.kotlinModule
import java.io.File

class Config(
    var redis: Redis = Redis(),
    var telegram: Telegram = Telegram(),
    var bgm: Bgm = Bgm(),
    var debug: Boolean = false
) {

    companion object {
        private val MAPPER = ObjectMapper(YAMLFactory()).registerModules(kotlinModule())
        private val file = File("config/config.yml")
        var CONFIG: Config = Config()

        init {
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
                MAPPER.writeValue(file, CONFIG)
                throw Exception("请填写配置文件。")
            } else {
                CONFIG = MAPPER.readValue(file, Config::class.java)
            }
        }
    }

}

data class Redis(
    var host: String = "localhost",
    var port: Int = 6379,
    var database: Int = 0,
)

data class Telegram(
    var baseUrl: String = "https://api.telegram.org",
    var token: String = "",
    var userMode: Boolean = false,
    var updateBaseUrl: String = baseUrl
)

data class Bgm(
    var appId: String = "",
    var appSecret: String = "",
    var redirectUrl: String = "",
)