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
import moe.kurenai.mihoyo.module.BaseResponse
import moe.kurenai.mihoyo.module.CreateQRCodeLogin
import moe.kurenai.mihoyo.module.QRCodeStatus
import moe.kurenai.mihoyo.module.QueryQRCodeStatus
import java.io.ByteArrayOutputStream

object MiHoYo {

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
            header("x-rpc-app_id", "bll8iq97cem8")
        }.body<BaseResponse<CreateQRCodeLogin>>().unwrap()
    }

    suspend fun queryQRCodeStatus(ticket: String): QueryQRCodeStatus {
        val response = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/queryQRLoginStatus") {
            header("x-rpc-app_id", "bll8iq97cem8")
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

    private fun <T> BaseResponse<T>.unwrap(): T {
        if (this.retcode != 0 || this.data == null) throw MiHoYoException(this.retcode, this.message)
        return this.data
    }

}
