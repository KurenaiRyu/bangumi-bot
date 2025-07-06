package moe.kurenai.mihoyo.module.zzz


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AvatarList(
    @SerialName("avatar_list")
    val avatarList: List<Avatar> = listOf()
) {
    @Serializable
    data class Avatar(
        @SerialName("avatar_profession")
        val avatarProfession: Int = 0,
        @SerialName("awaken_state")
        val awakenState: String = "",
        @SerialName("camp_name_mi18n")
        val campNameMi18n: String = "",
        @SerialName("element_type")
        val elementType: Int = 0,
        @SerialName("full_name_mi18n")
        val fullNameMi18n: String = "",
        @SerialName("group_icon_path")
        val groupIconPath: String = "",
        @SerialName("hollow_icon_path")
        val hollowIconPath: String = "",
        val id: Int = 0,
        @SerialName("is_chosen")
        val isChosen: Boolean = false,
        val level: Int = 0,
        @SerialName("name_mi18n")
        val nameMi18n: String = "",
        val rank: Int = 0,
        val rarity: String = "",
        @SerialName("role_square_url")
        val roleSquareUrl: String = "",
        @SerialName("sub_element_type")
        val subElementType: Int = 0
    )
}
