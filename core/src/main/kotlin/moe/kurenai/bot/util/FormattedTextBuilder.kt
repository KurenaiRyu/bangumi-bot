package moe.kurenai.bot.util

import it.tdlight.jni.TdApi

class FormattedTextBuilder {

    private val sb = StringBuilder()
    private val entities = mutableListOf<TdApi.TextEntity>()

    fun appendText(text: String): FormattedTextBuilder {
        sb.append(text)
        return this
    }

    fun appendLink(text: String, link: String): FormattedTextBuilder {
        entities.add(TdApi.TextEntity(sb.length, text.length, TdApi.TextEntityTypeTextUrl(link)))
        sb.append(text)
        return this
    }

    fun appendBold(text: String): FormattedTextBuilder {
        entities.add(TdApi.TextEntity(sb.length, text.length, TdApi.TextEntityTypeBold()))
        sb.append(text)
        return this
    }

    fun appendQuote(text: String): FormattedTextBuilder {
        val start = sb.length
        sb.append(text)
        if (!sb.endsWith('\n')) sb.appendLine()
        val end = sb.length
        entities.add(TdApi.TextEntity(start, end - start, TdApi.TextEntityTypeBlockQuote()))
        return this
    }

    fun wrapQuote(block: FormattedTextBuilder.() -> Unit) {
        val start = sb.length
        block()
        if (!sb.endsWith('\n')) sb.appendLine()
        val end = sb.length
        entities.add(TdApi.TextEntity(start, end - start, TdApi.TextEntityTypeBlockQuote()))
    }

    fun appendCode(text: String): FormattedTextBuilder {
        entities.add(TdApi.TextEntity(sb.length, text.length, TdApi.TextEntityTypeCode()))
        sb.append(text)
        return this
    }

    fun appendStrikethrough(text: String): FormattedTextBuilder {
        entities.add(TdApi.TextEntity(sb.length, text.length, TdApi.TextEntityTypeStrikethrough()))
        sb.append(text)
        return this
    }

    fun appendMentionName(text: String, userId: Long): FormattedTextBuilder {
        entities.add(TdApi.TextEntity(sb.length, text.length, TdApi.TextEntityTypeMentionName(userId)))
        sb.append(text)
        return this
    }

    fun appendLine(): FormattedTextBuilder {
        sb.appendLine()
        return this
    }

    fun build(): TdApi.FormattedText {
        return TdApi.FormattedText(sb.toString(), entities.toTypedArray())
    }

    companion object {
        inline fun formattedText(block: FormattedTextBuilder.() -> Unit): TdApi.FormattedText {
            return FormattedTextBuilder().apply(block).build()
        }
    }

}
