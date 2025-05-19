//package moe.kurenai.bot
//
//import io.ktor.client.*
//import io.ktor.client.call.*
//import io.ktor.client.request.*
//import io.ktor.client.statement.*
//import io.ktor.http.*
//import kotlinx.coroutines.runBlocking
//import java.io.File
//import kotlin.test.Test
//
//class TestJsonRead {
//
//    @Test
//    fun test() {
//        val rt = Runtime.getRuntime()
//        var prevFree = rt.freeMemory() / 1024 / 1024
//        var total = rt.totalMemory() / 1024 / 1024
//        var free = rt.freeMemory() / 1024 / 1024
//        println(
//            String.format(
//                "Total: %sm, Free: %sm, Diff: %sm",
//                total,
//                free,
//                prevFree - free
//            )
//        )
//
//        prevFree = free
//
//        val jsonLines = MAPPER.readerFor(JsonNode::class.java).readValues<JsonNode>(File("./character.jsonlines"))
//        val infobox = jsonLines.next().get("infobox")
//            .textValue()
//            .removePrefix("{{")
//            .removeSuffix("}}")
//            .replace("\r", "")
//            .replace("\n", "")
//            .replace("Infobox Crt|", "")
//        println(infobox)
//        var equalIndex = 0
//        var index2 = 0
//        val keys = ArrayList<String>()
//        val values = ArrayList<String>()
//        var first = true
//        var objFlag = 0
//        for (i in infobox.indices) {
//            when (infobox[i]) {
//                '=' -> {
//                    if (objFlag == 0) {
//                        if (first) {
//                            keys.add(infobox.substring(0, i - 1))
//                            first = false
//                        } else {
//                            keys.add(infobox.substring(index2 + 1, i))
//                            values.add(infobox.substring(equalIndex + 1, index2))
//                        }
//                    }
//                    equalIndex = i
//                }
//
//                '|' -> {
//                    index2 = i
//                }
//
//                '{' -> {
//                    objFlag++
//                }
//
//                '}' -> {
//                    objFlag--
//                }
//            }
//            if (i == infobox.length - 1)
//                values.add(infobox.substring(equalIndex + 1, i))
//        }
//        val map = HashMap<String, String>()
//        for (i in keys.indices) {
//            map[keys[i]] = values[i]
//        }
//        println(map)
//        println(map["别名"]?.replace("{[", "")?.replace("]}", "")?.split("][")?.joinToString("\n"))
//
//        total = rt.totalMemory() / 1024 / 1024
//        free = rt.freeMemory() / 1024 / 1024
//        println(
//            String.format(
//                "Total: %sm, Free: %sm, Diff: %sm",
//                total,
//                free,
//                prevFree - free
//            )
//        );
//    }
//
//    @Test
//    fun getToken() {
//        val OS_NAME = System.getProperty("os.name")
//        val OS_ARCH = System.getProperty("os.arch")
//        val OS_VERSION = System.getProperty("os.version")
//        val UA = "Kurenai Bangumi SDK Client/0.0.1 ($OS_NAME $OS_ARCH $OS_VERSION)"
//        val DEFAULT_MIME_TYPE = "application/json"
//        runBlocking {
//            val req = AccessTokenRequest(
//                AccessTokenGrantType.AUTHORIZATION_CODE,
//                "bgm211261c459e297e44",
//                "08cc6c23fb05fd3aeb1b13c8a06a94c5",
//                "http://bgm.kurenai.moe:2052/callback",
//                "2a5d73ea827126172605e3c335a8489621e1e5ee",
//                null,
//                null
//            )
//            println(HttpClient().use { client ->
//                val resp = client.request {
//                    url {
//                        url.takeFrom("https://bgm.tv/oauth/${req.path}")
//                        if (req.httpMethod == moe.kurenai.bgm.request.HttpMethod.GET) {
//                            method = HttpMethod.Get
//                            for (entry in DefaultMapper.convertToMap(req)) {
//                                parameters.append(entry.key.toString(), entry.value.toString())
//                            }
//                        } else {
//                            method = HttpMethod.Post
//                            setBody(DefaultMapper.convertToByteArray(req))
//                        }
//                    }
//                    headers {
//                        append(HttpHeaders.Accept, DEFAULT_MIME_TYPE)
//                        append(HttpHeaders.ContentType, DEFAULT_MIME_TYPE)
//                        append(HttpHeaders.UserAgent, UA)
//                        req.token?.let { append(HttpHeaders.Authorization, "Bearer ${req.token}") }
//                    }
//                }
//                resp.parse(req.responseType)
//            }.accessToken)
//
//        }
//    }
//
//    private suspend fun <T> HttpResponse.parse(reference: TypeReference<T>): T {
//        val body = this.body<ByteArray>()
//        return when (this.status) {
//            HttpStatusCode.OK -> {
//                kotlin.runCatching { MAPPER.readValue(body, reference) }
//                    .recover {
//                        throw if (it is JacksonException) it
//                        else kotlin.runCatching { MAPPER.readValue(body, BgmException::class.java) }.getOrNull() ?: it
//                    }.getOrThrow()
//            }
//
//            HttpStatusCode.Unauthorized -> {
//                throw BgmException(MAPPER.readValue(body, UnauthorizedError::class.java))
//            }
//
//            HttpStatusCode.NotFound -> {
//                throw BgmException(MAPPER.readValue(body, NotFoundError::class.java))
//            }
//
//            HttpStatusCode.UnprocessableEntity -> {
//                throw BgmException(MAPPER.readValue(body, ValidationError::class.java))
//            }
//
//            else -> {
//                throw BgmException(BgmError("Unknown response type"))
//            }
//        }
//    }
//}
