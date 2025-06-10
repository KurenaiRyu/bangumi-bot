package moe.kurenai.bot.util

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.jsoup.Jsoup

/**
 * @author Kurenai
 * @since 2023/2/28 5:02
 */

object HttpUtil {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getLatestUA(): String {
        return json.decodeFromString<List<String>>(
            client.get("https://jnrbsn.github.io/user-agents/user-agents.json").bodyAsText()
        ).last { it.contains("Windows NT") && it.contains("Chrome") }
    }

    suspend fun getOgImageUrl(url: Url, client: HttpClient = this.client): List<String> {
        val doc = Jsoup.parse(client.get(url).bodyAsText())
        return doc.select("meta[property='og:image']").mapNotNull {
            it.attr("content")
        }
    }

}
