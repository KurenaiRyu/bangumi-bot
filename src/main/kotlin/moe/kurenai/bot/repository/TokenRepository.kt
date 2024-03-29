package moe.kurenai.bot.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import moe.kurenai.bgm.exception.BgmException
import moe.kurenai.bgm.model.auth.AccessToken
import moe.kurenai.bot.BangumiBot
import moe.kurenai.bot.util.json
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * @author Kurenai
 * @since 2023/1/26 18:37
 */
object TokenRepository {

    private val log = LoggerFactory.getLogger(TokenRepository::class.java)
    private const val tokenFilePath = "config/token.json"
    private val lock = Mutex()

    init {
        loadToken()
    }

    @Serializable
    data class TokenEntity(
        val userId: Long,
        val accessToken: AccessToken,
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

    suspend fun findById(userId: Long): AccessToken? {
        return tokens[userId]?.let { entity ->
            lock.withLock {
                if (entity.expires > LocalDateTime.now().atOffset(ZoneOffset.ofHours(8)).toEpochSecond()) {
                    entity.accessToken
                } else {
                    kotlin.runCatching {
                        BangumiBot.bgmClient.refreshToken(entity.accessToken.refreshToken)
                    }.onFailure {
                        if (it.cause is BgmException) {
                            tokens.remove(userId)
                        }
                    }.onSuccess {
                        tokens[userId] = entity.copy(accessToken = it, expires = it.expiresIn + getNowSeconds())
                    }.getOrNull().also {
                        save()
                    }
                }
            }
        }
    }

    private fun save() {
        CoroutineScope(Dispatchers.IO).launch {
            lock.withLock {
                Path.of(tokenFilePath).writeText(json.encodeToString(ListSerializer(TokenEntity.serializer()), tokens.values.toList()))
            }
        }
    }

    suspend fun put(userId: Long, accessToken: AccessToken) {
        lock.withLock {
            val old = tokens[userId]
            tokens[userId] = if (old == null) {
                TokenEntity(userId, accessToken, accessToken.computeExpires())
            } else {
                old.copy(accessToken = accessToken, expires = accessToken.expiresIn + getNowSeconds())
            }
        }
        save()
    }

    fun putIfAbsent(userId: Long, accessToken: AccessToken) {
        tokens.putIfAbsent(userId, TokenEntity(userId, accessToken, accessToken.computeExpires()))
        save()
    }

    private fun getNowSeconds(): Long = LocalDateTime.now().atOffset(ZoneOffset.ofHours(8)).toEpochSecond()

    private fun AccessToken.computeExpires() = this.expiresIn + getNowSeconds()

}
