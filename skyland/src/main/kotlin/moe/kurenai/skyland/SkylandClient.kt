package moe.kurenai.skyland.moe.kurenai.skyland

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import moe.kurenai.skyland.moe.kurenai.skyland.model.GrantRequest

class SkylandClient {

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

    }

    context(ctx: SkylandContext)
    suspend fun grant() {
        client.post(URL.OAUTH2_GRANT) {
            headers {
                append(HttpHeaders.Accept, "application/json")
            }

            setBody(GrantRequest(token = ctx.token))
        }
    }

    private fun getDId() {
        // storageName = '.thumbcache_' + md5(SM_CONFIG['organization']) // 用于从本地存储获得值
        // uid = uuid()
        // priId=md5(uid)[0:16]
        // ep=rsa(uid,publicKey)
        // SMID = localStorage.get(storageName);// 获得本地存储存的值
        // _0x30b2eb为递归md5


    }

    companion object {

        const val APP_CODE = "4ca99fa6b56cc2ba"

        object URL {
            const val OAUTH2_GRANT = "https://as.hypergryph.com/user/oauth2/v2/grant"
            const val AUTH_GENERATE_CRED_BY_CODE = "https://zonai.skland.com/api/v1/user/auth/generate_cred_by_code"
            const val GAME_ATTENDANCE = "https://zonai.skland.com/api/v1/game/attendance"
            const val GAME_PLAYER_BINDING = "https://zonai.skland.com/api/v1/game/player/binding"
        }
    }

}
