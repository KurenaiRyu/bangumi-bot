package moe.kurenai.bot.repository.bangumi

import io.ktor.http.*
import it.tdlight.jni.TdApi.*
import moe.kurenai.bgm.model.person.PersonDetail
import moe.kurenai.bgm.model.subject.getLarge
import moe.kurenai.bgm.model.subject.getSmall
import moe.kurenai.bot.repository.bangumi.BangumiApi.personCache
import moe.kurenai.bot.repository.bangumi.BangumiApi.result
import moe.kurenai.bot.repository.bangumi.BangumiApi.useApi
import moe.kurenai.bot.util.BgmUtil.format
import moe.kurenai.bot.util.BgmUtil.formatToList
import moe.kurenai.bot.util.BgmUtil.toGrid
import moe.kurenai.bot.util.HttpUtil

/**
 * @author Kurenai
 * @since 2023/1/26 14:59
 */
internal object PersonRepository {

    suspend fun findById(id: Int, token: String? = null): PersonDetail {
        return personCache.get(id) { _ ->
            useApi(token) {
                it.getPersonById(id).result()
            }
        }
    }

//    suspend fun findByIds(ids: Collection<Int>, token: String? = null): Collection<PersonDetail> {
//        return personCache.getAll(ids) { keys ->
//            keys.map { k ->
//                CoroutineScope(Dispatchers.IO).async {
//                    BangumiBot.bgmClient.send(GetPersonDetail(k).apply { this.token = token })
//                }
//            }.associate {
//                val subject = it.await()
//                subject.id to subject
//            }
//        }.values
//    }

    suspend fun getContent(person: PersonDetail, link: String): Array<InputInlineQueryResult> {
        val title = person.name
        val infoBox = person.infobox.formatToList()

        val entities = arrayOf(TextEntity(0, person.name.length, TextEntityTypeTextUrl(link)))
        val caption = listOfNotNull(title, infoBox.format()).joinToString("\n\n")
        val formattedText = FormattedText(caption, entities)
        val default = InputInlineQueryResultPhoto().apply {
            this.id = "P${person.id} - img"
            photoUrl = person.images.getLarge().also {
//                TelegramUserBot.fetchRemoteFileIdByUrl(it)
            }
            thumbnailUrl = person.images.getSmall()
            this.title = person.name
            this.inputMessageContent = InputMessagePhoto().apply {
                this.caption = formattedText
            }
        }

        val resultList = mutableListOf(
            InputInlineQueryResultArticle().apply {
                val url = person.images.getLarge().toGrid()
                this.id = "P${person.id} - text"
                this.url = url
                thumbnailUrl = url
                this.title = person.name
                this.inputMessageContent = InputMessageText().apply {
                    text = FormattedText(
                        " $caption", arrayOf(
                            TextEntity(0, 1, TextEntityTypeTextUrl(person.images.getLarge())),
                            TextEntity(1, person.name.length, TextEntityTypeTextUrl(link))
                        )
                    )
                }
            },
            default
        )
        infoBox.filter { it.second.startsWith("http") }.flatMap {
            kotlin.runCatching {
                HttpUtil.getOgImageUrl(Url(it.second))
            }.getOrDefault(emptyList())
        }.forEachIndexed { i, url ->
//            TelegramUserBot.fetchRemoteFileIdByUrl(url)
            resultList.add(InputInlineQueryResultPhoto().apply {
                this.id = "P${person.id} - ${i + 1}"
                photoUrl = url
                thumbnailUrl = url
                this.title = person.name
                this.inputMessageContent = InputMessagePhoto().apply {
                    this.caption = formattedText
                }
            })
        }
        return resultList.toTypedArray()
    }

}
