package moe.kurenai.mihoyo.module


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BindInfoList(
    val list: List<BindInfo> = listOf()
) {
    @Serializable
    data class BindInfo(
        @SerialName("game_biz")
        val gameBiz: String = "",
        @SerialName("game_uid")
        val gameUid: String = "",
        @SerialName("is_chosen")
        val isChosen: Boolean = false,
        @SerialName("is_official")
        val isOfficial: Boolean = false,
        val level: Int = 0,
        val nickname: String = "",
        val region: String = "",
        @SerialName("region_name")
        val regionName: String = "",
        val unmask: List<String> = listOf()
    )
}
