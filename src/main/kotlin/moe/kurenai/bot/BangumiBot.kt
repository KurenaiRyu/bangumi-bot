package moe.kurenai.bot

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import moe.kurenai.bgm.BgmClient
import moe.kurenai.bgm.exception.UnauthorizedException
import moe.kurenai.bgm.model.auth.AccessToken
import moe.kurenai.bgm.model.character.CharacterDetail
import moe.kurenai.bgm.model.person.PersonDetail
import moe.kurenai.bgm.model.subject.*
import moe.kurenai.bgm.request.Request
import moe.kurenai.bgm.request.charater.GetCharacterDetail
import moe.kurenai.bgm.request.person.GetPersonDetail
import moe.kurenai.bgm.request.subject.GetSubject
import moe.kurenai.bgm.request.subject.GetSubjectPersons
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.config.JsonJacksonKotlinCodec
import moe.kurenai.bot.util.getAwait
import moe.kurenai.tdlight.LongPollingCoroutineTelegramBot
import moe.kurenai.tdlight.client.TDLightCoroutineClient
import moe.kurenai.tdlight.model.MessageEntityType
import moe.kurenai.tdlight.model.ResponseWrapper
import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.model.message.MessageEntity
import moe.kurenai.tdlight.request.message.AnswerInlineQuery
import moe.kurenai.tdlight.request.message.SendMessage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.redisson.Redisson
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import moe.kurenai.tdlight.request.Request as TDRequest

object BangumiBot {

    const val SUBJECT_KEY = "SUBJECT"
    const val PERSON_KEY = "PERSON"
    const val CHARACTER_KEY = "CHARACTER"
    const val SUBJECT_PERSON_KEY = "S-P"
    const val SUBJECT_CHARACTER_KEY = "S-C"
    const val CHARACTER_PERSON_KEY = "C-P"
    const val CHARACTER_SUBJECT_KEY = "C-S"
    const val PERSON_SUBJECT_KEY = "P-S"
    const val PERSON_CHARACTER_KEY = "P-C"
    const val RANDOM_CODE = "RANDOM_CODE"
    const val TOKEN = "TOKEN"
    const val TOKEN_TTL = "TOKEN_TTL"
    const val AUTH_LOCK = "AUTH_LOCK"
    val MAPPER = jacksonObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT)

    private val CACHE_TTL = Duration.ofMinutes(10)
    private val serverPort = System.getProperty("PORT")?.toInt() ?: 8080

    private val redisMapper = jacksonObjectMapper()
        .registerModules(Jdk8Module(), JavaTimeModule())
        .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build(),
            ObjectMapper.DefaultTyping.EVERYTHING
        )

    val redisson = Redisson.create(org.redisson.config.Config().also {
        it.codec = JsonJacksonKotlinCodec(redisMapper)
        it.useSingleServer().setAddress("redis://${CONFIG.redis.host}:${CONFIG.redis.port}")
            .setDatabase(CONFIG.redis.database)
    })
    val redissonReactive = redisson.reactive()

    val bgmClient = BgmClient(
        CONFIG.bgm.appId,
        CONFIG.bgm.appSecret,
        CONFIG.bgm.redirectUrl,
        isDebugEnabled = CONFIG.debug
    ).coroutine()
    val tdClient = TDLightCoroutineClient(
        CONFIG.telegram.baseUrl,
        CONFIG.telegram.token,
        CONFIG.telegram.userMode,
        isDebugEnabled = CONFIG.debug,
        updateBaseUrl = CONFIG.telegram.updateBaseUrl
    )
    lateinit var tgBot: LongPollingCoroutineTelegramBot


    val tokens = redissonReactive.getMap<Long, AccessToken>(TOKEN)
    val tokenTTLList = redissonReactive.getScoredSortedSet<Long>(TOKEN_TTL)

    val scheduledThreadPool = Executors.newScheduledThreadPool(1)

    val intRegex = Regex("\\d+")

    private val log: Logger = LogManager.getLogger()

    suspend fun start() {
        startWebServerByKtor()
        startRefreshTask()
        tgBot = LongPollingCoroutineTelegramBot(listOf(UpdateSubscribe()), tdClient)
        tgBot.start()
    }

    private fun startRefreshTask() {
        scheduledThreadPool.scheduleAtFixedRate({
            runBlocking {
                tokenTTLList.valueRange(
                    0.0,
                    true,
                    LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond().toDouble(),
                    true
                ).awaitSingleOrNull()?.let { list ->
                    channelFlow {
                        list.forEach { id ->
                            getToken(id)?.let { send(it) }
                        }
                    }.collect { token ->
                        kotlin.runCatching {
                            bgmClient.refreshToken(token.refreshToken)
                        }.onFailure {
                            if (it is UnauthorizedException) {
                                log.warn("User ${token.userId} refresh token was unauthorized, remove", it)
                                launch { removeToken(token.userId) }
                            } else {
                                log.warn("Refresh ${token.userId} token failed", it)
                            }
                        }.getOrNull()?.let {
                            launch {
                                putToken(it)
                                log.info("Refresh ${token.userId} token success")
                            }
                        }
                    }
                }
            }
        }, 1, TimeUnit.DAYS.toMinutes(1), TimeUnit.MINUTES)
    }

    private fun startWebServerByKtor() {
        embeddedServer(Netty, port = serverPort) {
            val logger = this@embeddedServer.log
            routing {
                route("/callback") {
                    get {
                        logger.info("${call.request.local.host}: ${call.request.uri}")
                        val code = call.parameters["code"]
                        val randomCode = call.parameters["state"]
                        if (randomCode != null && code != null) {
                            val randomCodeBucket = redissonReactive.getBucket<Long>(RANDOM_CODE.appendKey(randomCode))
                            randomCodeBucket.getAwait()?.let { userId: Long ->
                                kotlin.runCatching {
                                    logger.debug("Attempt bind user: $userId")
                                    val token = bgmClient.getToken(code)
                                    logger.debug("Bind success: $userId : ${token.userId}")
                                    val tokenTTLList = redissonReactive.getScoredSortedSet<Long>(TOKEN_TTL)
                                    tokenTTLList.add(
                                        LocalDateTime.now().atZone(ZoneId.systemDefault())
                                            .toEpochSecond() + token.expiresIn.toDouble(), token.userId
                                    ).awaitSingleOrNull()
                                    logger.debug("Add token: $userId : ${token.userId}")
                                    redissonReactive.getMap<Long, AccessToken>(TOKEN).put(userId, token).awaitSingleOrNull()
                                    logger.debug("Redirect to bot")
                                    call.respondRedirect("https://t.me/${tdClient.getMe().username}?start=success")
                                    randomCodeBucket.delete()
                                }.onFailure {
                                    BangumiBot.log.error(it)
                                    call.respondText { "Error: ${it.message}" }
                                }
                            } ?: kotlin.run {
                                call.respondText { "请求超时或异常，请重新发送 /start 命令查看信息" }
                            }
                        } else {
                            call.respondText { "Error: 缺少必要的请求参数" }
                        }
                    }
                }
                route("/") {
                    get {
                        call.respondText { "Bangumi Bot. power by Kurenai" }
                    }
                }
            }
        }.start(false).also {
            log.info("Web server listen to $serverPort")
        }
    }

    suspend fun Message.token(): AccessToken? {
        return tokens[this.from!!.id].awaitSingleOrNull()
    }

    suspend fun InlineQuery.token(): AccessToken? {
        return tokens[this.from.id].awaitSingleOrNull()
    }

    suspend fun getToken(tgId: Long): AccessToken? {
        return tokens[tgId].awaitSingleOrNull()
    }

    suspend fun removeToken(tgId: Long): AccessToken? {
        return tokens.remove(tgId).awaitSingleOrNull().also {
            tokenTTLList.remove(tgId).awaitSingleOrNull()
        }
    }

    suspend fun putToken(token: AccessToken) {
        tokenTTLList.add(token.expiresIn.toDouble(), token.userId).awaitSingleOrNull()
        tokens.put(token.userId, token).awaitSingleOrNull()
    }

    suspend fun send(chatId: String, msg: String): Message {
        return tdClient.send(SendMessage(chatId, msg))
    }

    suspend fun getSubjects(subs: Collection<SubjectSmall>, cacheEnabled: Boolean = true): Flow<Subject> {
        return getSubjects(subs.map { it.id }, cacheEnabled)
    }

    suspend fun getSubjects(ids: List<Int>, cacheEnabled: Boolean = true): Flow<Subject> {
        return channelFlow {
            ids.forEach { id ->
                launch {
                    val bucket = redisson.getBucket<Subject>(SUBJECT_KEY.appendKey(id))
                    kotlin.runCatching {
                        bucket.get() ?: bgmClient.send(GetSubject(id)).also {
                            bucket.set(it)
                            bucket.expireIfNotSet(CACHE_TTL)
                        }
                    }.onFailure {
                        log.error("Get subject $id error: ${it.message}")
                    }.getOrNull()?.let { send(it) }
                }
            }
        }
    }

    fun getPersons(
        persons: Collection<RelatedPerson>,
        token: String? = null,
        cacheEnabled: Boolean = true
    ): Flow<PersonDetail> {
        return getPersons(persons.map { it.id }, token, cacheEnabled)
    }

    fun getPersons(ids: List<Int>, token: String? = null, cacheEnabled: Boolean = true): Flow<PersonDetail> {
        return channelFlow {
            ids.forEach { id ->
                launch {
                    val bucket = redisson.getBucket<PersonDetail>(PERSON_KEY.appendKey(id))
                    val result = kotlin.runCatching {
                        bucket.get() ?: kotlin.run {
                            val detail = bgmClient.send(GetPersonDetail(id).apply { this.token = token })
                            bucket.set(detail)
                            bucket.expireIfNotSet(CACHE_TTL)
                            detail
                        }
                    }.onFailure {
                        log.error("Get persion $id error: ${it.message}")
                    }
                    result.getOrNull()?.let { send(it) }
                }
            }
        }
    }

    fun getCharacters(
        characters: Collection<RelatedCharacter>,
        token: String? = null,
        cacheEnabled: Boolean = true
    ): Flow<CharacterDetail> {
        return getCharacters(characters.map { it.id }, token, cacheEnabled)
    }

    fun getCharacters(ids: List<Int>, token: String? = null, cacheEnabled: Boolean = true): Flow<CharacterDetail> = channelFlow {
        ids.forEach { id ->
            launch {
                val bucket = redisson.getBucket<CharacterDetail>(CHARACTER_KEY.appendKey(id))
                val result = kotlin.runCatching {
                    bucket.get() ?: kotlin.run {
                        val detail = bgmClient.send(GetCharacterDetail(id).apply { this.token = token })
                        bucket.set(detail)
                        bucket.expireIfNotSet(CACHE_TTL)
                        detail
                    }
                }.onFailure {
                    log.error("Get character $id error: ${it.message}")
                }
                result.getOrNull()?.let { send(it) }
            }
        }
    }

    suspend fun getSubjectPersons(id: Int): List<RelatedPerson>? {
        val bucket = redissonReactive.getBucket<List<RelatedPerson>>(SUBJECT_PERSON_KEY.appendKey(id))
        return kotlin.runCatching {
            bucket.get().toFuture().await() ?: kotlin.run {
                bgmClient.send(GetSubjectPersons(id)).also {
                    bucket.set(it).awaitSingleOrNull()
                    bucket.expireIfNotSet(CACHE_TTL).awaitSingleOrNull()
                }
            }
        }.onFailure {
            log.error("Get subject-persons $id error: ${it.message}")
        }.getOrNull()
    }

//    fun getSubjectCharacter(id: Int): Flux<RelatedCharacter> {
//        val bucket = redisson.getBucket<List<RelatedCharacter>>(SUBJECT_CHARACTER_KEY.appendKey(id))
//        return bucket.get()
//            .switchIfEmpty(Mono.defer {
//                bgmClient.send(GetSubjectCharacters(id))
//                    .flatMap { item ->
//                        bucket.set(item)
//                            .then(bucket.expireIfNotSet(CACHE_TTL))
//                            .map { item }
//                    }.onErrorResume { case ->
//                        log.error("Get subject-characters $id error: ${case.message}")
//                        Mono.empty()
//                    }
//            }).flatMapMany {
//                Flux.fromIterable(it)
//            }.sort { a, b ->
//                a.id - b.id
//            }
//    }

//    fun getCharacterPersons(id: Int): Flux<CharacterPerson> {
//        val bucket = redisson.getBucket<List<CharacterPerson>>(CHARACTER_PERSON_KEY.appendKey(id))
//        return bucket.get()
//            .switchIfEmpty(Mono.defer {
//                bgmClient.send(GetCharacterRelatedPersons(id))
//                    .flatMap { item ->
//                        bucket.set(item)
//                            .then(bucket.expireIfNotSet(CACHE_TTL))
//                            .map { item }
//                    }.onErrorResume { case ->
//                        log.error("Get character-persons $id error: ${case.message}")
//                        Mono.empty()
//                    }
//            }).flatMapMany {
//                Flux.fromIterable(it)
//            }.sort { a, b ->
//                a.id - b.id
//            }
//    }

//    fun getCharacterSubjects(id: Int): Flux<RelatedSubjects> {
//        val bucket = redisson.getBucket<List<RelatedSubjects>>(CHARACTER_SUBJECT_KEY.appendKey(id))
//        return bucket.get()
//            .switchIfEmpty(Mono.defer {
//                bgmClient.send(GetCharacterRelatedSubjects(id))
//                    .flatMap { item ->
//                        bucket.set(item)
//                            .then(bucket.expireIfNotSet(CACHE_TTL))
//                            .map { item }
//                    }.onErrorResume { case ->
//                        log.error("Get character-subjects $id error: ${case.message}")
//                        Mono.empty()
//                    }
//            }).flatMapMany {
//                Flux.fromIterable(it)
//            }.sort { a, b ->
//                a.id - b.id
//            }
//    }

//    fun getPersonSubjects(id: Int): Flux<RelatedSubjects> {
//        val bucket = redisson.getBucket<List<RelatedSubjects>>(PERSON_SUBJECT_KEY.appendKey(id))
//        return bucket.get()
//            .switchIfEmpty(Mono.defer {
//                bgmClient.send(GetPersonRelatedSubjects(id))
//                    .flatMap { item ->
//                        bucket.set(item)
//                            .then(bucket.expireIfNotSet(CACHE_TTL))
//                            .map { item }
//                    }.onErrorResume { case ->
//                        log.error("Get person-subjects $id error: ${case.message}")
//                        Mono.empty()
//                    }
//            }).flatMapMany {
//                Flux.fromIterable(it)
//            }.sort { a, b ->
//                a.id - b.id
//            }
//    }

//    fun getPersonCharacters(id: Int): Flux<CharacterPerson> {
//        val bucket = redisson.getBucket<List<CharacterPerson>>(PERSON_CHARACTER_KEY.appendKey(id))
//        return bucket.get()
//            .switchIfEmpty(Mono.defer {
//                bgmClient.send(GetPersonRelatedCharacters(id))
//                    .flatMap { item ->
//                        bucket.set(item)
//                            .then(bucket.expireIfNotSet(CACHE_TTL))
//                            .map { item }
//                    }.onErrorResume { case ->
//                        log.error("Get person-characters $id error: ${case.message}")
//                        Mono.empty()
//                    }
//            }).flatMapMany {
//                Flux.fromIterable(it)
//            }.sort { a, b ->
//                a.id - b.id
//            }
//    }

    fun getSubjectContent(sub: Subject, link: String): Pair<String, List<MessageEntity>> {
        val title = " [${sub.type.category()}]　${sub.name}"
        val infoBox = sub.infobox?.formatInfoBox() ?: ""

        val titleIndex = sub.type.category().length + 4
        val entities = listOf(
            MessageEntity(MessageEntityType.TEXT_LINK, 0, 1).apply { url = sub.images.getLarge() },
            MessageEntity(MessageEntityType.TEXT_LINK, titleIndex, sub.name.length).apply { url = link },
        )

        return listOfNotNull(title, infoBox).joinToString("\n\n") to entities
    }

    fun getSubjectContentSimple(sub: Subject, link: String): Pair<String, List<MessageEntity>> {
        val title = " [${sub.type.category()}]　${sub.name}"

        val titleIndex = sub.type.category().length + 4
        val entities = listOf(
            MessageEntity(MessageEntityType.TEXT_LINK, 0, 1).apply { url = sub.images.getLarge() },
            MessageEntity(MessageEntityType.TEXT_LINK, titleIndex, sub.name.length).apply { url = link },
        )

        return listOfNotNull(title, sub.summary).joinToString("\n\n") to entities
    }

    fun getPersonContent(person: PersonDetail, link: String): Pair<String, List<MessageEntity>> {
        val title = " ${person.name}"
        val infoBox = person.infobox?.formatInfoBox() ?: ""

        val entities = listOf(
            MessageEntity(MessageEntityType.TEXT_LINK, 0, 1).apply { url = person.images.getLarge() },
            MessageEntity(MessageEntityType.TEXT_LINK, 1, person.name.length).apply { url = link },
        )

        return listOfNotNull(title, infoBox).joinToString("\n\n") to entities
    }

    fun getCharacterContent(character: CharacterDetail, link: String): Pair<String, List<MessageEntity>> {
        val title = " ${character.name}"
        val infoBox = character.infobox?.formatInfoBox() ?: ""

        val entities = listOf(
            MessageEntity(MessageEntityType.TEXT_LINK, 0, 1).apply { url = character.images.getLarge() },
            MessageEntity(MessageEntityType.TEXT_LINK, 1, character.name.length).apply { url = link },
        )
        return listOfNotNull(title, infoBox).joinToString("\n\n") to entities
    }

    suspend fun <T> Request<T>.send(): T = kotlin.runCatching {
        bgmClient.send(this)
    }.onFailure {
        log.error("Bgm request error: ${it.message}", it)
    }.getOrThrow()

    suspend fun <T> TDRequest<ResponseWrapper<T>>.send(): T = tdClient.send(this)

    fun getEmptyAnswer(inlineId: String): AnswerInlineQuery = AnswerInlineQuery(inlineId).apply {
        inlineResults = emptyList()
        cacheTime = 0
        switchPmText = "搜索结果为空"
        switchPmParameter = "help"
    }

    private fun Int.category(): String = when (this) {
        1 -> "书籍" //book
        2 -> "动画" //anime
        3 -> "音乐" //music
        4 -> "游戏" //game
        6 -> "三次元" //real
        else -> "？？"        //?
    }

    private fun JsonNode.formatInfoBox(): String {
        return this.joinToString("\n") { node ->
            val valueNode = node.findValue("value")
            val value = if (valueNode.isTextual) valueNode.textValue()
            else valueNode
                .toList()
                .joinToString("、") { it.findValue("v").textValue() }
            "${node.findValue("key").textValue()}: $value"
        }
    }
}