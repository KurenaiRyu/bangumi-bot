package moe.kurenai.bot.command

import it.tdlight.jni.TdApi.UpdateNewInlineQuery
import java.net.URI

interface InlineHandler {

    suspend fun handle(inlineQuery: UpdateNewInlineQuery, uri: URI): HandleResult

}
