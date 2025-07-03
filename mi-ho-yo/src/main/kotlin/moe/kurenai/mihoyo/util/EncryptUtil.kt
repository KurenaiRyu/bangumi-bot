package moe.kurenai.mihoyo.util

import io.ktor.http.*
import moe.kurenai.common.util.md5
import moe.kurenai.mihoyo.MiHoYoClient
import java.nio.charset.StandardCharsets
import java.util.UUID

object EncryptUtil {

    internal const val APP_ID = "bll8iq97cem8"
    internal const val APP_VERSION = "2.85.1"
    internal const val API_SALT = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
    internal const val API_SALT2 = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"
    internal const val API_SALT_X4 = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"
    internal const val API_SALT_X6 = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
    internal const val API_SALT_PROD = "JwYDpKvLj6MrMqqYU6jTKF17KNO2PXoS"
    internal val DEVICE_ID = UUID.nameUUIDFromBytes("Pixel 5".toByteArray()).toString()
    internal const val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/108.0.5359.128 Mobile Safari/537.36 miHoYoBBS/$APP_VERSION"

    @Suppress("NOTHING_TO_INLINE")
    inline fun createSecret1(url: String) = createSecret1(Url(url))

    fun createSecret1(url: Url) {
        doCreateSecret(url, SecretType.DS1)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun createSecret2(url: String) = createSecret2(Url(url))

    fun createSecret2(url: Url) {
        doCreateSecret(url, SecretType.DS2)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun doCreateSecret(url: Url, type: SecretType): String {

        val t = System.currentTimeMillis()
        return when (type) {
            SecretType.DS1 -> {
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                val r = chars.asSequence().shuffled().take(6).toString()
                val main = "salt=${this.API_SALT}&t=$t&r=$r"
                main.toByteArray(StandardCharsets.UTF_8).md5().toHexString()
            }
            SecretType.DS2 -> {
                val r = (100001..200000).random()
                val b = ""
                val q = url.parameters.names().asSequence().sorted().joinToString("&") {
                    "$it=${url.parameters[it]}"
                }
                val main = "salt=${this.API_SALT2}&t=$t&b=$b&q=$q"
                main.toByteArray(StandardCharsets.UTF_8).md5().toHexString()
            }
        }

    }

    enum class SecretType {
        DS1, DS2
    }

}
