package moe.kurenai.kuro//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//import okhttp3.FormBody
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import java.time.ZonedDateTime
//import java.time.ZoneId
//import java.util.concurrent.TimeUnit
//import kotlin.system.exitProcess
//
//@Serializable
//data class Response(
//    @SerialName("code")
//    val code: Int,
//    @SerialName("msg")
//    val msg: String,
//    @SerialName("success")
//    val success: Boolean? = null,
//    @SerialName("data")
//    val data: Map<String, Any>? = null
//)
//
//class KurobbsClientException(message: String, cause: Throwable? = null) : Exception(message, cause)
//
//class KurobbsClient(private val token: String) {
//    companion object {
//        const val FIND_ROLE_LIST_API_URL = "https://api.kurobbs.com/gamer/role/default"
//        const val SIGN_URL = "https://api.kurobbs.com/encourage/signIn/v2"
//        const val USER_SIGN_URL = "https://api.kurobbs.com/user/signIn"
//        const val USER_MINE_URL = "https://api.kurobbs.com/user/mineV2"
//    }
//
//    private val client: OkHttpClient
//    private val json = Json { ignoreUnknownKeys = true }
//    private val result: MutableMap<String, String> = mutableMapOf()
//    private val exceptions: MutableList<Exception> = mutableListOf()
//
//    init {
//        if (token.isBlank()) {
//            throw KurobbsClientException("TOKEN is required to call Kurobbs APIs.")
//        }
//
//        client = OkHttpClient.Builder()
//            .connectTimeout(15, TimeUnit.SECONDS)
//            .readTimeout(15, TimeUnit.SECONDS)
//            .writeTimeout(15, TimeUnit.SECONDS)
//            .addInterceptor { chain ->
//                val originalRequest = chain.request()
//                val newRequest = originalRequest.newBuilder()
//                    .header("osversion", "Android")
//                    .header("devcode", "2fba3859fe9bfe9099f2696b8648c2c6")
//                    .header("countrycode", "CN")
//                    .header("ip", "10.0.2.233")
//                    .header("model", "2211133C")
//                    .header("source", "android")
//                    .header("lang", "zh-Hans")
//                    .header("version", "1.0.9")
//                    .header("versioncode", "1090")
//                    .header("token", token)
//                    .header("content-type", "application/x-www-form-urlencoded; charset=utf-8")
//                    .header("accept-encoding", "gzip")
//                    .header("user-agent", "okhttp/3.10.0")
//                    .build()
//                chain.proceed(newRequest)
//            }
//            .build()
//    }
//
//    private fun post(url: String, data: Map<String, Any>): Response {
//        try {
//            val formBody = FormBody.Builder().apply {
//                data.forEach { (key, value) ->
//                    add(key, value.toString())
//                }
//            }.build()
//
//            val request = Request.Builder()
//                .url(url)
//                .post(formBody)
//                .build()
//
//            val response = client.newCall(request).execute()
//            response.use { httpResponse ->
//                if (!httpResponse.isSuccessful) {
//                    throw KurobbsClientException("Request to $url failed with status ${httpResponse.code}")
//                }
//
//                val responseBody = httpResponse.body?.string()
//                    ?: throw KurobbsClientException("Empty response from $url")
//
//                val res = json.decodeFromString<Response>(responseBody)
//                println("POST $url -> code=${res.code}, success=${res.success}, msg=${res.msg}")
//                return res
//            }
//        } catch (e: Exception) {
//            if (e is KurobbsClientException) throw e
//            throw KurobbsClientException("Failed to process request to $url: ${e.message}", e)
//        }
//    }
//
//    fun getMineInfo(type: Int = 1): Map<String, Any> {
//        val res = post(USER_MINE_URL, mapOf("type" to type))
//        return res.data ?: throw KurobbsClientException("User info is missing in response.")
//    }
//
//    fun getUserGameList(userId: Int): Map<String, Any> {
//        val res = post(FIND_ROLE_LIST_API_URL, mapOf("queryUserId" to userId))
//        return res.data ?: throw KurobbsClientException("User game list is missing in response.")
//    }
//
//    fun checkIn(): Response {
//        val mineInfo = getMineInfo()
//        val mine = mineInfo["mine"] as? Map<String, Any>
//        val userId = (mine?.get("userId") as? Number)?.toInt() ?: 0
//        val userGameList = getUserGameList(userId)
//
//        val beijingZone = ZoneId.of("Asia/Shanghai")
//        val beijingTime = ZonedDateTime.now(beijingZone)
//
//        @Suppress("UNCHECKED_CAST")
//        val roleList = (userGameList["defaultRoleList"] as? List<Map<String, Any>>) ?: emptyList()
//        if (roleList.isEmpty()) {
//            throw KurobbsClientException("No default role found for the user.")
//        }
//
//        val roleInfo = roleList[0]
//
//        val data = mapOf(
//            "gameId" to ((roleInfo["gameId"] as? Number)?.toInt() ?: 2),
//            "serverId" to (roleInfo["serverId"] ?: ""),
//            "roleId" to ((roleInfo["roleId"] as? Number)?.toInt() ?: 0),
//            "userId" to ((roleInfo["userId"] as? Number)?.toInt() ?: 0),
//            "reqMonth" to String.format("%02d", beijingTime.monthValue)
//        )
//        return post(SIGN_URL, data)
//    }
//
//    fun signIn(): Response {
//        return post(USER_SIGN_URL, mapOf("gameId" to 2))
//    }
//
//    private fun processSignAction(
//        actionName: String,
//        actionMethod: () -> Response,
//        successMessage: String,
//        failureMessage: String
//    ) {
//        try {
//            val resp = actionMethod()
//            if (resp.success == true) {
//                result[actionName] = successMessage
//                println("$actionName -> $successMessage")
//            } else {
//                exceptions.add(KurobbsClientException("$failureMessage, ${resp.msg}"))
//            }
//        } catch (e: Exception) {
//            exceptions.add(e)
//        }
//    }
//
//    fun start() {
//        processSignAction(
//            actionName = "checkin",
//            actionMethod = { checkIn() },
//            successMessage = "签到奖励签到成功",
//            failureMessage = "签到奖励签到失败"
//        )
//
//        processSignAction(
//            actionName = "sign_in",
//            actionMethod = { signIn() },
//            successMessage = "社区签到成功",
//            failureMessage = "社区签到失败"
//        )
//
//        log()
//    }
//
//    val msg: String
//        get() = if (result.isNotEmpty()) {
//            result.values.joinToString(", ") + "!"
//        } else {
//            ""
//        }
//
//    private fun log() {
//        val message = msg
//        if (message.isNotEmpty()) {
//            println(message)
//        }
//        if (exceptions.isNotEmpty()) {
//            throw KurobbsClientException(exceptions.joinToString("; ") { it.toString() })
//        }
//    }
//
//    fun close() {
//        client.dispatcher.executorService.shutdown()
//        client.connectionPool.evictAll()
//    }
//}
//
//// Usage example
//fun main() {
//    val token = System.getenv("TOKEN") ?: throw IllegalArgumentException("TOKEN environment variable is required")
//
//    try {
//        val kurobbs = KurobbsClient(token)
//        kurobbs.start()
//        val message = kurobbs.msg
//        if (message.isNotEmpty()) {
//            println("Notification: $message")
//        }
//    } catch (e: KurobbsClientException) {
//        System.err.println("Error: ${e.message}")
//        exitProcess(1)
//    } catch (e: Exception) {
//        System.err.println("Unexpected error: ${e.message}")
//        e.printStackTrace()
//        exitProcess(1)
//    }
//}
