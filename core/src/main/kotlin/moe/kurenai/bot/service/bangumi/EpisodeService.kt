package moe.kurenai.bot.service.bangumi

import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.InputInlineQueryResult
import moe.kurenai.bangumi.models.EpisodeDetail
import moe.kurenai.bangumi.models.Subject
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi
import moe.kurenai.bot.util.FormattedTextBuilder

object EpisodeService {

    context(token: UserAccessToken?)
    suspend fun findById(id: Int): EpisodeDetail {
        return useApi {
            it.getEpisodeById(id).result()
        }
    }

    fun getContent(episode: EpisodeDetail, subject: Subject, url: String): Array<InputInlineQueryResult> {
        val builder = FormattedTextBuilder()
        builder
            .appendBold(episode.name)
            .appendLine()
            .appendText("${subject.name} #${episode.ep?.toInt()?:"??"}")
            .appendLine().appendLine()
            .wrapQuote {
                appendText(episode.desc)
            }
            .appendText(url)

        return arrayOf(TdApi.InputInlineQueryResultArticle().apply {
            this.url = url
            this.id = episode.id.toString()
            this.title = "${episode.name} / ${subject.name}"
            this.inputMessageContent = TdApi.InputMessageText().apply {
                this.text = builder.build()
            }
        })
    }
}
