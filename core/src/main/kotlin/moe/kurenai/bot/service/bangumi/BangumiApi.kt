package moe.kurenai.bot.service.bangumi

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import moe.kurenai.bangumi.apis.DefaultApi
import moe.kurenai.bangumi.apis.OauthBangumiApi
import moe.kurenai.bangumi.infrastructure.ApiClient
import moe.kurenai.bangumi.infrastructure.HttpResponse
import moe.kurenai.bangumi.models.Character
import moe.kurenai.bangumi.models.CharacterPerson
import moe.kurenai.bangumi.models.PersonDetail
import moe.kurenai.bangumi.models.Subject
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.common.util.getLogger
import moe.kurenai.common.util.json
import org.apache.commons.pool2.PooledObject
import org.apache.commons.pool2.PooledObjectFactory
import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.impl.GenericObjectPool
import kotlin.time.Duration.Companion.days

internal object BangumiApi {

    private val log = getLogger()
    private val httpLogger = object : Logger {
        override fun log(message: String) {
            log.info(message)
        }
    }

    private val defaultApiPool = GenericObjectPool(buildFactory {
        DefaultApi(httpClientEngine = OkHttp.create()) {
            it.configHttpClient()
        }
    })
    internal val oauthApi = OauthBangumiApi(httpClientEngine = OkHttp.create()) {
        it.configHttpClient()
    }

    private fun HttpClientConfig<*>.configHttpClient() {
        install(Logging) {
            logger = httpLogger
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "kurenai/bangumi-bot")
        }
    }


    context(token: UserAccessToken?)
    internal inline fun <T> useApi(block: (DefaultApi) -> T): T {
        val api = defaultApiPool.borrowObject()
        api.setBearerToken(token?.accessToken ?: "")
        try {
            return block(api)
        } finally {
            defaultApiPool.returnObject(api)
        }
    }

    private fun <T : ApiClient> buildFactory(block: () -> T): PooledObjectFactory<T> {
        return object : PooledObjectFactory<T> {
            override fun activateObject(po: PooledObject<T>?) {}

            override fun destroyObject(po: PooledObject<T>?) {}

            override fun makeObject(): PooledObject<T> {
                return DefaultPooledObject(block())
            }

            override fun validateObject(po: PooledObject<T>?): Boolean {
                return true;
            }

            override fun passivateObject(po: PooledObject<T>?) {
                po?.`object`?.setBearerToken("")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T> HttpResponse<*>.result(): T {
        if (this.success) {
            return this.response.body<T>()
        } else {
            throw IllegalStateException("[${this.status}] ${this.response.bodyAsText()}")
        }
    }

}
