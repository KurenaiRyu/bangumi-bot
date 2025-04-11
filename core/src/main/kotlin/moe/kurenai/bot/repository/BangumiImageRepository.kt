package moe.kurenai.bot.repository

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * @author Kurenai
 * @since 2023/3/3 11:32
 */

object BangumiImageRepository {

    private val client = HttpClient {
        followRedirects = false
    }

    suspend fun fetchImageUrl(url: String) = client.get(url).headers[HttpHeaders.Location] ?: url

}
