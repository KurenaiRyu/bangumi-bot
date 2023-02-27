package moe.kurenai.bot

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.junit.Test

/**
 * @author Kurenai
 * @since 2023/2/27 17:10
 */

class BiliBiliTest {

    @Test
    fun testShortLink() = runBlocking {
        val url = "https://b23.tv/khh2gBJ"
        HttpClient() {
            followRedirects = false
        }.use { client ->
            val response = client.get(url)
            val redirectUrl = Url(Jsoup.parse(response.bodyAsText()).select("a").attr("href"))
            val segments = redirectUrl.pathSegments
            println(redirectUrl.pathSegments.last().takeIf { it.isNotBlank() } ?: segments[segments.lastIndex - 1])
        }
    }

    @Test
    fun testShortLinkByWtf() = runBlocking {
        val url = "https://b23.wtf/khh2gBJ"
        HttpClient().use { client ->
            val response = client.get(url)
            println(response.bodyAsText())
            println(response.headers.toMap())
        }
    }

}
