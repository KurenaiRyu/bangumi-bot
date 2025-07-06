package moe.kurenai.mihoyo.module.zzz


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.kurenai.mihoyo.module.common.DateTime

@Serializable
data class Challenge(
    @SerialName("all_floor_detail")
    val allFloorDetail: List<AllFloorDetail> = listOf(),
    @SerialName("battle_time_47")
    val battleTime47: Int = 0,
    @SerialName("begin_time")
    val beginTime: String = "",
    @SerialName("end_time")
    val endTime: String = "",
    @SerialName("fast_layer_time")
    val fastLayerTime: Int = 0,
    @SerialName("hadal_begin_time")
    val hadalBeginTime: DateTime = DateTime(),
    @SerialName("hadal_end_time")
    val hadalEndTime: DateTime = DateTime(),
    @SerialName("has_data")
    val hasData: Boolean = false,
    @SerialName("max_layer")
    val maxLayer: Int = 0,
    @SerialName("rating_list")
    val ratingList: List<Rating> = listOf(),
    @SerialName("schedule_id")
    val scheduleId: Int = 0
) {
    @Serializable
    data class AllFloorDetail(
        val buffs: List<Buff> = listOf(),
        @SerialName("challenge_time")
        val challengeTime: String = "",
        @SerialName("floor_challenge_time")
        val floorChallengeTime: DateTime = DateTime(),
        @SerialName("layer_id")
        val layerId: Int = 0,
        @SerialName("layer_index")
        val layerIndex: Int = 0,
        @SerialName("node_1")
        val node1: Node = Node(),
        @SerialName("node_2")
        val node2: Node = Node(),
        val rating: String = "",
        @SerialName("zone_name")
        val zoneName: String = ""
    ) {
        @Serializable
        data class Buff(
            val text: String = "",
            val title: String = ""
        )

        @Serializable
        data class Node(
            val avatars: List<Avatar> = listOf(),
            @SerialName("battle_time")
            val battleTime: Int = 0,
            val buddy: Buddy = Buddy(),
            @SerialName("element_type_list")
            val elementTypeList: List<Int> = listOf(),
            @SerialName("monster_info")
            val monsterInfo: MonsterInfo = MonsterInfo()
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
            data class Buddy(
                @SerialName("bangboo_rectangle_url")
                val bangbooRectangleUrl: String = "",
                val id: Int = 0,
                val level: Int = 0,
                val rarity: String = ""
            )

            @Serializable
            data class MonsterInfo(
                val level: Int = 0,
                val list: List<Monster> = listOf()
            ) {
                @Serializable
                data class Monster(
                    @SerialName("bg_icon")
                    val bgIcon: String = "",
                    @SerialName("elec_weakness")
                    val elecWeakness: Int = 0,
                    @SerialName("ether_weakness")
                    val etherWeakness: Int = 0,
                    @SerialName("fire_weakness")
                    val fireWeakness: Int = 0,
                    @SerialName("ice_weakness")
                    val iceWeakness: Int = 0,
                    @SerialName("icon_url")
                    val iconUrl: String = "",
                    val id: Int = 0,
                    val name: String = "",
                    @SerialName("physics_weakness")
                    val physicsWeakness: Int = 0,
                    @SerialName("race_icon")
                    val raceIcon: String = "",
                    @SerialName("weak_element_type")
                    val weakElementType: Int = 0
                )
            }
        }
    }

    @Serializable
    data class Rating(
        val rating: String = "",
        val times: Int = 0
    )
}
