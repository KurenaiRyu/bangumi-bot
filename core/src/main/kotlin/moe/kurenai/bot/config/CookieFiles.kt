package moe.kurenai.bot.config

import java.nio.file.Path
import kotlin.io.path.Path

enum class CookieFiles {
    BILIBILI,;

    fun getPath(): Path {
        return Path("config", "COOKIE_${this.name}.txt")
    }

    companion object {
        fun ofName(name: String): CookieFiles? {
            return CookieFiles.entries.find { it.name.equals(name, true) }
        }
    }

}
