package moe.kurenai.skyland.moe.kurenai.skyland

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import moe.kurenai.common.util.buildKtorLogger
import moe.kurenai.common.util.getLogger
import moe.kurenai.skyland.moe.kurenai.skyland.model.AuthByCodeRequest
import moe.kurenai.skyland.moe.kurenai.skyland.model.CredInfo
import moe.kurenai.skyland.moe.kurenai.skyland.model.DidResponse
import moe.kurenai.skyland.moe.kurenai.skyland.model.Grant
import moe.kurenai.skyland.moe.kurenai.skyland.model.GrantRequest
import moe.kurenai.skyland.moe.kurenai.skyland.model.SkylandResponse
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class SkylandClient {

    private val log = getLogger()

    private val client = HttpClient(OkHttp) {
        defaultRequest {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.AcceptEncoding, "gzip")
                append(HttpHeaders.UserAgent, "Skland/1.0.1 (com.hypergryph.skland; build:100001014; Android 31; ) Okhttp/4.11.0")
                append(HttpHeaders.Connection, "close")
            }
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            logger = buildKtorLogger { log.debug(it) }
            level = LogLevel.ALL
        }
    }

    context(ctx: SkylandContext)
    private fun HttpRequestBuilder.resolveSignHeader(url: String, body: String) {
        val url = Url(url)
        headers {
            append("cred", ctx.credInfo.cred)

            when (method) {
                HttpMethod.Get -> append("sign", "client_credentials")
            }

        }
    }

    /**
     * 获得签名头
     *
     * 接口地址+方法为Get请求？用query否则用body+时间戳+ 请求头的四个重要参数（dId，platform，timestamp，vName）.toJSON()
     * 将此字符串做HMAC加密，算法为SHA-256，密钥token为请求cred接口会返回的一个token值
     * 再将加密后的字符串做MD5即得到sign
     * @param token: 拿cred时候的token
     * @param path: 请求路径（不包括网址）
     * @param params: 如果是GET，则是它的query。POST则为它的body
     * @return: 计算完毕的sign
     */
    context(ctx: SkylandContext)
    private fun generateSignature(path: String, params: String) {
        // 总是说请勿修改设备时间，怕不是yj你的服务器有问题吧，所以这里特地-2
        val t = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8))
    }

    context(ctx: SkylandContext)
    suspend fun grantCode(): String {
        val did = getDId()
        val httpRes = client.post(URL.OAUTH2_GRANT) {
            headers {
                append("dId", did)
            }

            setBody(GrantRequest(token = ctx.token))
        }
        if (httpRes.status.isSuccess()) {
            error("Grant code failed: ${httpRes.bodyAsText()}")
        }
        val res = httpRes.body<SkylandResponse<Grant>>()
        if (res.status != 0) {
            error("Grant code failed: ${res.msg}")
        }
        return res.data!!.code
    }

    context(ctx: SkylandContext)
    suspend fun authByCode(grantCode: String): CredInfo {
        val dId = getDId()
        val res = client.post(URL.AUTH_GENERATE_CRED_BY_CODE) {
            headers {
                append("dId", dId)
            }

            setBody(AuthByCodeRequest(code = grantCode))
        }.body<SkylandResponse<CredInfo>>()

        if (res.status != 0) {
            error("Auth code failed: ${res.msg}")
        }

        return res.data!!
    }

    private suspend fun getDId(): String {
        val req = SecuritySM.buildDidRequest()

        val response = client.post(URL.DEVICES_INFO_URL) {
            setBody(req)
        }.body<DidResponse>()

        if (response.code != 1100) {
            error("Unexpected response code ${response.code}")
        }

        return "B" + response.detail.deviceId
    }

    suspend fun getBindingList() {
        client.get(URL.GAME_PLAYER_BINDING) {
            headers {

            }
        }
        TODO("Not yet implemented")
    }

    companion object {

        const val APP_CODE = "4ca99fa6b56cc2ba"

        object URL {
            const val OAUTH2_GRANT = "https://as.hypergryph.com/user/oauth2/v2/grant"
            // 查询dId请求头
            const val DEVICES_INFO_URL = "https://fp-it.portal101.cn/deviceprofile/v4"
            const val AUTH_GENERATE_CRED_BY_CODE = "https://zonai.skland.com/api/v1/user/auth/generate_cred_by_code"
            const val GAME_ATTENDANCE = "https://zonai.skland.com/api/v1/game/attendance"
            const val GAME_PLAYER_BINDING = "https://zonai.skland.com/api/v1/game/player/binding"
        }
    }

}
