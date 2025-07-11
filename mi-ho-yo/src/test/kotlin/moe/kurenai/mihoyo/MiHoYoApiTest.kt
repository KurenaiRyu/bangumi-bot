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
import moe.kurenai.mihoyo.MiHoYo.APP_ID
import moe.kurenai.mihoyo.module.*
import moe.kurenai.mihoyo.module.zzz.Challenge
import moe.kurenai.mihoyo.module.zzz.MemDetail
import moe.kurenai.mihoyo.util.MiHoYoHeaders
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class MiHoYoApiTest {

    val log = getLogger()
    companion object {
        const val lettersAndNumbers = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }

    val dataPath = Path.of("./data")

    val zzzPath = dataPath.resolve("ZZZ")
    val zzzPage = "v2.0.4_#/zzz"


    val uuid = UUID.nameUUIDFromBytes("Kurenai".toByteArray())
    val deviceFp = "38d80d535f7af"
    val androidVersion = 13
    val deviceModel = "redmi k50 ultra"
    val miHoYoBBSVersion = "2.71.1"
    val K2 = "rtvTthKxEyreVXQCnhluFgLXPOFKPHlA"
    val salt_4X = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"
    val UA = "Mozilla/5.0 (Linux; Android $androidVersion; $deviceModel Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/108.0.5359.128 Mobile Safari/537.36 miHoYoBBS/$miHoYoBBSVersion"

    val accountContext by lazy {
        val bindInfoList = Json.decodeFromString(BindInfoList.serializer(), dataPath.resolve("BindInfo.json").readText()).list
        val cookie = dataPath.resolve("MiHoYoBBSLogin.cookie").readLines().joinToString("; ") {
            it.substringBefore(";")
        }
        AccountContext(bindInfoList, cookie)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val miHoYoPlugin = createClientPlugin("MihoyoPlugin") {
        onRequest {req, content ->

            req.header("x-rpc-sys_version", androidVersion) // 安卓系统（或iOS）大版本号
            req.header("x-rpc-channel", "xiaomi")
            req.header("x-rpc-device_name", "xiaomi $deviceModel")
            req.header("x-rpc-device_model", deviceModel)
            req.header("x-rpc-device_id", uuid)
            req.header("x-rpc-device_fp", deviceFp)
            req.header("x-rpc-app_version", miHoYoBBSVersion)
            req.header("X-Requested-With", "com.mihoyo.hyperion")

            val host = req.url.host
            req.header(HttpHeaders.Origin, "https://$host")
            req.header(HttpHeaders.Host, host)
            req.header(HttpHeaders.Referrer, "https://app.mihoyo.com")
            req.header(HttpHeaders.UserAgent, UA)

            val t = System.currentTimeMillis()

            val clientType = req.headers["x-rpc-client_type"]
            val ds = when (clientType) {
                "2" -> {
                    val r = lettersAndNumbers.asSequence().shuffled().take(6).toString()
                    val main = "salt=$K2&t=$t&r=$r".toByteArray(StandardCharsets.UTF_8)
                    main.md5().toHexString()
                }
                "5" -> {
                    var r = (100000..200000).random()
                    if (r == 100000) r=642367
                    val parameters = req.url.parameters
                    val q = if (parameters.names().isEmpty()) ""
                    else {
                        parameters.names().asSequence().sorted().joinToString("&") {
                            "$it=${parameters[it]}"
                        }
                    }
                    val b = ""
                    val main = "salt=$salt_4X&t=$t&b=$b&q=$q"
                    "$t,$r,${main.toByteArray(StandardCharsets.UTF_8).md5().toHexString()}"
                }

                else -> {""}
            }

            if (ds.isNotBlank()) req.header("DS", ds)

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
        }
        install(ContentNegotiation) {
            json(Json{
                ignoreUnknownKeys = true
            })
        }
    }

    @Test
    fun testBinding(): Unit = runBlocking {
        val cookie = dataPath.resolve("MiHoYoBBSLogin.cookie").readLines().joinToString("; ") {
            it.substringBefore(";")
        }
        println(client.get("https://api-takumi.mihoyo.com/binding/api/getUserGameRolesByCookie") {
            header("x-rpc-client_type", 5)
            header(HttpHeaders.Cookie, cookie)
        }.body<BindInfoList>())
    }

    @Test
    fun testZZZChallenge(): Unit = runBlocking {
        val bindInfoList = Json.decodeFromString(BindInfoList.serializer(), dataPath.resolve("BindInfo.json").readText()).list
        val info = bindInfoList.find { "nap_cn" == it.gameBiz }!!
        val cookie = dataPath.resolve("MiHoYoBBSLogin.cookie").readLines().joinToString("; ") {
            it.substringBefore(";")
        }
        val challengeRes = client.get("https://api-takumi-record.mihoyo.com/event/game_record_zzz/api/zzz/challenge") {
            parameter("schedule_type", 1)
            parameter("need_all", true)
            parameter("server", info.region)
            parameter("role_id", info.gameUid)

//            val geeTest = Json.encodeToString(buildJsonObject {
//                put("viewUid", info.gameUid)
//                put("server", info.region)
//                put("gameId", 8)
//                put("page", zzzPage)
//                put("isHost", 1)
//                put("viewSource", 3)
//                put("actionSource", 127)
//            })

            header("x-rpc-client_type", 5)
//            header("x-rpc-page", zzzPage)
//            header("x-rpc-geetest_ext", geeTest)
//            header("x-rpc-platform", 2)
            header(HttpHeaders.Cookie, cookie)
        }.body<BaseResponse<Challenge>>()

        zzzPath.resolve("Challenge_${challengeRes.data!!.scheduleId}.json").writeText(Json.encodeToString(challengeRes.data!!), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    @Test
    fun testSignList(): Unit = runBlocking {
        with(accountContext) {
            val ret = MiHoYo.getSignList()
            zzzPath.resolve("SignList.json").writeText(Json.encodeToString(ret), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        }
    }

    @Test
    fun testZZZSignInfo(): Unit = runBlocking {
        with(accountContext) {
            val ret = MiHoYo.getZZZSignInfo()
            zzzPath.resolve("SignList.json").writeText(Json.encodeToString(ret), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        }
    }

    @Test
    fun testZZZAvatarList(): Unit = runBlocking {
        with(accountContext) {
            val list = MiHoYo.getZZZAvatarList()
            zzzPath.resolve("AvatarList.json").writeText(Json.encodeToString(list), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        }

//        val url = "https://api-takumi-record.mihoyo.com/event/game_record_zzz/api/zzz/avatar/basic"
//        val ret = client.get(url) {
//            parameter("role_id", bind.gameUid)
//            parameter("server", bind.region)
//
//            header("x-rpc-client_type", 5)
//            header(HttpHeaders.Cookie, cookie)
//        }.body<BaseResponse<AvatarList>>()

//        zzzPath.resolve("AvatarList.json").writeText(Json.encodeToString(ret.data!!), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    @Test
    fun testZZZAvatarDetail(): Unit = runBlocking {
        val agentId = 1221
        with(accountContext) {
            val ret = MiHoYo.getZZZAvatarDetail(agentId)
            zzzPath.resolve("AvatarDetail_$agentId.json").writeText(Json.encodeToString(ret.avatarList.first()), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        }

//        val url = "https://api-takumi-record.mihoyo.com/event/game_record_zzz/api/zzz/avatar/info?id_list[]=1091&need_wiki=true"
//        val ret = client.get(url) {
//            parameter("role_id", accountContext.zzzInfo.gameUid)
//            parameter("server", accountContext.zzzInfo.region)
//
//            header("x-rpc-client_type", 5)
//            header(HttpHeaders.Cookie, accountContext.cookie)
//        }.body<BaseResponse<AvatarDetail>>()
//
//        zzzPath.resolve("AvatarDetail_1091.json").writeText(Json.encodeToString(ret.data!!), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    @Test
    fun testZZZMemDetail(): Unit = runBlocking {
        with(accountContext) {
            val ret = MiHoYo.getZZZMemDetail()
            zzzPath.resolve("MemDetail_${ret.zoneId}.json").writeText(Json.encodeToString(ret), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        }
//        val memDetailRes = client.get("https://api-takumi-record.mihoyo.com/event/game_record_zzz/api/zzz/mem_detail?schedule_type=1") {
//            parameter("uid", info.gameUid)
//            parameter("region", info.region)
//
//            header(HttpHeaders.Cookie, cookie)
//        }.body<BaseResponse<MemDetail>>()
//        val zzzPath = dataPath.resolve("ZZZ")
//        zzzPath.createDirectories()
//        zzzPath.resolve("MemDetail_${memDetailRes.data!!.zoneId}.json").writeText(Json.encodeToString(memDetailRes.data!!), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    @Test
    fun testCreateLogin() = runBlocking {
        val ret = client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/createQRLogin"){
            header("x-rpc-app_id", APP_ID)
            header("x-rpc-client_type", 2)
        }.body<BaseResponse<CreateQRCodeLogin>>()
        if (ret.retcode != 0) return@runBlocking
        val createQALogin = ret.data?:throw IllegalStateException("No data.")
        genQRCodeImg(createQALogin.url)
        delay(5.seconds)

        while (true) {
            val qrStatusResponse = kotlin.runCatching {
                client.post("https://passport-api.miyoushe.com/account/ma-cn-passport/web/queryQRLoginStatus") {
                    header(MiHoYoHeaders.X_RPC_APP_ID, APP_ID)
                    header("x-rpc-client_type", 2)
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("ticket", ret.data?.ticket)
                    })
                }
            }.getOrNull()
            if (qrStatusResponse != null) {
                val qrStatusRet = qrStatusResponse.body<BaseResponse<QueryQRCodeStatus>>()
                if (qrStatusRet.retcode == 0) {
                    val status = qrStatusRet.data!!.status
                    if (status == QRCodeStatus.CONFIRMED) {
                        for (cookie in qrStatusResponse.setCookie()) {
                            Path.of("./data/MiHoYoBBSLogin.cookie").writeText("${cookie.name}=${cookie.value}\r\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
                        }
                        Path.of("./data/MiHoYoBBSLogin.json").writeText(Json.encodeToString(qrStatusRet.data), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
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
        MatrixToImageWriter.writeToPath(qrCode, "jpg", Path.of( "./data/qrcode.jpg", ))
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
    fun getDeviceFp(): Unit = runBlocking {
        println(MiHoYo.getDeviceFp())
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
