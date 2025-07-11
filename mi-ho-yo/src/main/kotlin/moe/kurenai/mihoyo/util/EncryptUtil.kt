package moe.kurenai.mihoyo.util

import io.ktor.http.*
import moe.kurenai.common.util.md5
import moe.kurenai.mihoyo.MiHoYo
import java.nio.charset.StandardCharsets

object EncryptUtil {
    @Suppress("NOTHING_TO_INLINE")
    inline fun createSecret1(url: String) = createSecret1(Url(url))

    fun createSecret1(url: Url) = doCreateSecret(url, SecretType.DS1)

    @Suppress("NOTHING_TO_INLINE")
    inline fun createSecret2(url: String, salt: String = MiHoYo.API_SALT_X4) = createSecret2(Url(url), salt)

    fun createSecret2(url: Url, salt: String = MiHoYo.API_SALT_X4) = doCreateSecret(url, SecretType.DS2, salt)

    @OptIn(ExperimentalStdlibApi::class)
    private fun doCreateSecret(url: Url, type: SecretType, salt: String = MiHoYo.API_SALT_X4): String {

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
                val main = "salt=$salt&t=$t&b=$b&q=$q"
                "$t,$r,${main.toByteArray(StandardCharsets.UTF_8).md5().toHexString()}"
            }
        }

    }

    enum class SecretType {
        DS1, DS2
    }

}
