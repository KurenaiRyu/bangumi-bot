package moe.kurenai.bangumi.constant

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PersonCareer(val value: String, val displayNameCN: String) {

    @SerialName(value = "producer")
    PRODUCER("producer", "制作人"),

    @SerialName(value = "mangaka")
    MANGAKA("mangaka", "漫画家"),

    @SerialName(value = "artist")
    ARTIST("artist", "歌手"),

    @SerialName(value = "seiyu")
    SEIYU("seiyu", "声优"),

    @SerialName(value = "writer")
    WRITER("writer", "作家"),

    @SerialName(value = "illustrator")
    ILLUSTRATOR("illustrator", "画师"),

    @SerialName(value = "actor")
    ACTOR("actor", "演员");

    /**
     * Override [toString()] to avoid using the enum variable name as the value, and instead use
     * the actual value defined in the API spec file.
     *
     * This solves a problem when the variable name and its value are different, and ensures that
     * the client sends the correct enum values to the server always.
     */
    override fun toString(): String = value

    companion object {
        /**
         * Converts the provided [data] to a [String] on success, null otherwise.
         */
        fun encode(data: Any?): String? = if (data is PersonCareer) "$data" else null

        /**
         * Returns a valid [PersonCareer] for [data], null otherwise.
         */
        fun decode(data: Any?): PersonCareer? = data?.let {
            val normalizedData = "$it".lowercase()
            entries.firstOrNull { value ->
                it == value || normalizedData == "$value".lowercase()
            }
        }
    }
}
