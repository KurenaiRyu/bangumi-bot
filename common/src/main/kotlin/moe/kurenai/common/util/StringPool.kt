package moe.kurenai.common.util

/**
 * Copy to jodd.util
 *
 * 字符串常量池，为避免硬编码
 * @author Kurenai
 * @since 2021-07-08
 */
object StringPool {
    const val AMPERSAND = "&"
    const val AND = "and"
    const val AT = "@"
    const val ASTERISK = "*"
    const val STAR = ASTERISK
    const val BACK_SLASH = "\\"
    const val COLON = ":"
    const val COMMA = ","
    const val DASH = "-"
    const val DOLLAR = "$"
    const val DOT = "."
    const val DOTDOT = ".."
    const val DOT_CLASS = ".class"
    const val DOT_JAVA = ".java"
    const val EMPTY = ""
    const val EQUALS = "="
    const val FALSE = "false"
    const val SLASH = "/"
    const val HASH = "#"
    const val HAT = "^"
    const val LEFT_BRACE = "{"
    const val LEFT_BRACKET = "("
    const val LEFT_CHEV = "<"
    const val NEWLINE = "\n"
    const val N = "n"
    const val NO = "no"
    const val NULL = "null"
    const val OFF = "off"
    const val ON = "on"
    const val PERCENT = "%"
    const val PIPE = "|"
    const val PLUS = "+"
    const val QUESTION_MARK = "?"
    const val EXCLAMATION_MARK = "!"
    const val QUOTE = "\""
    const val RETURN = "\r"
    const val TAB = "\t"
    const val RIGHT_BRACE = "}"
    const val RIGHT_BRACKET = ")"
    const val RIGHT_CHEV = ">"
    const val SEMICOLON = ";"
    const val SINGLE_QUOTE = "'"
    const val BACKTICK = "`"
    const val SPACE = " "
    const val TILDA = "~"
    const val LEFT_SQ_BRACKET = "["
    const val RIGHT_SQ_BRACKET = "]"
    const val TRUE = "true"
    const val UNDERSCORE = "_"
    const val UTF_8 = "UTF-8"
    const val US_ASCII = "US-ASCII"
    const val ISO_8859_1 = "ISO-8859-1"
    const val Y = "y"
    const val YES = "yes"
    const val ONE = "1"
    const val ZERO = "0"
    const val DOLLAR_LEFT_BRACE = "\${"
    const val CRLF = "\r\n"
    const val HTML_NBSP = "&nbsp;"
    const val HTML_AMP = "&amp"
    const val HTML_QUOTE = "&quot;"
    const val HTML_LT = "&lt;"
    const val HTML_GT = "&gt;"

    // ---------------------------------------------------------------- array
    val EMPTY_ARRAY = arrayOfNulls<String>(0)
    val BYTES_NEW_LINE = "\n".toByteArray()
}
