package moe.kurenai.skyland

import io.ktor.http.decodeURLPart
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import moe.kurenai.common.util.json
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.GzipSink
import okio.Path.Companion.toPath
import okio.buffer
import okio.cipherSink
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 数美加密方法类
 * @see: https://github.com/YueHen14/skyland-auto-sign/blob/main/SecuritySm.py
 */
object SecuritySM {

    private val log = LoggerFactory.getLogger(SecuritySM::class.java)
    private const val SM_PATH = "sm_config.json"

    // 数美配置
    @OptIn(ExperimentalSerializationApi::class)
    private val SM_CONFIG by lazy {
        val fs = FileSystem.RESOURCES
        if (fs.exists(SM_PATH.toPath())) {
            val jsonStr = fs.read("sm_config.json".toPath()) {
                String(readByteArray(), StandardCharsets.UTF_8)
            }
            runCatching {
                json.decodeFromString<SMConfig>(jsonStr)
            }.onFailure {
                log.error("Error while reading sm config file", it)
            }.getOrThrow()
        } else SMConfig()
    }

    private val PK by lazy { loadPublicKey(SM_CONFIG.publicKey) }

    private val DES_RULE = mapOf(
        "appId" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "uy7mzc4h",
            "obfuscated_name" to "xx"
        ),
        "box" to mapOf(
            "is_encrypt" to 0,
            "obfuscated_name" to "jf"
        ),
        "canvas" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "snrn887t",
            "obfuscated_name" to "yk"
        ),
        "clientSize" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "cpmjjgsu",
            "obfuscated_name" to "zx"
        ),
        "organization" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "78moqjfc",
            "obfuscated_name" to "dp"
        ),
        "os" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "je6vk6t4",
            "obfuscated_name" to "pj"
        ),
        "platform" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "pakxhcd2",
            "obfuscated_name" to "gm"
        ),
        "plugins" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "v51m3pzl",
            "obfuscated_name" to "kq"
        ),
        "pmf" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "2mdeslu3",
            "obfuscated_name" to "vw"
        ),
        "protocol" to mapOf(
            "is_encrypt" to 0,
            "obfuscated_name" to "protocol"
        ),
        "referer" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "y7bmrjlc",
            "obfuscated_name" to "ab"
        ),
        "res" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "whxqm2a7",
            "obfuscated_name" to "hf"
        ),
        "rtype" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "x8o2h2bl",
            "obfuscated_name" to "lo"
        ),
        "sdkver" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "9q3dcxp2",
            "obfuscated_name" to "sc"
        ),
        "status" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "2jbrxxw4",
            "obfuscated_name" to "an"
        ),
        "subVersion" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "eo3i2puh",
            "obfuscated_name" to "ns"
        ),
        "svm" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "fzj3kaeh",
            "obfuscated_name" to "qr"
        ),
        "time" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "q2t3odsk",
            "obfuscated_name" to "nb"
        ),
        "timezone" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "1uv05lj5",
            "obfuscated_name" to "as"
        ),
        "tn" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "x9nzj1bp",
            "obfuscated_name" to "py"
        ),
        "trees" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "acfs0xo4",
            "obfuscated_name" to "pi"
        ),
        "ua" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "k92crp1t",
            "obfuscated_name" to "bj"
        ),
        "url" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "y95hjkoo",
            "obfuscated_name" to "cf"
        ),
        "version" to mapOf(
            "is_encrypt" to 0,
            "obfuscated_name" to "version"
        ),
        "vpw" to mapOf(
            "cipher" to "DES",
            "is_encrypt" to 1,
            "key" to "r9924ab5",
            "obfuscated_name" to "ca"
        )
    )

    private val BROWSER_ENV = mapOf(
        "plugins" to "MicrosoftEdgePDFPluginPortableDocumentFormatinternal-pdf-viewer1,MicrosoftEdgePDFViewermhjfbmdgcfjbbpaeojofohoefgiehjai1",
        "ua" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0",
        "canvas" to "259ffe69",
        "timezone" to -480,
        "platform" to "Win32",
        "url" to "https://www.skland.com/",
        "referer" to "",
        "res" to "1920_1080_24_1.25",
        "clientSize" to "0_0_1080_1920_1920_1080_1920_1080",
        "status" to "0011"
    )

    private fun loadPublicKey(publicKeyString: String): PublicKey {
        val decodedKey = publicKeyString.decodeBase64()?:error("Could not decode public key")
        val spec = X509EncodedKeySpec(decodedKey.toByteArray())
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(spec)
    }

    private fun des(o: Map<String, Any?>): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for ((key, value) in o) {
            if (key in DES_RULE) {
                val rule = DES_RULE[key]!!
                val res = if ((rule["is_encrypt"] as Int) == 1) {
                    val keyStr = rule["key"] as String
                    val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
                    val keySpec = SecretKeySpec(keyStr.toByteArray().copyOf(24), "DESede")
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec)

                    val data = value.toString().toByteArray()
                    // 补足字节
                    val remain = data.size % cipher.blockSize
                    val paddedData = data.copyOf(data.size + (cipher.blockSize - remain))
                    val encrypted = Buffer()
                    encrypted.cipherSink(cipher).buffer().use {
                        it.write(paddedData)
                    }
                    encrypted.md5().hex()
                } else value
                result[rule["obfuscated_name"] as String] = res
            } else {
                result[key] = value
            }
        }
        return result
    }

    private fun aes(v: ByteString, k: ByteString): String {
        val iv = "0102030405060708"
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val keySpec = SecretKeySpec(k.toByteArray(), "AES")
        val ivSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val byteSize = v.size + 1
        val mod = byteSize % 16
        val data = if (mod != 0) {
            ByteArray(16 - mod + byteSize)
        } else {
            ByteArray(byteSize)
        }

//        var data = ByteArray(byteSize)
//        v.copyInto(0, data, 0, v.size)
//        data[v.size] = 0
//        while (data.size % 16 != 0) {
//            data = data.copyOf(data.size + 1)
//            data[data.size - 1] = 0
//        }

        val encrypted = cipher.doFinal(data)
        return encrypted.toByteString().hex()
    }

    private fun gzip(map: Map<String, Any?>): String {
        val jsonStr = mapToJson(map)
        val buff = Buffer()
        GzipSink(buff).buffer().use {
            it.writeUtf8(jsonStr)
        }
        return buff.snapshot().base64()
    }

    private fun getTn(o: Map<String, Any?>): String {
        val sortedKeys = o.keys.sorted()
        val sb = StringBuilder("")

        for (i in sortedKeys) {
            val v: String = when (val value = o[i]) {
                is Number -> {
                    (value.toDouble() * 10000).toLong().toString()
                }
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    getTn(value as Map<String, Any?>)
                }
                else -> value.toString()
            }
            sb.append(v)
        }
        return sb.toString()
    }

    private fun getSmid(): String {
        val dateTime = LocalDateTime.now()
        val year = dateTime.year
        val month = dateTime.monthValue
        val day = dateTime.dayOfMonth
        val hour = dateTime.hour
        val minute = dateTime.minute
        val second = dateTime.second

        val timeStr = String.format("%04d%02d%02d%02d%02d%02d", year, month, day, hour, minute, second)
        val uid = UUID.randomUUID().toString()
        val uidHash = uid.encodeUtf8().md5().hex()
        val v = timeStr + uidHash + "00"
        val smskWeb = "smsk_web_$v".encodeUtf8().md5().hex().substring(0, 14)
        return v + smskWeb + "0"
    }

    private fun getEp(uuid: ByteString): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, PK)

        return cipher.doFinal(uuid.toByteArray()).toByteString().base64()
    }

    fun buildDidRequest(): JsonObject {
        // storageName = '.thumbcache_' + md5(SM_CONFIG['organization']) // 用于从本地存储获得值
        // uid = uuid()
        // priId=md5(uid)[0:16]
        // ep=rsa(uid,publicKey)
        // SMID = localStorage.get(storageName);// 获得本地存储存的值
        // _0x30b2eb为递归md5

        val uuid = UUID.randomUUID().toString().encodeUtf8()
        val priId = uuid.md5().substring(0, 16)

        val ep = getEp(uuid)

        val desTarget = BROWSER_ENV.toMutableMap()
        val currentTime = System.currentTimeMillis()
        desTarget.apply {
            put("vpw", UUID.randomUUID().toString())
            put("svm", currentTime)
            put("trees", UUID.randomUUID().toString())
            put("pmf", currentTime)

            put("protocol", 102)
            put("organization", SM_CONFIG.organization)
            put("appId", SM_CONFIG.appId)
            put("os", "web")
            put("version", "3.0.0")
            put("sdkver", "3.0.0")
            put("box", "")  //似乎是个SMID，但是第一次的时候是空,不过不影响结果
            put("rtype", "all")
            put("smid", getSmid())
            put("subVersion", "1.0.0")
            put("time", 0)
        }
        desTarget["tn"] = getTn(desTarget).encodeUtf8().md5().hex()

        val desResult = des(desTarget)
        val gzipData = gzip(desResult).encodeUtf8()
        val aesResult = aes(gzipData, priId)

        return buildJsonObject {
            put("appId", "default")
            put("compress", 2)
            put("data", aesResult)
            put("encode", 5)
            put("ep", ep)
            put("organization", SM_CONFIG.organization)
            put("os", "web")  //固定值
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun mapToJson(map: Map<String, Any?>): String {
        val jsonElement = buildJsonObject {
            for ((key, value) in map) {
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        put(key, mapToJson(value as Map<String, Any?>))
                    }
                    null -> put(key, null)
                    else -> put(key, value.toString())
                }
            }
        }
        return jsonElement.toString()
    }

}
