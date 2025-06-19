package moe.kurenai.bot

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
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.kurenai.bot.util.getLogger
import moe.kurenai.bot.util.md5
import org.jetbrains.kotlin.gradle.targets.js.toHex
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import kotlin.io.path.writeBytes
import kotlin.time.Duration.Companion.seconds


class MiHoYoApiTest {

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
            val main = "salt=$K2&t=$t&r=$r".toByteArray(StandardCharsets.UTF_8)
            val ds = main.md5().toHex()
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
            json(moe.kurenai.bot.util.json)
        }
    }

    @Test
    fun testCreateLogin() = runBlocking {
        val ret = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/createQRLogin"){
            header("x-rpc-app_id", "bll8iq97cem8")
        }.body<BaseResult<CreateQALogin>>()
        if (ret.retcode != 0) return@runBlocking
        genQRCodeImg(ret.data!!.url)
        delay(5.seconds)

        while (true) {
            val qrStatusRet = kotlin.runCatching {
                client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/queryQRLoginStatus") {
                    header("x-rpc-app_id", "bll8iq97cem8")
                    setBody(
                        """
                           { "ticket": "${ret.data.ticket}" }
                        """.trimIndent()
                    )
                }.body<BaseResult<QueryQRCodeStatus>>()
            }.getOrNull()
            if (qrStatusRet != null && qrStatusRet.retcode == 0) {
                val status = qrStatusRet.data!!.status
                println("QRCode status: $status")
                if (status == QueryQRCodeStatus.QRCodeStatus.CONFIRMED) {
                    break
                }
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

@Serializable
data class BaseResult<T>(
    val retcode: Int,
    val message: String,
    val data: T? = null
)

@Serializable
data class CreateQALogin(
    val ticket: String,
    val url: String
)

@Serializable
data class QueryQRCodeStatus(
    @SerialName("app_id")
    val appId: String,
    @SerialName("client_type")
    val clientType: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("need_realperson")
    val needRealPerson: Boolean,
    @SerialName("realname_info")
    val realNameInfo: RealNameInfo? = null,
    @SerialName("scanned_at")
    val scannedAt: String,
    val status: QRCodeStatus,
    val tokens: List<String>,
    @SerialName("user_info")
    val userInfo: UserInfo? = null
) {


    @Serializable
    data class RealNameInfo(
        @SerialName("action_ticket")
        val actionTicket: String,
        @SerialName("action_type")
        val actionType: String,
        val required: Boolean
    )

    @Serializable
    data class UserInfo(
        @SerialName("account_name")
        val accountName: String,
        val aid: String,
        @SerialName("area_code")
        val areaCode: String,
        val country: String,
        val email: String,
        @SerialName("identity_code")
        val identityCode: String,
        @SerialName("is_adult")
        val isAdult: Int,
        @SerialName("is_email_verify")
        val isEmailVerify: Int,
        val links: List<String>,
        val mid: String,
        val mobile: String,
        @SerialName("password_time")
        val passwordTime: String,
        val realname: String,
        @SerialName("rebind_area_code")
        val rebindAreaCode: String,
        @SerialName("rebind_mobile")
        val rebindMobile: String,
        @SerialName("rebind_mobile_time")
        val rebindMobileTime: String,
        @SerialName("safe_area_code")
        val safeAreaCode: String,
        @SerialName("safe_mobile")
        val safeMobile: String,
        @SerialName("unmasked_email")
        val unmaskedEmail: String,
        @SerialName("unmasked_email_type")
        val unmaskedEmailType: Int
    )

    @Serializable
    enum class QRCodeStatus {
        @SerialName("Created") CREATED,
        @SerialName("Scanned") SCANNED,
        @SerialName("Confirmed") CONFIRMED,
    }
}
