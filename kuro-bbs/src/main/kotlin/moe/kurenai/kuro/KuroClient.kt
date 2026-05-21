package moe.kurenai.kuro

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.ZoneId
import java.time.ZonedDateTime

class KuroClient {

    private val client = HttpClient(OkHttp) {

        defaultRequest {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Origin, "https://web-static.kurobbs.com")
                append(HttpHeaders.AcceptLanguage, "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                append(HttpHeaders.AcceptEncoding, "gzip")
                append(HttpHeaders.ContentType, "application/x-www-form-urlencoded; charset=utf-8")
                append(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 13; 2211133C Build/TKQ1.220905.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/114.0.5735.131 Mobile Safari/537.36 Kuro/1.0.9 KuroGameBox/1.0.9")
                append("x-requested-with", "com.kurogame.kjq")
                append("sec-fetch-site", "same-site")
                append("sec-fetch-mode", "cors")
                append("sec-fetch-dest", "empty")
                append("osversion", "Android")
                append("devcode", "2fba3859fe9bfe9099f2696b8648c2c6")
                append("countrycode", "CN")
                append("ip", "10.0.2.233")
                append("model", "2211133C")
                append("source", "android")
                append("lang", "zh-Hans")
                append("version", "1.0.9")
                append("versioncode", "1090")
                append("pragma", "no-cache")
                append("cache-control", "no-cache")
            }
        }

        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

    }

    context(ctx: KuroContext)
    suspend fun getMineInfo(otherUserId: Int): MineInfo {
        val res: KuroResponse<MineInfo> = client.post(URL.USER_MINE_URL) {
            headers {
                append("token", ctx.token)
            }
            setBody(FormDataContent(
                parameters {
                    append("otherUserId", otherUserId.toString())
                }
            ))
        }.body()
        return handleResponse(res)!!
    }

    context(ctx: KuroContext)
    suspend fun getGamerRoleList(userId: Int): GamerRoleList {
        val res: KuroResponse<GamerRoleList> = client.post(URL.GAMER_ROLE_LIST) {
            headers {
                append("token", ctx.token)
            }
            setBody(FormDataContent(
                parameters {
                    append("queryUserId", userId.toString())
                }
            ))
        }.body()
        return handleResponse(res)!!
    }

    context(ctx: KuroContext)
    suspend fun sign(roleInfo: GamerRoleList.RoleInfo) {
        val res: KuroResponse<DefaultKuroResponseData> = client.post(URL.SIGN_URL) {
            headers {
                append("token", ctx.token)
            }
            setBody(FormDataContent(
                parameters {
                    append("gameId", roleInfo.gameId.toString())
                    append("serverId", roleInfo.serverId)
                    append("roleId", roleInfo.roleId.toString())
                    append("userId", roleInfo.userId.toString())
                    append("reqMonth", String.format("%02d", ZonedDateTime.now(zoneId).monthValue))
                }
            ))
        }.body()

        handleException(res)
    }

    private fun <T: KuroResponseData> handleResponse(response: KuroResponse<T>): T? {
        handleException(response)
        return response.data
    }

    private fun <T: KuroResponseData> handleException(response: KuroResponse<T>) {
        if (!(response.success?:false)) throw KuroException(response.code, response.msg)
    }

    companion object {

        private val zoneId = ZoneId.of("Asia/Shanghai")

        object URL {
            const val GAMER_ROLE_LIST = "https://api.kurobbs.com/gamer/role/default"
            const val SIGN_URL = "https://api.kurobbs.com/encourage/signIn/v2"
            const val USER_SIGN_URL = "https://api.kurobbs.com/user/signIn"
            const val USER_MINE_URL = "https://api.kurobbs.com/user/mineV2"
        }
    }
}
