package moe.kurenai.bot

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.kurenairyu.cache.redis.lettuce.jackson.JacksonCodec
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import moe.kurenai.bgm.BgmClient
import moe.kurenai.bgm.model.auth.AccessToken
import moe.kurenai.bgm.model.character.CharacterDetail
import moe.kurenai.bgm.model.character.CharacterPerson
import moe.kurenai.bgm.model.person.PersonDetail
import moe.kurenai.bgm.model.subject.*
import moe.kurenai.bgm.request.Request
import moe.kurenai.bgm.request.charater.GetCharacterDetail
import moe.kurenai.bgm.request.charater.GetCharacterRelatedPersons
import moe.kurenai.bgm.request.charater.GetCharacterRelatedSubjects
import moe.kurenai.bgm.request.person.GetPersonDetail
import moe.kurenai.bgm.request.person.GetPersonRelatedCharacters
import moe.kurenai.bgm.request.person.GetPersonRelatedSubjects
import moe.kurenai.bgm.request.subject.GetSubject
import moe.kurenai.bgm.request.subject.GetSubjectCharacters
import moe.kurenai.bgm.request.subject.GetSubjectPersons
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.config.JsonJacksonKotlinCodec
import moe.kurenai.bot.config.RecordNamingStrategyPatchModule
import moe.kurenai.tdlight.LongPollingTelegramBot
import moe.kurenai.tdlight.client.TDLightClient
import moe.kurenai.tdlight.model.ResponseWrapper
import moe.kurenai.tdlight.model.inline.InlineQuery
import moe.kurenai.tdlight.model.message.Message
import moe.kurenai.tdlight.request.message.AnswerInlineQuery
import moe.kurenai.tdlight.request.message.SendMessage
import moe.kurenai.tdlight.util.MarkdownUtil.fm2md
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.redisson.Redisson
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    private val CACHE_TTL = Duration.ofMinutes(10)

    private val redisMapper = jacksonObjectMapper()
        .registerModules(Jdk8Module(), JavaTimeModule(), RecordNamingStrategyPatchModule())
        .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build(), ObjectMapper.DefaultTyping.EVERYTHING)

    val redisson = Redisson.create(org.redisson.config.Config().also {
        it.codec = JsonJacksonKotlinCodec(redisMapper)
        it.useSingleServer().setAddress("redis://${CONFIG.redis.host}:${CONFIG.redis.port}").setDatabase(CONFIG.redis.database)
    }).reactive()
    val lettuce = RedisClient
        .create(RedisURI.builder().withHost(CONFIG.redis.host).withPort(CONFIG.redis.port).withDatabase(CONFIG.redis.database).build())
        .connect(JacksonCodec<Any>(redisMapper))
        .reactive()
    val bgmClient = BgmClient(CONFIG.bgm.appId, CONFIG.bgm.appSecret, CONFIG.bgm.redirectUrl, isDebugEnabled = CONFIG.debug).reactive()
    val tdClient = TDLightClient(CONFIG.telegram.baseUrl, CONFIG.telegram.token, CONFIG.telegram.userMode, isDebugEnabled = CONFIG.debug, updateBaseUrl = CONFIG.telegram.updateBaseUrl)
    val tgBot = LongPollingTelegramBot(listOf(UpdateSubscribe()), tdClient)


    val tokens = redisson.getMap<String, AccessToken>(TOKEN)
    val tokenTTLList = redisson.getScoredSortedSet<String>(TOKEN_TTL)

    val scheduledThreadPool = Executors.newScheduledThreadPool(1)

    val intRegex = Regex("\\d+")

    private val log: Logger = LogManager.getLogger()

    fun start() {
//        starttWebServer()
        startRefreshTask()
    }

    private fun startRefreshTask() {
        scheduledThreadPool.scheduleAtFixedRate({
            tokenTTLList.valueRange(0.0, true, LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond().toDouble(), true)
                .flatMapMany { Flux.fromIterable(it) }
                .flatMap(tokens::get)
                .flatMap { token ->
                    bgmClient.refreshToken(token.refreshToken)
                }.flatMap { new ->
                    tokenTTLList.add(new.expiresIn.toDouble(), new.userId.toString())
                        .zipWith(Mono.defer {
                            tokens.put(new.userId.toString(), new)
                        })
                }.subscribe()
        }, 1, TimeUnit.DAYS.toMinutes(1), TimeUnit.MINUTES)
    }

    private fun starttWebServer() {
        val vertx = Vertx.vertx()
        val router = Router.router(vertx)
        router.get("/callback").handler { ctx ->
            log.info("${ctx.request().localAddress()}: ${ctx.request().uri()}")
            ctx.queryParam("code")?.firstOrNull()?.let { code ->
                try {
                    val randomCode = ctx.queryParam("state")?.first()

                    if (randomCode != null) {
                        val lock = redisson.getLock(AUTH_LOCK.appendKey(randomCode))
                        val userId = redisson.getBucket<Long>(RANDOM_CODE.appendKey(randomCode))
                        userId.get().flatMap { id ->
                            lock.tryLock(30, TimeUnit.SECONDS)
                                .flatMap { locked ->
                                    if (locked) {
                                        bgmClient.getToken(code)
                                            .flatMap { token ->
                                                tokenTTLList.add(token.expiresIn.toDouble(), token.userId.toString())
                                                    .zipWith(tokens.put(token.userId.toString(), token))
                                            }.flatMap {
                                                ctx.redirect("https://t.me/${tdClient.me.username}?start=success")
                                                userId.delete()
                                            }
                                    } else {
                                        ctx.response()
                                            .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                                            .send(Buffer.buffer("<p>处理中，请稍后</p>"))
                                        Mono.just(true)
                                    }
                                }
                        }.switchIfEmpty(Mono.defer {
                            ctx.response()
                                .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                                .send("<p>请重新发送 /start 命令查看信息</p>")
                            Mono.empty()
                        }).doOnError { ex ->
                            ctx.response()
                                .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                                .send("<p>Error: \n${ex.message}</p>")
                        }.subscribe()
                    } else {
                        ctx.response()
                            .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                            .send("<p>Error: 缺少必要的请求参数</p>")
                    }
                } catch (e: Exception) {
                    log.error(e)
                    ctx.response()
                        .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                        .send("<p>Error: \n${e.message}</p>")
                }
            }
        }
        router.get("/").handler { ctx ->
            log.debug("${ctx.request().localAddress()}: ${ctx.request().path()}")
            ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                .send("</p>Bangumi Bot. power by kurenai</p>")
        }
        val port = System.getProperty("PORT")?.toInt() ?: 8080
        vertx.createHttpServer(HttpServerOptions().apply {
            host = "0.0.0.0"
        }).requestHandler(router::handle).listen(port).onSuccess {
            log.info("Web server listen to $port")
        }
    }

    fun Message.token(): Mono<AccessToken> {
        return tokens[this.from!!.id.toString()]
    }

    fun InlineQuery.token(): Mono<AccessToken> {
        return tokens[this.from.id.toString()]
    }

    fun getToken(tgId: Long): Mono<AccessToken> {
        return tokens[tgId.toString()]
    }

    fun AccessToken.check() {
        //TODO check expire
        this.expiresIn
    }

    fun send(chatId: String, msg: String): Mono<Message> {
        return Mono.fromCompletionStage(tdClient.send(SendMessage(chatId, msg)))
    }

    fun getSubjects(subs: Collection<SubjectSmall>, cacheEnabled: Boolean = true): Flux<Subject> {
        return getSubjects(subs.map { it.id }, cacheEnabled)
    }

    fun getSubjects(ids: List<Int>, cacheEnabled: Boolean = true): Flux<Subject> {
        return Flux.fromIterable(ids)
            .log()
            .flatMap { id ->
                val bucket = redisson.getBucket<Subject>(SUBJECT_KEY.appendKey(id))
                bucket.get()
                    .switchIfEmpty(Mono.defer {
                        bgmClient.send(GetSubject(id))
                            .flatMap { sub ->
                                bucket.set(sub)
                                    .then(bucket.expireIfNotSet(CACHE_TTL))
                                    .map { sub }
                            }
                    }).doOnError { case ->
                        log.error("Get subject $id error: ${case.message}")
                    }
            }.sort { a, b ->
                a.id - b.id
            }

    }

    fun getPersons(persons: Collection<RelatedPerson>, token: String? = null, cacheEnabled: Boolean = true): Flux<PersonDetail> {
        return getPersons(persons.map { it.id }, token, cacheEnabled)
    }

    fun getPersons(ids: List<Int>, token: String? = null, cacheEnabled: Boolean = true): Flux<PersonDetail> {
        return Flux.fromIterable(ids)
            .flatMap { id ->
                val bucket = redisson.getBucket<PersonDetail>(PERSON_KEY.appendKey(id))
                bucket.get()
                    .switchIfEmpty(Mono.defer {
                        bgmClient.send(GetPersonDetail(id).apply { this.token = token })
                            .flatMap { detail ->
                                bucket.set(detail)
                                    .then(bucket.expireIfNotSet(CACHE_TTL))
                                    .map { detail }
                            }
                    })
                    .onErrorResume { case ->
                        log.error("Get persion $id error: ${case.message}")
                        Mono.empty()
                    }
            }.sort { a, b ->
                a.id - b.id
            }
    }

    fun getCharacters(characters: Collection<RelatedCharacter>, token: String? = null, cacheEnabled: Boolean = true): Flux<CharacterDetail> {
        return getCharacters(characters.map { it.id }, token, cacheEnabled)
    }

    fun getCharacters(ids: List<Int>, token: String? = null, cacheEnabled: Boolean = true): Flux<CharacterDetail> {
        return Flux.fromIterable(ids)
            .flatMap { id ->
                val bucket = redisson.getBucket<CharacterDetail>(CHARACTER_KEY.appendKey(id))
                bucket.get().switchIfEmpty(Mono.defer {
                    bgmClient.send(GetCharacterDetail(id).apply { this.token = token })
                        .flatMap { detail ->
                            bucket.set(detail)
                                .then(bucket.expireIfNotSet(CACHE_TTL))
                                .map { detail }
                        }
                }).onErrorResume { case ->
                    log.error("Get character $id error: ${case.message}")
                    Mono.empty()
                }
            }.sort { a, b ->
                a.id - b.id
            }
    }

    fun getSubjectPersons(id: Int): Flux<RelatedPerson> {
        val bucket = redisson.getBucket<List<RelatedPerson>>(SUBJECT_PERSON_KEY.appendKey(id))
        return bucket.get()
            .switchIfEmpty(Mono.defer {
                bgmClient.send(GetSubjectPersons(id))
                    .flatMap { item ->
                        bucket.set(item)
                            .then(bucket.expireIfNotSet(CACHE_TTL))
                            .map { item }
                    }.onErrorResume { case ->
                        log.error("Get subject-persons $id error: ${case.message}")
                        Mono.empty()
                    }
            }).flatMapMany {
                Flux.fromIterable(it)
            }.sort { a, b ->
                a.id - b.id
            }
    }

    fun getSubjectCharacter(id: Int): Flux<RelatedCharacter> {
        val bucket = redisson.getBucket<List<RelatedCharacter>>(SUBJECT_CHARACTER_KEY.appendKey(id))
        return bucket.get()
            .switchIfEmpty(Mono.defer {
                bgmClient.send(GetSubjectCharacters(id))
                    .flatMap { item ->
                        bucket.set(item)
                            .then(bucket.expireIfNotSet(CACHE_TTL))
                            .map { item }
                    }.onErrorResume { case ->
                        log.error("Get subject-characters $id error: ${case.message}")
                        Mono.empty()
                    }
            }).flatMapMany {
                Flux.fromIterable(it)
            }.sort { a, b ->
                a.id - b.id
            }
    }

    fun getCharacterPersons(id: Int): Flux<CharacterPerson> {
        val bucket = redisson.getBucket<List<CharacterPerson>>(CHARACTER_PERSON_KEY.appendKey(id))
        return bucket.get()
            .switchIfEmpty(Mono.defer {
                bgmClient.send(GetCharacterRelatedPersons(id))
                    .flatMap { item ->
                        bucket.set(item)
                            .then(bucket.expireIfNotSet(CACHE_TTL))
                            .map { item }
                    }.onErrorResume { case ->
                        log.error("Get character-persons $id error: ${case.message}")
                        Mono.empty()
                    }
            }).flatMapMany {
                Flux.fromIterable(it)
            }.sort { a, b ->
                a.id - b.id
            }
    }

    fun getCharacterSubjects(id: Int): Flux<RelatedSubjects> {
        val bucket = redisson.getBucket<List<RelatedSubjects>>(CHARACTER_SUBJECT_KEY.appendKey(id))
        return bucket.get()
            .switchIfEmpty(Mono.defer {
                bgmClient.send(GetCharacterRelatedSubjects(id))
                    .flatMap { item ->
                        bucket.set(item)
                            .then(bucket.expireIfNotSet(CACHE_TTL))
                            .map { item }
                    }.onErrorResume { case ->
                        log.error("Get character-subjects $id error: ${case.message}")
                        Mono.empty()
                    }
            }).flatMapMany {
                Flux.fromIterable(it)
            }.sort { a, b ->
                a.id - b.id
            }
    }

    fun getPersonSubjects(id: Int): Flux<RelatedSubjects> {
        val bucket = redisson.getBucket<List<RelatedSubjects>>(PERSON_SUBJECT_KEY.appendKey(id))
        return bucket.get()
            .switchIfEmpty(Mono.defer {
                bgmClient.send(GetPersonRelatedSubjects(id))
                    .flatMap { item ->
                        bucket.set(item)
                            .then(bucket.expireIfNotSet(CACHE_TTL))
                            .map { item }
                    }.onErrorResume { case ->
                        log.error("Get person-subjects $id error: ${case.message}")
                        Mono.empty()
                    }
            }).flatMapMany {
                Flux.fromIterable(it)
            }.sort { a, b ->
                a.id - b.id
            }
    }

    fun getPersonCharacters(id: Int): Flux<CharacterPerson> {
        val bucket = redisson.getBucket<List<CharacterPerson>>(PERSON_CHARACTER_KEY.appendKey(id))
        return bucket.get()
            .switchIfEmpty(Mono.defer {
                bgmClient.send(GetPersonRelatedCharacters(id))
                    .flatMap { item ->
                        bucket.set(item)
                            .then(bucket.expireIfNotSet(CACHE_TTL))
                            .map { item }
                    }.onErrorResume { case ->
                        log.error("Get person-characters $id error: ${case.message}")
                        Mono.empty()
                    }
            }).flatMapMany {
                Flux.fromIterable(it)
            }.sort { a, b ->
                a.id - b.id
            }
    }

    fun getSubjectContent(sub: Subject): String {
        val title = "\\[${sub.type.category().fm2md()}\\][${sub.name.fm2md()}](${sub.images.getLarge().fm2md()})"
        val infoBox = sub.infobox?.formatInfoBox()?.fm2md()

        return listOfNotNull(title, infoBox).joinToString("\n\n")
    }

    fun getPersonContent(person: PersonDetail): String {
        val title = "[${person.name.fm2md()}](${person.images.getLarge().fm2md()})"
        val infoBox = person.infobox?.formatInfoBox()?.fm2md()

        return listOfNotNull(title, infoBox).joinToString("\n\n")
    }

    fun getCharacterContent(character: CharacterDetail): String {
        val title = "[${character.name.fm2md()}](${character.images.getLarge().fm2md()})"
        val infoBox = character.infobox?.formatInfoBox()?.fm2md()

        return listOfNotNull(title, infoBox).joinToString("\n\n")
    }

    fun <T> Request<T>.send(): Mono<T> {
        return bgmClient.send(this)
    }

    fun <T> moe.kurenai.tdlight.request.Request<ResponseWrapper<T>>.send(): Mono<T> {
        return Mono.fromFuture(tdClient.send(this))
    }

    fun getEmptyAnswer(inlineId: String): AnswerInlineQuery {
        return AnswerInlineQuery(inlineId).apply {
            inlineResults = emptyList()
            cacheTime = 0
            switchPmText = "搜索结果为空"
            switchPmParameter = "help"
        }
    }

    private fun Int.category(): String {
        return when (this) {
            1 -> "书籍" //book
            2 -> "动画" //anime
            3 -> "音乐" //music
            4 -> "游戏" //game
            6 -> "三次元" //real
            else -> "？？"        //?
        }
    }

    private fun JsonNode.formatInfoBox(): String {
        return this.joinToString("\n") { node ->
            val valueNode = node.findValue("value")
            val value = if (valueNode.isTextual) valueNode.textValue()
            else valueNode
                .toList()
                .joinToString("\n\t    ") { it.findValue("v").textValue() }
            "${node.findValue("key").textValue()}: $value"
        }
    }
}