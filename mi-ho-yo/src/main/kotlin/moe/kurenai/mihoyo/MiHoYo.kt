package moe.kurenai.mihoyo

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import moe.kurenai.common.util.getLogger
import moe.kurenai.common.util.json
import moe.kurenai.mihoyo.exception.MiHoYoException
import moe.kurenai.mihoyo.module.*
import moe.kurenai.mihoyo.module.zzz.AvatarList
import moe.kurenai.mihoyo.util.MiHoYoHeaders
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.*
import kotlin.random.Random

object MiHoYo {

    internal const val APP_ID = "bll8iq97cem8"
    internal const val APP_VERSION = "2.85.1"
    internal const val API_SALT = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
    internal const val API_SALT2 = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"
    internal const val API_SALT_X4 = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"
    internal const val API_SALT_X6 = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
    internal const val API_SALT_PROD = "JwYDpKvLj6MrMqqYU6jTKF17KNO2PXoS"
    internal val DEVICE_ID = UUID.nameUUIDFromBytes("Pixel 5".toByteArray()).toString()
    internal const val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/108.0.5359.128 Mobile Safari/537.36 miHoYoBBS/$APP_VERSION"
    internal val cookieMap = HashMap<String, String>()

    internal var deviceFp: String = ""
        private set

    private val log = getLogger()
    private val httpLogger = object : Logger {
        override fun log(message: String) {
            log.info(message)
        }
    }
    private val miHoYoPlugin = createClientPlugin("MiHoYo Plugin") {
        onRequest { request, _ ->
            val headers = request.headers
            headers.setIfAbsent(HttpHeaders.UserAgent, UA)
        }
    }

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = httpLogger
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Cookie }
        }
        install(miHoYoPlugin)
    }

    suspend fun createQRCodeLogin(): CreateQRCodeLogin {
        return client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/createQRLogin"){
            header(MiHoYoHeaders.X_RPC_APP_ID, APP_ID)
        }.body<BaseResponse<CreateQRCodeLogin>>().unwrap()
    }

    suspend fun queryQRCodeStatus(ticket: String): QueryQRCodeStatus {
        val response = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/queryQRLoginStatus") {
            header(MiHoYoHeaders.X_RPC_APP_ID, APP_ID)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(mapOf("ticket" to ticket)))
        }
        val ret = response.body<BaseResponse<QueryQRCodeStatus>>().unwrap()
        if (ret.status == QRCodeStatus.CONFIRMED) {
            val cookie = response.setCookie().joinToString("; ") {
                "${it.name}=${it.value}"
            }
            ret.cookie = cookie
        }
        return ret
    }

    fun createQRCodeImage(url: String, width: Int = 200, height: Int = 200): ByteArray {
        val qrCode = MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, width, height)
        return ByteArrayOutputStream().use {
            MatrixToImageWriter.writeToStream(qrCode, "jpg", it)
            it.toByteArray()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun CreateQRCodeLogin.createQRCodeImage(width: Int = 200, height: Int = 200) = createQRCodeImage(this.url, width, height)

    suspend fun CreateQRCodeLogin.waitForLogin(): QueryQRCodeStatus {
        while (true) {
            val ret = queryQRCodeStatus(this.ticket)
            if (ret.status == QRCodeStatus.CONFIRMED) return ret
        }
    }

    suspend fun getDeviceFp(): String {
        val url = "https://public-data-api.mihoyo.com/device-fp/api/getFp"
        val seedId = generateSeedId()
        val productName = generateProductName()
        val deviceFp = deviceFp.ifBlank { generateDeviceFp() }
        val postContent = """
            {
                "device_id": "$seedId",
                "seed_id": "${UUID.randomUUID()}",
                "seed_time": "${Instant.now().toEpochMilli()}",
                "platform": "2",
                "device_fp": "$deviceFp",
                "app_name": "bbs_cn",
                "ext_fields": "{\"proxyStatus\":0,\"isRoot\":0,\"romCapacity\":\"512\",\"deviceName\":\"Pixel5\",\"productName\":\"$productName\",\"romRemain\":\"512\",\"hostname\":\"db1ba5f7c000000\",\"screenSize\":\"1080x2400\",\"isTablet\":0,\"aaid\":\"\",\"model\":\"Pixel5\",\"brand\":\"google\",\"hardware\":\"windows_x86_64\",\"deviceType\":\"redfin\",\"devId\":\"REL\",\"serialNumber\":\"unknown\",\"sdCapacity\":125943,\"buildTime\":\"1704316741000\",\"buildUser\":\"cloudtest\",\"simState\":0,\"ramRemain\":\"124603\",\"appUpdateTimeDiff\":1716369357492,\"deviceInfo\":\"google\\\/$productName\\\/redfin:13\\\/TQ3A.230901.001\\\/2311.40000.5.0:user\\\/release-keys\",\"vaid\":\"\",\"buildType\":\"user\",\"sdkVersion\":\"33\",\"ui_mode\":\"UI_MODE_TYPE_NORMAL\",\"isMockLocation\":0,\"cpuType\":\"arm64-v8a\",\"isAirMode\":0,\"ringMode\":2,\"chargeStatus\":3,\"manufacturer\":\"Google\",\"emulatorStatus\":0,\"appMemory\":\"512\",\"osVersion\":\"13\",\"vendor\":\"unknown\",\"accelerometer\":\"\",\"sdRemain\":123276,\"buildTags\":\"release-keys\",\"packageName\":\"com.mihoyo.hyperion\",\"networkType\":\"WiFi\",\"oaid\":\"\",\"debugStatus\":1,\"ramCapacity\":\"125943\",\"magnetometer\":\"\",\"display\":\"TQ3A.230901.001\",\"appInstallTimeDiff\":1706444666737,\"packageVersion\":\"2.20.2\",\"gyroscope\":\"\",\"batteryStatus\":85,\"hasKeyboard\":10,\"board\":\"windows\"}",
                "bbs_device_id": "$DEVICE_ID"
            }
            """.trimIndent()
        val ret = client.post(url) {
            setBody(postContent)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, UA)
        }.body<BaseResponse<DeviceFp>>().unwrap()
        if (ret.code != 200) throw MiHoYoException(ret.code, ret.msg)
        this.deviceFp = ret.deviceFp
        return ret.deviceFp
    }

    context(AccountContext)
    suspend fun getAvatarList(): List<AvatarList.Avatar> {
        val url = "https://api-takumi-record.mihoyo.com/event/game_record_zzz/api/zzz/avatar/basic"
        val ret = client.get(url) {
            parameter("role_id", zzzInfo.gameUid)
            parameter("server", zzzInfo.region)
            header(HttpHeaders.Cookie, cookie)
        }.body<BaseResponse<AvatarList>>().unwrap()
        return ret.avatarList
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun generateSeedId(): String {
        return Random.nextBytes(8).toHexString()
    }

    private fun generateProductName(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return CharArray(6) {
            chars[Random.nextInt(chars.length)]
        }.concatToString()
    }

    private fun generateDeviceFp(): String {
        val chars = "abcdef123456789"
        return CharArray(13) {
            chars[Random.nextInt(chars.length)]
        }.concatToString()
    }

    private fun HeadersBuilder.setIfAbsent(name: String, value: String) {
        if (!this.contains(name)) this[name] = value
    }

    private inline fun <reified T> BaseResponse<T>.unwrap(successCode: Int = 0): T {
        if (this.retcode != successCode || this.data == null) throw MiHoYoException(this.retcode, this.message)
        return this.data
    }

}
