package moe.kurenai.bot

import com.fasterxml.jackson.databind.JsonNode
import moe.kurenai.bgm.model.person.PersonDetail
import moe.kurenai.bgm.model.subject.getGrid
import moe.kurenai.bot.command.CommandDispatcher
import moe.kurenai.tdlight.AbstractUpdateSubscriber
import moe.kurenai.tdlight.model.MessageEntityType
import moe.kurenai.tdlight.model.inline.InlineQueryResultArticle
import moe.kurenai.tdlight.model.inline.InputTextMessageContent
import moe.kurenai.tdlight.model.keyboard.InlineKeyboardButton
import moe.kurenai.tdlight.model.keyboard.InlineKeyboardMarkup
import moe.kurenai.tdlight.model.message.MessageEntity
import moe.kurenai.tdlight.model.message.Update
import moe.kurenai.tdlight.request.message.AnswerInlineQuery
import org.apache.logging.log4j.LogManager
import kotlin.math.min

class UpdateSubscribe : AbstractUpdateSubscriber() {
    companion object {
        private val log = LogManager.getLogger()
        const val DEFAULT_IMAGE_URL = "https://bgm.tv/img/no_icon_subject.png"
        const val DEFAULT_SIZE = 30
    }

    override fun onComplete0() {
    }

    override fun onError0(e: Throwable) {
        log.error("Subscribe error", e)
    }

    override fun onNext0(update: Update) {
//        log.info("\n>>>>>>>${convertToString(update)}")

        try {
//            handleInlineQuery(update)
            CommandDispatcher.handle(update)
        } catch (e: Exception) {
            log.error(e.message, e)
        }
    }

    override fun onSubscribe0() {
    }

//    private fun handleInlineQuery(update: Update) {
//        val inlineQuery = update.inlineQuery ?: return
//        if (inlineQuery.query.isBlank()) return
//        val query = inlineQuery.query.trim()
//        val offset = inlineQuery.offset.takeIf { it.isNotBlank() }?.toInt() ?: 1
//        if (offset <= 0) return
//        try {
//            tdClient.sendSync(getAnswer(update, inlineQuery, query, offset) ?: emptyAnswer(inlineQuery.id))
//        } catch (e: Exception) {
//            log.error(e.message, e)
//            tdClient.sendSync(emptyAnswer(inlineQuery.id))
//        }
//    }

//    private fun getAnswer(update: Update, inlineQuery: InlineQuery, rawQuery: String, offset: Int): AnswerInlineQuery? {
//
//        val token = update.inlineQuery?.token()
//        val split = rawQuery.trim().split(" ", limit = 3).map { it.trim() }
//        if (split.size > 1 && split[1].matches(Regex("\\d+"))) {
//            val searchType = split[0]
//            val itemId = split[1].toInt()
//            val keyword = if (split.size > 2) split[2].takeIf { it.isNotBlank() }?.trim() else null
//            if (searchType.equals("/P", true)) {
//                return person(itemId, inlineQuery.id, token)
//            } else if (searchType.equals("/S", true)) {
//                return subject(itemId, inlineQuery.id)
//            } else if (searchType.startsWith("/subject-persons")) {
//                return subjectPersons(itemId, inlineQuery.id, offset, keyword)
//            } else if (searchType.startsWith("/subject-characters")) {
//                return subjectCharacters(itemId, inlineQuery.id, offset, keyword)
//            } else if (searchType.startsWith("/character-persons")) {
//                return characterPersons(itemId, inlineQuery.id, offset, keyword, token)
//            } else if (searchType.startsWith("/character-subjects")) {
//                return characterSubjects(itemId, inlineQuery.id, offset, keyword, token)
//            } else if (searchType.startsWith("/person-subjects")) {
//                return personSubjects(itemId, inlineQuery.id, offset, keyword, token)
//            } else if (searchType.startsWith("/person-characters")) {
//                return personCharacters(itemId, inlineQuery.id, offset, keyword, token)
//            }
//        }
//
//        val searchType = when (rawQuery.take(2).uppercase()) {
//            "/B" -> 1
//            "/A" -> 2
//            "/M" -> 3
//            "/G" -> 4
//            "/R" -> 6
//            else -> 0
//        }
//        val query = if (searchType > 0) rawQuery.substring(2)
//        else if (rawQuery.startsWith("/")) return null
//        else rawQuery
//
//        if (offset <= 0) return null
//        val searchResult = lettuce.get<String, SearchResult>("SEARCH", "${inlineQuery.query.hashCode()}-$offset") ?: bgmClient.sendSync(SearchSubject(query).apply {
//            if (searchType > 0) type = searchType
//            responseGroup = ResponseGroup.SMALL
//            maxResults = DEFAULT_SIZE
//            start = offset
//        })
//        lettuce.putIfAbsent("SEARCH", "${inlineQuery.query.hashCode()}-$offset", searchResult, 1, TimeUnit.HOURS)
//        val subs = try {
//            searchResult.list.map { it.id }.let { getSubjects(it).join() }
//        } catch (e: NotFoundException) {
//            tdClient.send(AnswerInlineQuery(inlineQuery.id))
//            return null
//        }
//        return AnswerInlineQuery(inlineQuery.id).apply {
//            val nOffset = offset + searchResult.list.size
//            nextOffset = if (nOffset < searchResult.results) nOffset.toString() else "0"
//            cacheTime = 300
//            inlineResults = subject2InlineResult(subs)
//        }
//    }
//
//    private fun subject(id: Int, queryId: String): AnswerInlineQuery {
//        log.info(">>>>>>>>>>>>Search subject $id from $queryId")
//        return getSubjects(listOf(id)).thenApply { subs ->
//            buildAnswer(subs.size, queryId).apply {
//                inlineResults = subject2InlineResult(subs)
//            }
//        }.join()
//    }
//
//    private fun person(id: Int, queryId: String, token: String?): AnswerInlineQuery {
//        log.info(">>>>>>>>>>>>Search person $id from $queryId")
//        return getPersons(listOf(id), token).thenApply { details ->
//            val results = person2InlineResult(details)
//            buildAnswer(results.size, queryId).apply {
//                inlineResults = results
//            }
//        }.join()
//    }
//
//    private fun subjectPersons(id: Int, queryId: String, offset: Int, keyword: String?): AnswerInlineQuery {
//        log.info(">>>>>>>>>>>>Search subject $id person from $queryId")
//        val list = getSubjectPersons(id).let { l ->
//            keyword?.let {
//                l.filter {
//                    it.name.contains(keyword) || it.relation.contains(keyword) || it.type.personType().contains(keyword)
//                            || it.career.joinToString(" ") { it.career() }.contains(keyword)
//                }
//            } ?: l
//        }
//
//        val map = HashMap<Int, Pair<RelatedPerson, ArrayList<String>>>()
//        list.forEach { p ->
//            map.putIfAbsent(p.id, p to arrayListOf(p.relation))?.second?.add(p.relation)
//        }
//        val persons = map.values.toList()
//        val results = relatePerson2InlineResult(persons.sortedBy { "${it.first.type}${it.first.id}}" }
//            .handleOffset(offset))
//        return buildAnswer(list.size, queryId).apply {
//            inlineResults = results
//            if (results.size == DEFAULT_SIZE) nextOffset = "${offset + 1}"
//        }
//    }
//
//    private fun subjectCharacters(id: Int, queryId: String, offset: Int, keyword: String?): AnswerInlineQuery {
//        log.info(">>>>>>>>>>>>Search subject $id characters from $queryId")
//        val sub = getSubjects(listOf(id)).join()[0]
//        val count: Int
//        val list = getSubjectCharacter(id).let { l ->
//            keyword?.let {
//                l.filter {
//                    it.name.contains(keyword)
//                            || it.relation.contains(keyword)
//                            || it.career?.contains(keyword) == true
//                }
//            }
//                ?: l
//        }.sortedBy { it.id }
//            .also { count = it.size }
//            .handleOffset(offset)
//        val persons = ConcurrentHashMap<Int, List<CharacterPerson>>()
//        return CompletableFuture
//            .allOf(*list.map { CompletableFuture.supplyAsync { persons[it.id] = getCharacterPersons(it.id) }.exceptionally { log.error(it.message, it) } }.toTypedArray())
//            .thenCombineAsync(getCharacters(list)) { _, characters ->
//                val results = characters.map { detail ->
//                    val relate = list.find { it.id == detail.id }
//                    val info = detail.infobox.formatInfoBox()
//                    InlineQueryResultArticle("C${detail.id}", detail.name).apply {
//                        description = listOfNotNull(
//                            relate?.relation,
//                            listOfNotNull(sub.type.artistType(), persons[detail.id]?.firstOrNull { it.type == 1 && it.subjectId == id }?.name).joinToString(" "),
//                        ).joinToString(" / ")
//                        url = "https://bgm.tv/character/${detail.id}"
//                        hideUrl = true
//                        thumbUrl = detail.images.getGrid()
//                        inputMessageContent = InputTextMessageContent(
//                            "${detail.name}\n\n" +
//                                    "${description}\n" +
//                                    info +
//                                    detail.summary.subSummary()
//                        ).apply {
//                            entities = listOf(MessageEntity(MessageEntityType.TEXT_LINK, 0, detail.name.length).apply {
//                                url = detail.images.getLarge()
//                            })
//                        }
//                        replyMarkup = InlineKeyboardMarkup(
//                            listOf(
//                                listOf(
//                                    InlineKeyboardButton("详情").apply { url = "https://bgm.tv/character/${detail.id}" },
//                                    InlineKeyboardButton("条目").apply { switchInlineQueryCurrentChat = "/character-subjects ${detail.id}" },
//                                    InlineKeyboardButton("人物").apply { switchInlineQueryCurrentChat = "/character-persons ${detail.id}" },
//                                )
//                            )
//                        )
//                    }
//                }
//                buildAnswer(count, queryId).apply {
//                    inlineResults = results
//                    if (results.size == DEFAULT_SIZE) nextOffset = "${offset + 1}"
//                }
//            }.join()
//    }
//
//    private fun characterSubjects(id: Int, queryId: String, offset: Int, keyword: String?, token: String?): AnswerInlineQuery {
//        log.info(">>>>>>>>>>>>Search character $id subjects from $queryId")
//        val relates = getCharacterSubjects(id).handleOffset(offset)
//        return getSubjects(relates.distinctBy { it.id }.map { it.id }).thenApply { subjects ->
//            val subjectMap = subjects.associateBy { it.id }
//            relates.map { relate ->
//                val sub = subjectMap[relate.id] ?: return@map null
//                val tag = sub.type.category()
//                val title = sub.nameCn.takeIf(String::isNotBlank) ?: sub.name
//                val subUrl = "https://bgm.tv/subject/${sub.id}"
//                val info = listOfNotNull(sub.date ?: "????-??-??", tag, sub.platform, "${sub.rating?.score ?: "???"} ⭐").filter { it.isNotBlank() }.joinToString(" / ")
//                val ep = "共 ${sub.totalEpisodes} 集"
//                val summary = sub.summary.take(200) + if (sub.summary.length > 150) "..." else ""
//                InlineQueryResultArticle("S${relate.id}-${relate.staff}", title).apply {
//                    description = "${relate.staff}\n"
//                    url = subUrl
//                    hideUrl = true
//                    thumbUrl = sub.images.getGrid()
//                    inputMessageContent = InputTextMessageContent("$title\n${sub.name}\n\n$info\n$ep\n\n$summary").apply {
//                        entities = listOf(MessageEntity(MessageEntityType.TEXT_LINK, 0, title.length).apply {
//                            url = sub.images.getLarge()
//                        })
//                    }
//                    replyMarkup = InlineKeyboardMarkup(
//                        listOf(
//                            listOf(
//                                InlineKeyboardButton("详情").apply { url = subUrl },
//                                InlineKeyboardButton("角色").apply { switchInlineQueryCurrentChat = "/subject-characters ${sub.id}" },
//                                InlineKeyboardButton("人物").apply { switchInlineQueryCurrentChat = "/subject-persons ${sub.id}" },
//                            )
//                        )
//                    )
//                }
//            }.filterNotNull()
//        }.thenApply { results ->
//            AnswerInlineQuery(queryId).apply {
//                inlineResults = results
//                if (results.size == DEFAULT_SIZE) nextOffset = "${offset + 1}"
//            }
//        }.join()
//    }
//
//    private fun characterPersons(id: Int, queryId: String, offset: Int, keyword: String?, token: String?): AnswerInlineQuery {
//        log.info(">>>>>>>>>>>>Search character $id persons from $queryId")
//        val relates = getCharacterPersons(id).handleOffset(offset)
//        return getPersons(relates.distinctBy { it.id }.map { it.id }, token).thenApply { persons ->
//            val personMap = persons.associateBy { it.id }
//            relates.map { relate ->
//                val person = personMap[relate.id] ?: return@map null
//                val info = person.infobox.formatInfoBox()
//                InlineQueryResultArticle("C${relate.id}-S${relate.subjectId}", relate.name).apply {
//                    description = "${relate.subjectNameCn}/${relate.subjectName}\n${person.birthYear ?: "????"} - ${person.birthMon ?: "??"} - ${person.birthDay ?: "??"}"
//                    thumbUrl = person.images.getGrid()
//                    url = "https://bgm.tv/character/${person.id}"
//                    hideUrl = true
//                    inputMessageContent = InputTextMessageContent(
//                        "${person.name} / ${person.type.personType()}\n\n" +
//                                "${person.birthYear ?: "????"} - ${person.birthMon ?: "??"} - ${person.birthDay ?: "??"}\n" +
//                                "$info${person.summary.subSummary()}"
//                    ).apply {
//                        entities = listOf(MessageEntity(MessageEntityType.TEXT_LINK, 0, person.name.length).apply {
//                            url = person.images.getLarge()
//                        })
//                    }
//                    replyMarkup = InlineKeyboardMarkup(
//                        listOf(
//                            listOf(
//                                InlineKeyboardButton("详情").apply { url = "https://bgm.tv/character/${person.id}" },
//                                InlineKeyboardButton("条目").apply { switchInlineQueryCurrentChat = "/person-subjects ${person.id}" },
//                                InlineKeyboardButton("角色").apply { switchInlineQueryCurrentChat = "/person-characters ${person.id}" },
//                            )
//                        )
//                    )
//                }
//            }.filterNotNull()
//        }.thenApply { results ->
//            AnswerInlineQuery(queryId).apply {
//                inlineResults = results
//                if (results.size == DEFAULT_SIZE) nextOffset = "${offset + 1}"
//            }
//        }.join()
//    }
//
//    private fun personSubjects(id: Int, queryId: String, offset: Int, keyword: String?, token: String?): AnswerInlineQuery {
//        log.info(">>>>>>>>>>>>Search person $id subjects from $queryId")
//        val relates = getPersonSubjects(id)
//        return getSubjects(relates.distinctBy { it.id }.map { it.id }).thenApply { subjects ->
//            val subjectMap = subjects
//                .filter {
//                    if (keyword != null && keyword.isNotBlank()) {
//                        it.type.category().contains(keyword) ||
//                                it.name.contains(keyword) ||
//                                it.nameCn.contains(keyword) ||
//                                it.platform?.contains(keyword) == true
//                    } else true
//                }.associateBy { it.id }
//            relates.handleOffset(offset).map { relate ->
//                val sub = subjectMap[relate.id] ?: return@map null
//                val tag = sub.type.category()
//                val title = "[${sub.type.category()}] ${sub.nameCn.takeIf(String::isNotBlank) ?: sub.name}"
//                val subUrl = "https://bgm.tv/subject/${sub.id}"
//                val info = listOfNotNull(tag, sub.platform, "${sub.rating?.score ?: "???"} ⭐").filter { it.isNotBlank() }.joinToString(" / ") +
//                        sub.infobox.formatInfoBox()
//                val ep = "共 ${sub.totalEpisodes} 集"
//                InlineQueryResultArticle("S${relate.id}-${relate.staff}", title).apply {
//                    description = "${relate.staff}\n"
//                    url = subUrl
//                    hideUrl = true
//                    thumbUrl = sub.images.getGrid()
//                    inputMessageContent = InputTextMessageContent(
//                        sub.nameCn +
//                            "\n${sub.name}" +
//                            "\n\n$ep" +
//                            "\n\n$info" +
//                            "\n\n${sub.summary.subSummary()}").apply {
//                        entities = listOf(MessageEntity(MessageEntityType.TEXT_LINK, 0, title.length).apply {
//                            url = sub.images.getLarge()
//                        })
//                    }
//                    replyMarkup = InlineKeyboardMarkup(
//                        listOf(
//                            listOf(
//                                InlineKeyboardButton("详情").apply { url = subUrl },
//                                InlineKeyboardButton("角色").apply { switchInlineQueryCurrentChat = "/subject-characters ${sub.id}" },
//                                InlineKeyboardButton("人物").apply { switchInlineQueryCurrentChat = "/subject-persons ${sub.id}" },
//                            )
//                        )
//                    )
//                }
//            }.filterNotNull()
//        }.thenApply { results ->
//            AnswerInlineQuery(queryId).apply {
//                inlineResults = results
//                if (results.size == DEFAULT_SIZE) nextOffset = "${offset + 1}"
//            }
//        }.join()
//    }
//
//    private fun personCharacters(id: Int, queryId: String, offset: Int, keyword: String?, token: String?): AnswerInlineQuery {
//        log.info(">>>>>>>>>>>>Search person $id characters from $queryId")
//        val relates = getPersonCharacters(id).sortedBy { it.id }
//        return getCharacters(relates.distinctBy { it.id }.map { it.id }, token).thenApply { characters ->
//            val characterMap = characters.associateBy { it.id }
//            relates.handleOffset(offset).map { relate ->
//                val character = characterMap[relate.id] ?: return@map null
//                val title = character.name
//                val url = "https://bgm.tv/character/${character.id}"
//                InlineQueryResultArticle("C${relate.id}-S${relate.subjectId}", title).apply {
//                    description = listOfNotNull(
//                        relate.subjectName,
//                        relate.subjectNameCn
//                    ).joinToString(" / ")
//                    this.url = url
//                    hideUrl = true
//                    thumbUrl = character.images.getGrid()
//                    inputMessageContent = InputTextMessageContent(
//                        title +
//                                "\n\n${character.gender.gender()}" +
//                                "\n\n${character.infobox.formatInfoBox()}" +
//                                character.summary.subSummary()
//                    ).apply {
//                        entities = listOf(MessageEntity(MessageEntityType.TEXT_LINK, 0, title.length).apply {
//                            this.url = character.images.getLarge()
//                        })
//                    }
//                    replyMarkup = InlineKeyboardMarkup(
//                        listOf(
//                            listOf(
//                                InlineKeyboardButton("详情").apply { this.url = url },
//                                InlineKeyboardButton("角色").apply { switchInlineQueryCurrentChat = "/subject-characters ${character.id}" },
//                                InlineKeyboardButton("人物").apply { switchInlineQueryCurrentChat = "/subject-persons ${character.id}" },
//                            )
//                        )
//                    )
//                }
//            }.filterNotNull()
//        }.thenApply { results ->
//            AnswerInlineQuery(queryId).apply {
//                inlineResults = results
//                if (results.size == DEFAULT_SIZE) nextOffset = "${offset + 1}"
//            }
//        }.join()
//    }
//
//    private fun subject2InlineResult(subjects: List<Subject>): List<InlineQueryResultArticle> {
//        return subjects.map { sub ->
//            val tag = sub.type.category()
//            val title = sub.nameCn.takeIf(String::isNotBlank) ?: sub.name
//            val largeImgUrl = sub.images.getLarge()
//            val mediumImgUrl = sub.images.getGrid()
//            val subUrl = "https://bgm.tv/subject/${sub.id}"
//            val ep = "共 ${sub.totalEpisodes} 集"
//            val info = listOfNotNull(ep, tag, sub.platform, "${sub.rating?.score ?: "???"} ⭐").filter { it.isNotBlank() }.joinToString(" / ") +
//                    "\n\n${sub.infobox.formatInfoBox()}"
//            val summary = sub.summary.take(200) + if (sub.summary.length > 150) "..." else ""
//            InlineQueryResultArticle("S${sub.id}", "[$tag] ${sub.nameCn.takeIf(String::isNotBlank) ?: sub.name}").apply {
//                description = "${sub.date ?: "????-??-??"} / ${sub.platform}\n" +
//                        "${sub.rating?.score} ⭐"
//                url = subUrl
//                hideUrl = true
//                thumbUrl = mediumImgUrl
//                inputMessageContent = InputTextMessageContent(
//                    "$title\n" +
//                            sub.name +
//                            "\n\n$info" +
//                            "\n\n$summary"
//                ).apply {
//                    entities = listOf(MessageEntity(MessageEntityType.TEXT_LINK, 0, title.length).apply {
//                        url = largeImgUrl
//                    })
//                }
//                replyMarkup = InlineKeyboardMarkup(
//                    listOf(
//                        listOf(
//                            InlineKeyboardButton("详情").apply { url = subUrl },
//                            InlineKeyboardButton("角色").apply { switchInlineQueryCurrentChat = "/subject-characters ${sub.id}" },
//                            InlineKeyboardButton("人物").apply { switchInlineQueryCurrentChat = "/subject-persons ${sub.id}" },
//                        )
//                    )
//                )
//            }
//        }
//    }
//
//    private fun relatePerson2InlineResult(persons: List<Pair<RelatedPerson, ArrayList<String>>>): List<InlineQueryResultArticle> {
//        return getPersons(persons.map { it.first.id }).thenApply { details ->
//            val personMap = details.associateBy { it.id }
//            persons.map { p ->
//                val detail = personMap[p.first.id]
//                val person = p.first
//                val related = p.second.joinToString(" ")
//                val career = person.career.joinToString(" ") { it.career() }
//                val info = detail?.infobox.formatInfoBox()
//                InlineQueryResultArticle("P${person.id}", person.name).apply {
//                    description = related
//                    url = "https://bgm.tv/person/${person.id}"
//                    hideUrl = true
//                    thumbUrl = person.images.getGrid()
//                    inputMessageContent = InputTextMessageContent(
//                        "${person.name} / $career / ${person.type.personType()}" +
//                                "\n\n${related}" +
//                                "\n\n${info}" +
//                                "\n\n${detail?.summary?.subSummary()}"
//                    ).apply {
//                        entities = listOf(MessageEntity(MessageEntityType.TEXT_LINK, 0, person.name.length).apply {
//                            url = person.images.getLarge()
//                        })
//                    }
//                    replyMarkup = InlineKeyboardMarkup(
//                        listOf(
//                            listOf(
//                                InlineKeyboardButton("详情").apply { url = "https://bgm.tv/person/${person.id}" },
//                                InlineKeyboardButton("条目").apply { switchInlineQueryCurrentChat = "/subject-subjects ${person.id}" },
//                                InlineKeyboardButton("角色").apply { switchInlineQueryCurrentChat = "/subject-characters ${person.id}" },
//                            )
//                        )
//                    )
//                }
//            }
//        }.join()
//    }

    private fun person2InlineResult(details: List<PersonDetail>): List<InlineQueryResultArticle> {
        return details.map { p ->
            val info = p.infobox.formatInfoBox()
            InlineQueryResultArticle("P${p.id}", p.name).apply {
                description = p.gender.gender()
                thumbUrl = p.images.getGrid()
                inputMessageContent = InputTextMessageContent("${p.name}\n\n$info\n\n${p.summary.subSummary()}").apply {
                    entities = listOf(MessageEntity(MessageEntityType.TEXT_LINK, 0, p.name.length).apply {
                        url = p.images?.large?.takeIf { it.isNotBlank() } ?: DEFAULT_IMAGE_URL
                    })
                }
                replyMarkup = InlineKeyboardMarkup(
                    listOf(
                        listOf(
                            InlineKeyboardButton("详情").apply { url = "https://bgm.tv/person/${p.id}" },
                        )
                    )
                )
            }
        }
    }

    private fun buildAnswer(count: Int, queryId: String): AnswerInlineQuery {
        return AnswerInlineQuery(queryId).apply {
            switchPmText = "共 $count 条结果"
            switchPmParameter = "help"
        }
    }

    private fun <T> List<T>.handleOffset(offset: Int): List<T> {
        if (this.size <= 1) return this
        val toIndex = min(DEFAULT_SIZE * offset, this.size)
        val fromIndex = min((offset - 1) * DEFAULT_SIZE, toIndex)
        return if (fromIndex == toIndex) emptyList()
        else this.subList((offset - 1) * DEFAULT_SIZE, min(DEFAULT_SIZE * offset, this.size))
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

    private fun Int.artistType(): String {
        return when (this) {
            1 -> "" //book
            2 -> "CV" //anime
            3 -> "" //music
            4 -> "演员" //game
            6 -> "演员" //real
            else -> ""        //?
        }
    }

    private fun String?.gender(): String {
        return when (this) {
            "male" -> "男"
            "female" -> "女"
            null -> "未知"
            else -> this
        }
    }

    private fun Int.week(): String {
        return when (this) {
            1 -> "星期一"
            2 -> "星期二"
            3 -> "星期三"
            4 -> "星期四"
            5 -> "星期五"
            6 -> "星期六"
            7 -> "星期天"
            else -> "？？？"
        }
    }

    private fun Int.personType(): String {
        return when (this) {
            1 -> "个人"
            2 -> "公司"
            3 -> "组合"
            else -> "未知活动形式"
        }
    }

    private fun String.career(): String {
        return when (this) {
            "producer" -> "制作人"
            "mangaka" -> "漫画家"
            "artist" -> "艺术家"
            "seiyu" -> "声优"
            "writer" -> "作家"
            "illustrator" -> "画家"
            "actor" -> "演员"
            else -> "未知职业"
        }
    }

    private fun Int.blood(): String {
        return when (this) {
            0 -> "A血型"
            1 -> "B血型"
            2 -> "O血型"
            else -> "未知"
        }
    }

    private fun String.subSummary(): String {
        return if (this.length > 300) {
            this.take(300) + "..."
        } else this
    }

    private fun JsonNode?.formatInfoBox(): String? {
        return this?.joinToString("\n") {
            val valueNode = it.findValue("value")
            val value = if (valueNode.isTextual) valueNode.textValue()
            else valueNode
                .toList()
                .joinToString(" | ") { it.findValue("v").textValue() + it.findValue("k")?.textValue()?.replace("\"", "")?.let { "($it)" } ?: "" }
            "${it.findValue("key").textValue()}: $value"
        }
    }
}

