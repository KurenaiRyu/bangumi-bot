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

    fun appendQuote(text: String, expendable: Boolean = true): FormattedTextBuilder {
        val start = sb.length
        sb.append(text)
        if (!sb.endsWith('\n')) sb.appendLine()
        val end = sb.length
        entities.add(TdApi.TextEntity(start, end - start,
            if (expendable) TdApi.TextEntityTypeExpandableBlockQuote() else TdApi.TextEntityTypeBlockQuote()))
        return this
    }

    fun wrapQuote(expendable: Boolean = true, block: FormattedTextBuilder.() -> Unit): FormattedTextBuilder {
        return wrapQuoteIfNeeded(expendable, determinateLength = Int.MAX_VALUE, block = block)
    }

    fun wrapQuoteIfNeeded(
        expendable: Boolean = true,
        determinateLength: Int = 100,
        block: FormattedTextBuilder.() -> Unit
    ): FormattedTextBuilder {
        val start = sb.length
        block()
        if (sb.endsWith("\n\n")) sb.deleteAt(sb.lastIndex)
        if (!sb.endsWith('\n')) sb.appendLine()
        val end = sb.length

        val length = end - start
        if (length >= determinateLength) {
            entities.add(TdApi.TextEntity(start, length,
                if (expendable) TdApi.TextEntityTypeExpandableBlockQuote() else TdApi.TextEntityTypeBlockQuote()))
        } else {
            if (start > 0 && sb.get(start - 1) != '\n') sb.insert(start, "\n\n")
            else sb.insert(start, '\n')

            val offset = sb.length - end
            for (entity in entities) {
                if (entity.offset >= start) {
                    entity.offset += offset
                }
            }
        }
        return this
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

    fun <T> joinList(collection: Collection<T>, separator: String = ", ", block: FormattedTextBuilder.(item: T) -> Unit) {
        var first = true
        for (item in collection) {
            if (first) {
                block(item)
                first = false
            } else {
                appendText(separator)
                block(item)
            }
        }
    }

    fun build(): TdApi.FormattedText {
        if (sb.endsWith("\n\n")) {
            sb.deleteAt(sb.lastIndex)
            for (entity in entities) {
                if (entity.offset + entity.length > sb.length) {
                    entity.length = sb.length - entity.length
                }
            }
        }

        return TdApi.FormattedText(sb.toString(), entities.toTypedArray())
    }

    companion object {
        inline fun formattedText(block: FormattedTextBuilder.() -> Unit): TdApi.FormattedText {
            return FormattedTextBuilder().apply(block).build()
        }
    }

}
