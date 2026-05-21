import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
                append("Accept", "application/json")
                append("osversion", "Android")
                append("devcode", "2fba3859fe9bfe9099f2696b8648c2c6")
                append("countrycode", "CN")
                append("ip", "10.0.2.233")
                append("model", "2211133C")
                append("source", "android")
                append("lang", "zh-Hans")
                append("version", "1.0.9")
                append("versioncode", "1090")
                append("content-type", "application/x-www-form-urlencoded; charset=utf-8")
                append("accept-encoding", "gzip")
                append("user-agent", "okhttp/3.10.0")
            }
        }

        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

    }

    context(ctx: KuroContext)
    suspend fun getMineInfo(type: Int = 1): MineInfo {
        val res: KuroResponse<MineInfo> = client.post(URL.USER_MINE_URL) {
            headers {
                append("token", ctx.token)
            }
            setBody(FormDataContent(
                parameters {
                    append("type", type.toString())
                }
            ))
        }.body<KuroResponse<MineInfo>>()
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
        val res: KuroResponse<DefaultKuroResponseData> = client.post(URL.GAMER_ROLE_LIST) {
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
        if (!response.success) throw KuroException(response.code, response.msg)
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
