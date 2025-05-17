package moe.kurenai.bot.repository.bangumi

import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter
import com.sksamuel.aedile.core.caffeineBuilder
import io.ktor.client.statement.*
import moe.kurenai.bangumi.apis.DefaultApi
import moe.kurenai.bangumi.apis.OauthBangumiApi
import moe.kurenai.bangumi.infrastructure.ApiClient
import moe.kurenai.bangumi.infrastructure.HttpResponse
import moe.kurenai.bgm.model.character.CharacterDetail
import moe.kurenai.bgm.model.character.CharacterPerson
import moe.kurenai.bgm.model.person.PersonDetail
import moe.kurenai.bgm.model.subject.Subject
import org.apache.commons.pool2.PooledObject
import org.apache.commons.pool2.PooledObjectFactory
import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.impl.GenericObjectPool
import kotlin.time.Duration.Companion.days

internal object BangumiApi {

    private val defaultApiPool = GenericObjectPool(buildFactory { DefaultApi() })
    private val oathApiPool = GenericObjectPool(buildFactory { OauthBangumiApi() })

    val counter = ConcurrentStatsCounter()

    val characterCache = caffeineBuilder<Int, CharacterDetail> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = counter
    }.build()

    val characterPersonCache = caffeineBuilder<Int, List<CharacterPerson>> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = counter
    }.build()

    val personCache = caffeineBuilder<Int, PersonDetail> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = counter
    }.build()

    val subjectCache = caffeineBuilder<Int, Subject> {
        maximumSize = 200
        expireAfterWrite = 7.days
        expireAfterAccess = 1.days
        statsCounter = counter
    }.build()

    internal suspend fun <T> useApi(token: String? = null, block: suspend (DefaultApi) -> T): T {
        val api = defaultApiPool.borrowObject()
        api.setBearerToken(token ?: "")
        try {
            return block(api)
        } finally {
            defaultApiPool.returnObject(api)
        }
    }

    suspend fun <T> useOauthApi(token: String? = null, block: suspend (OauthBangumiApi) -> T): T {
        val api = oathApiPool.borrowObject()
        api.setBearerToken(token ?: "")
        try {
            return block(api)
        } finally {
            oathApiPool.returnObject(api)
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
    suspend fun <T> HttpResponse<*>.result(): T {
        if (this.success) {
            return this.body() as T
        } else {
            throw IllegalStateException("[${this.status}] ${this.response.bodyAsText()}")
        }
    }

}
