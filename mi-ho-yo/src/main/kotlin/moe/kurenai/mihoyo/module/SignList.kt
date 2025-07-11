package moe.kurenai.mihoyo.module


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SignList(
    val awards: List<Award> = listOf(),
    val biz: String = "",
    val month: Int = 0,
    val resign: Boolean = false,
    @SerialName("short_extra_award")
    val shortExtraAward: ShortExtraAward = ShortExtraAward()
) {
    @Serializable
    data class Award(
        val cnt: Int = 0,
        val icon: String = "",
        val name: String = ""
    )

    @Serializable
    data class ShortExtraAward(
        @SerialName("end_time")
        val endTime: String = "",
        @SerialName("end_timestamp")
        val endTimestamp: String = "",
        @SerialName("has_extra_award")
        val hasExtraAward: Boolean = false,
        val list: List<JsonElement> = listOf(),
        @SerialName("start_time")
        val startTime: String = "",
        @SerialName("start_timestamp")
        val startTimestamp: String = ""
    )
}
