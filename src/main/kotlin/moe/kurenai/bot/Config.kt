package moe.kurenai.bot

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Yaml
import java.io.File

@Serializable
data class Config(
    var redis: Redis = Redis(),
    var telegram: Telegram = Telegram(),
    var bgm: Bgm = Bgm(),
    var bilibili: Bilibili = Bilibili(),
    var debug: Boolean = false
) {

    companion object {
        private val yaml = Yaml {
            encodeDefaultValues = false
        }
        private val file = File("config/config.yml")
        var CONFIG: Config = Config()

        init {
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
                yaml.encodeToString(serializer(), CONFIG)
                throw Exception("请填写配置文件。")
            } else {
                CONFIG = yaml.decodeFromString(serializer(), file.readText())
            }
        }
    }

}

@Serializable
data class Bilibili(
    var cookie: String? = null,
    var shortLinkHost: List<String> = emptyList()
)

@Serializable
data class Redis(
    var host: String = "localhost",
    var port: Int = 6379,
    var database: Int = 0,
)

@Serializable
data class Telegram(
    var baseUrl: String = "https://api.telegram.org",
    var apiId: Int? = null,
    var apiHash: String? = null,
    var token: String = "",
    var userMode: Boolean = false,
    var updateBaseUrl: String = baseUrl,
    var linkPreviewGroup: Long? = null
)

@Serializable
data class Bgm(
    var appId: String = "",
    var appSecret: String = "",
    var redirectUrl: String = "",
    var server: Server = Server()
)

@Serializable
data class Server(
    val port: Int = 8080,
    val keyStorePw: String = ""
)
