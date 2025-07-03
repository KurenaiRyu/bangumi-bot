package moe.kurenai.mihoyo

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import moe.kurenai.common.util.json
import moe.kurenai.mihoyo.exception.MiHoYoException
import moe.kurenai.mihoyo.module.*
import moe.kurenai.mihoyo.util.EncryptUtil.APP_ID
import moe.kurenai.mihoyo.util.EncryptUtil.DEVICE_ID
import moe.kurenai.mihoyo.util.MiHoYoHeaders
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random

object MiHoYoClient {

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
        install(ContentNegotiation) {
            json(json)
        }
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
        val postContent = """
            {
                "device_id": "$seedId",
                "seed_id": "${UUID.randomUUID()}",
                "seed_time": "${LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)}",
                "platform": "2",
                "device_fp": "{{DeviceFp}}",
                "app_name": "bbs_cn",
                "ext_fields": "{\"proxyStatus\":0,\"isRoot\":0,\"romCapacity\":\"512\",\"deviceName\":\"Pixel5\",\"productName\":\"$productName\",\"romRemain\":\"512\",\"hostname\":\"db1ba5f7c000000\",\"screenSize\":\"1080x2400\",\"isTablet\":0,\"aaid\":\"\",\"model\":\"Pixel5\",\"brand\":\"google\",\"hardware\":\"windows_x86_64\",\"deviceType\":\"redfin\",\"devId\":\"REL\",\"serialNumber\":\"unknown\",\"sdCapacity\":125943,\"buildTime\":\"1704316741000\",\"buildUser\":\"cloudtest\",\"simState\":0,\"ramRemain\":\"124603\",\"appUpdateTimeDiff\":1716369357492,\"deviceInfo\":\"google\\\/{{productName}}\\\/redfin:13\\\/TQ3A.230901.001\\\/2311.40000.5.0:user\\\/release-keys\",\"vaid\":\"\",\"buildType\":\"user\",\"sdkVersion\":\"33\",\"ui_mode\":\"UI_MODE_TYPE_NORMAL\",\"isMockLocation\":0,\"cpuType\":\"arm64-v8a\",\"isAirMode\":0,\"ringMode\":2,\"chargeStatus\":3,\"manufacturer\":\"Google\",\"emulatorStatus\":0,\"appMemory\":\"512\",\"osVersion\":\"13\",\"vendor\":\"unknown\",\"accelerometer\":\"\",\"sdRemain\":123276,\"buildTags\":\"release-keys\",\"packageName\":\"com.mihoyo.hyperion\",\"networkType\":\"WiFi\",\"oaid\":\"\",\"debugStatus\":1,\"ramCapacity\":\"125943\",\"magnetometer\":\"\",\"display\":\"TQ3A.230901.001\",\"appInstallTimeDiff\":1706444666737,\"packageVersion\":\"2.20.2\",\"gyroscope\":\"\",\"batteryStatus\":85,\"hasKeyboard\":10,\"board\":\"windows\"}",
                "bbs_device_id": "$DEVICE_ID"
            }
            """
        val ret = client.post(url) {
            setBody(postContent)
        }.body<DeviceFp>()
        if (ret.code != 200) throw MiHoYoException(ret.code, ret.msg)
        return ret.deviceFp
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun generateSeedId(): String {
        return Random.nextBytes(8).toHexString()
    }

    private fun generateProductName(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return chars.asSequence().shuffled().take(6).toList().toCharArray().concatToString()
    }

    private inline fun <reified T> BaseResponse<T>.unwrap(successCode: Int = 0): T {
        if (this.retcode != successCode || this.data == null) throw MiHoYoException(this.retcode, this.message)
        return this.data
    }

}
