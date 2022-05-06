package moe.kurenai.bot

import moe.kurenai.bot.util.StringPool.COLON

fun String.appendKey(key: Any?, connector: String = COLON): String {
    return if (key == null) {
        this
    } else if (key is String && key.isNotBlank()) {
        "$this$connector$key"
    } else {
        "$this$connector$key"
    }
}