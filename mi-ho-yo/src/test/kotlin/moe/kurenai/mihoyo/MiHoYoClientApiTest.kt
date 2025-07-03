package moe.kurenai.mihoyo

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import moe.kurenai.common.util.getLogger
import moe.kurenai.common.util.md5
import moe.kurenai.mihoyo.module.*
import moe.kurenai.mihoyo.util.MiHoYoHeaders
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class MiHoYoClientApiTest {

    val log = getLogger()
    companion object {
        const val lettersAndNumbers = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }

    val uuid = UUID.nameUUIDFromBytes("Kurenai".toByteArray())
    val androidVersion = 13
    val deviceModel = "redmi k50 ultra"
    val miHoYoBBSVersion = "2.71.1"
    val K2 = "rtvTthKxEyreVXQCnhluFgLXPOFKPHlA"
    val UA = "Mozilla/5.0 (Linux; Android $androidVersion; $deviceModel Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/108.0.5359.128 Mobile Safari/537.36 miHoYoBBS/$miHoYoBBSVersion"
    @OptIn(ExperimentalStdlibApi::class)
    private val miHoYoPlugin = createClientPlugin("MihoyoPlugin") {
        onRequest {req, content ->
            req.header("x-rpc-client_type", "2") // 安卓端APP
            req.header("x-rpc-sys_version", androidVersion) // 安卓系统（或iOS）大版本号
            req.header("x-rpc-channel", "xiaomi")
            req.header("x-rpc-device_name", "xiaomi $deviceModel")
            req.header("x-rpc-device_model", deviceModel)
            req.header("x-rpc-device_id", uuid)
            req.header("X-Requested-With", "com.mihoyo.hyperion")

            val host = req.url.host
            req.header(HttpHeaders.Origin, "https://$host")
            req.header(HttpHeaders.Host, host)
            req.header(HttpHeaders.Referrer, "https://app.mihoyo.com")

            val t = System.currentTimeMillis()
            val r = lettersAndNumbers.asSequence().shuffled().take(6).toString()
            val main = "salt=$K2&t=$t&r=$r"
            val ds = main.toByteArray(StandardCharsets.UTF_8).md5().toHexString()
            req.header("DS", ds)

        }
    }

    private val httpLogger = object : Logger {
        override fun log(message: String) {
            log.info(message)
        }
    }
    private val client = HttpClient {
        install(miHoYoPlugin) {}
        install(Logging) {
            logger = httpLogger
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Cookie }
        }
        install(ContentNegotiation) {
            json(Json{
                ignoreUnknownKeys = true
            })
        }
    }

    @Test
    fun testCreateLogin() = runBlocking {
        val ret = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/createQRLogin"){
            header("x-rpc-app_id", MiHoYoClient.APP_ID)
        }.body<BaseResponse<CreateQRCodeLogin>>()
        if (ret.retcode != 0) return@runBlocking
        val createQALogin = ret.data?:throw IllegalStateException("No data.")
        genQRCodeImg(createQALogin.url)
        delay(5.seconds)

        while (true) {
            val qrStatusResponse = kotlin.runCatching {
                client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/queryQRLoginStatus") {
                    header(MiHoYoHeaders.X_RPC_APP_ID, MiHoYoClient.APP_ID)
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(mapOf("ticket" to "createQALogin.ticket")))
                }
            }.getOrNull()
            if (qrStatusResponse != null) {
                val qrStatusRet = qrStatusResponse.body<BaseResponse<QueryQRCodeStatus>>()
                if (qrStatusRet.retcode == 0) {
                    val status = qrStatusRet.data!!.status
                    if (status == QRCodeStatus.CONFIRMED) {
                        break
                    }
                }
            }
            delay(5.seconds)
        }
    }

    @Test
    fun testGameToken(): Unit = runBlocking {
        val res = client.post("https://hk4e-sdk.mihoyo.com/hk4e_cn/combo/panda/qrcode/fetch") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(mapOf("app_id" to "12", "device" to uuid.toString())))
        }
        val ret= res.body<BaseResponse<JsonElement>>()
        val url = ret.data!!.jsonObject["url"]!!.jsonPrimitive.content
        genQRCodeImg(url)
        val ticket = Url(url).parameters.get("ticket")

        delay(5.seconds)
        while (true) {
            val status = client.post("https://hk4e-sdk.mihoyo.com/hk4e_cn/combo/panda/qrcode/query") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(mapOf(
                    "ticket" to ticket!!,
                    "device" to uuid.toString(),
                    "app_id" to "12",
                )))
            }.body<BaseResponse<FetchQRCodeStatus>>()

            if (status.retcode == 0 && QRCodeStatus.CONFIRMED == status.data?.stat) {
                break
            }
            delay(5.seconds)
        }


    }

    private fun genQRCodeImg(url: String) {
        val qrCode = MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 300, 300)
        MatrixToImageWriter.writeToPath(qrCode, "jpg", Path.of( "./qrcode.jpg", ))
    }

    @Test
    fun testGenQRCode() {
        val url = "https://user.mihoyo.com/login-platform/mobile.html?expire=1750300429\u0026tk=ff1110f7-b22a-4742-bb8c-c4403826fe46\u0026token_types=4#/login/qr"
        val qrCode = MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 300, 300)
//        printQRCodeToConsole(qrCode)

        MatrixToImageWriter.writeToPath(qrCode, "jpg", Path.of( "./qrcode.jpg", ))
    }

    @Test
    fun testSToken(): Unit = runBlocking {
        client.post("https://api-takumi.mihoyo.com/account/ma-cn-session/app/getTokenByGameToken") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("account_id", "mi-ho-yo-bbs-id")
                put("game_token", "zz")
            })
        }.body<BaseResponse<SToken>>()
    }

    @Test
    fun testJson() {
        println(Json.encodeToJsonElement(mapOf("ticket" to "createQALogin.ticket")))

    }

    fun printQRCodeToConsole(matrix: BitMatrix) {
        val black = "██"  // 使用双倍宽度的字符，让二维码更方
        val white = "  "
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                print(if (matrix.get(x, y)) black else white)
            }
            println()
        }
    }
}
