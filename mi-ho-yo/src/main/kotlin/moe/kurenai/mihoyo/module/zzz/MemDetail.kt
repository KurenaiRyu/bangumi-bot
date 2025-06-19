package moe.kurenai.mihoyo.module.zzz


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemDetail(
    @SerialName("avatar_icon")
    val avatarIcon: String = "",
    @SerialName("end_time")
    val endTime: EndTime = EndTime(),
    @SerialName("has_data")
    val hasData: Boolean = false,
    val list: List<Record> = listOf(),
    @SerialName("nick_name")
    val nickName: String = "",
    @SerialName("rank_percent")
    val rankPercent: Int = 0,
    @SerialName("start_time")
    val startTime: StartTime = StartTime(),
    @SerialName("total_score")
    val totalScore: Int = 0,
    @SerialName("total_star")
    val totalStar: Int = 0,
    @SerialName("zone_id")
    val zoneId: Int = 0
) {
    @Serializable
    data class EndTime(
        val day: Int = 0,
        val hour: Int = 0,
        val minute: Int = 0,
        val month: Int = 0,
        val second: Int = 0,
        val year: Int = 0
    )

    @Serializable
    data class Record(
        @SerialName("avatar_list")
        val avatarList: List<Avatar> = listOf(),
        val boss: List<Bos> = listOf(),
        val buddy: Buddy = Buddy(),
        val buffer: List<Buffer> = listOf(),
        @SerialName("challenge_time")
        val challengeTime: ChallengeTime = ChallengeTime(),
        val score: Int = 0,
        val star: Int = 0,
        @SerialName("total_star")
        val totalStar: Int = 0
    ) {
        @Serializable
        data class Avatar(
            @SerialName("avatar_profession")
            val avatarProfession: Int = 0,
            @SerialName("element_type")
            val elementType: Int = 0,
            val id: Int = 0,
            val level: Int = 0,
            val rank: Int = 0,
            val rarity: String = "",
            @SerialName("role_square_url")
            val roleSquareUrl: String = "",
            @SerialName("sub_element_type")
            val subElementType: Int = 0
        )

        @Serializable
        data class Bos(
            @SerialName("bg_icon")
            val bgIcon: String = "",
            val icon: String = "",
            val name: String = "",
            @SerialName("race_icon")
            val raceIcon: String = ""
        )

        @Serializable
        data class Buddy(
            @SerialName("bangboo_rectangle_url")
            val bangbooRectangleUrl: String = "",
            val id: Int = 0,
            val level: Int = 0,
            val rarity: String = ""
        )

        @Serializable
        data class Buffer(
            val desc: String = "",
            val icon: String = "",
            val name: String = ""
        )

        @Serializable
        data class ChallengeTime(
            val day: Int = 0,
            val hour: Int = 0,
            val minute: Int = 0,
            val month: Int = 0,
            val second: Int = 0,
            val year: Int = 0
        )
    }

    @Serializable
    data class StartTime(
        val day: Int = 0,
        val hour: Int = 0,
        val minute: Int = 0,
        val month: Int = 0,
        val second: Int = 0,
        val year: Int = 0
    )
}
