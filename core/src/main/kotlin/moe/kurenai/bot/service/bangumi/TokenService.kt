package moe.kurenai.bot.service.bangumi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.common.util.json
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds

/**
 * @author Kurenai
 * @since 2023/1/26 18:37
 */
internal object TokenService {

    private val log = LoggerFactory.getLogger(TokenService::class.java)
    private const val tokenFilePath = "config/token.json"
    private val lock = Mutex()

    @Serializable
    data class TokenEntity(
        val userId: Long,
        val accessToken: UserAccessToken,
        val expires: Long
    )

    private val tokens: ConcurrentHashMap<Long, TokenEntity> = ConcurrentHashMap(loadToken().associateBy { it.userId })

    private fun loadToken(): List<TokenEntity> {
        val tokenPath = Path.of(tokenFilePath)
        return kotlin.runCatching {
            if (tokenPath.exists().not()) {
                tokenPath.writeText(
                    json.encodeToString(ListSerializer(TokenEntity.serializer()), emptyList()),
                    options = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                )
                listOf()
            } else {
                json.decodeFromString(ListSerializer(TokenEntity.serializer()), Path.of(tokenFilePath).readText())
            }
        }.onFailure {
            log.warn("Load token error", it)
        }.recover {
            listOf()
        }.getOrThrow()
    }

    suspend fun findById(userId: Long): UserAccessToken? {
        return tokens[userId]?.let { entity ->
            val now = LocalDateTime.now().atOffset(ZoneOffset.ofHours(8)).minusHours(2).toEpochSecond()
            if (entity.expires < now) {
                entity.accessToken
            } else {
                lock.withLock {
                    val entity = tokens[userId]?: return@withLock null
                    withTimeout(30.seconds) {
                        if (entity.expires > now) {
                            entity.accessToken
                        } else {
                            kotlin.runCatching {
                                OauthService.refreshToken(entity.accessToken)
                            }.onFailure {
                                tokens.remove(userId)
                            }.onSuccess {
                                tokens[userId] = entity.copy(accessToken = it, expires = it.expiresIn + getNowSeconds())
                            }.getOrNull().also {
                                save()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun save() {
        withContext(Dispatchers.IO) {
            Path.of(tokenFilePath)
                .writeText(json.encodeToString(ListSerializer(TokenEntity.serializer()), tokens.values.toList()))
        }
    }

    suspend fun put(userId: Long, accessToken: UserAccessToken) = lock.withLock {
        val old = tokens[userId]
        tokens[userId] = old?.copy(accessToken = accessToken, expires = accessToken.expiresIn + getNowSeconds())
            ?: TokenEntity(userId, accessToken, accessToken.computeExpires())
        save()
    }

    suspend fun putIfAbsent(userId: Long, accessToken: UserAccessToken) = lock.withLock {
        tokens.putIfAbsent(userId, TokenEntity(userId, accessToken, accessToken.computeExpires()))
        save()
    }

    private fun getNowSeconds(): Long = LocalDateTime.now().atOffset(ZoneOffset.ofHours(8)).toEpochSecond()

    private fun UserAccessToken.computeExpires() = this.expiresIn + getNowSeconds()

}
