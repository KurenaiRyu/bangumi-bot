package moe.kurenai.mihoyo.util

import io.ktor.http.*
import moe.kurenai.common.util.md5
import moe.kurenai.mihoyo.MiHoYo
import java.nio.charset.StandardCharsets

object EncryptUtil {
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
                val main = "salt=${MiHoYo.API_SALT}&t=$t&r=$r"
                main.toByteArray(StandardCharsets.UTF_8).md5().toHexString()
            }
            SecretType.DS2 -> {
                val r = (100001..200000).random()
                val b = ""
                val q = url.parameters.names().asSequence().sorted().joinToString("&") {
                    "$it=${url.parameters[it]}"
                }
                val main = "salt=${MiHoYo.API_SALT2}&t=$t&b=$b&q=$q"
                main.toByteArray(StandardCharsets.UTF_8).md5().toHexString()
            }
        }

    }

    enum class SecretType {
        DS1, DS2
    }

}
