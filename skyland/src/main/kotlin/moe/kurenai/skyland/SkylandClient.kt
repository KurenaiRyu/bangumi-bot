package moe.kurenai.skyland

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.*
import jdk.internal.org.jline.utils.AttributedStringBuilder.append
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import moe.kurenai.common.util.buildKtorLogger
import moe.kurenai.common.util.getLogger
import moe.kurenai.skyland.model.*
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import java.time.LocalDateTime
import java.time.ZoneOffset

class SkylandClient {

    private val log = getLogger()

    private val client = HttpClient(OkHttp) {
        defaultRequest {
            headers {
                append(HttpHeaders.Accept, "application/json;text/plain")
                append(HttpHeaders.ContentType, "application/json")
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
    private fun HttpRequestBuilder.resolveSignHeader(json: String? = null) {
        headers {
            append("cred", ctx.credInfo.cred)

            val (sign, t) = when (method) {
                HttpMethod.Get -> generateSignature(url.encodedPath, url.toString().substringAfter('?'))
                else -> generateSignature(url.encodedPath, json!!)
            }
            append("sign", sign.utf8())
            append("platform", "")
            append("timestamp", t.toString())
            append("dId", "")
            append("vName", "")
        }
    }

    private fun HeadersBuilder.appendNormalHeader() {
        append(HttpHeaders.AcceptEncoding, "gzip")
        append(HttpHeaders.UserAgent, "Skland/1.0.1 (com.hypergryph.skland; build:100001014; Android 31; ) Okhttp/4.11.0")
        append(HttpHeaders.Connection, "close")
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
    private fun generateSignature(path: String, params: String): Pair<ByteString, Long> {
        // 总是说请勿修改设备时间，怕不是yj你的服务器有问题吧，所以这里特地-2
        val t = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8)) - 2
        val signJsonStr =  buildJsonObject {
            put("platform", "")
            put("timestamp", t.toString())
            put("dId", "")
            put("vName", "")
        }.toString()
        val s = (path + params + t + signJsonStr).encodeUtf8()
        return s.hmacSha256(ctx.credInfo.token.encodeUtf8()).md5() to t
    }

    context(ctx: SkylandContext)
    suspend fun grantCode(): String {
        val did = getDId()
        val httpRes = client.post(URL.OAUTH2_GRANT) {
            headers {
                appendNormalHeader()
                append("dId", did)
            }

            setBody(GrantRequest(token = ctx.token))
        }
        if (httpRes.status.isSuccess()) {
            error("Grant code failed: ${httpRes.bodyAsText()}")
        }
        val res = httpRes.body<SkylandResponse<Grant>>()
        if (res.code != 0) {
            error("Grant code failed: ${res.message}")
        }
        return res.data!!.code
    }

    context(ctx: SkylandContext)
    suspend fun authByCode(grantCode: String): CredInfo {
        val dId = getDId()
        val res = client.post(URL.AUTH_GENERATE_CRED_BY_CODE) {
            headers {
                appendNormalHeader()
                append("dId", dId)
            }

            setBody(AuthByCodeRequest(code = grantCode))
        }.body<SkylandResponse<CredInfo>>()

        if (res.code != 0) {
            error("Auth code failed: ${res.message}")
        }

        return res.data!!
    }

    private suspend fun getDId(): String {
        val req = SecuritySM.buildDidRequest()

        val response = client.post(URL.DEVICES_INFO_URL) {
            setBody(req)
        }.body<SkylandResponse<DidInfo>>()

        if (response.code != 1100) {
            error("Unexpected response code ${response.code}")
        }

        // 开头必须是B
        return "B" + response.data!!.deviceId
    }

    context(ctx: SkylandContext)
    suspend fun getBindingList(): List<BindingList.Binding> {
        val res = client.get(URL.GAME_PLAYER_BINDING) {
            resolveSignHeader()
        }.body<SkylandResponse<BindingList>>()
        if (res.code != 0) error("Binding list failed: ${res.message}")
        return res.data!!.list
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
