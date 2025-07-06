package moe.kurenai.mihoyo.module.zzz


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AvatarDetail(
    @SerialName("avatar_list")
    val avatarList: List<Avatar> = listOf(),
    @SerialName("avatar_wiki")
    val avatarWiki: Map<String, String> = mapOf(),
    @SerialName("cultivate_equip")
    val cultivateEquip: Map<String, String> = mapOf(),
    @SerialName("cultivate_index")
    val cultivateIndex: Map<String, String> = mapOf(),
    @SerialName("equip_wiki")
    val equipWiki: Map<String, String> = mapOf(),
    @SerialName("strategy_wiki")
    val strategyWiki: Map<String, String> = mapOf(),
    @SerialName("weapon_wiki")
    val weaponWiki: Map<String, String> = mapOf(),
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
        val equip: List<Equip> = listOf(),
        @SerialName("equip_plan_info")
        val equipPlanInfo: EquipPlanInfo = EquipPlanInfo(),
        @SerialName("full_name_mi18n")
        val fullNameMi18n: String = "",
        @SerialName("group_icon_path")
        val groupIconPath: String = "",
        @SerialName("hollow_icon_path")
        val hollowIconPath: String = "",
        val id: Int = 0,
        val level: Int = 0,
        @SerialName("name_mi18n")
        val nameMi18n: String = "",
        val properties: List<Property> = listOf(),
        val rank: Int = 0,
        val ranks: List<Rank> = listOf(),
        val rarity: String = "",
        @SerialName("role_square_url")
        val roleSquareUrl: String = "",
        @SerialName("role_vertical_painting_url")
        val roleVerticalPaintingUrl: String = "",
        val skills: List<Skill> = listOf(),
//        @SerialName("skin_list")  // TODO
//        val skinList: List<Any?> = listOf(),
        @SerialName("sub_element_type")
        val subElementType: Int = 0,
        @SerialName("us_full_name")
        val usFullName: String = "",
        @SerialName("vertical_painting_color")
        val verticalPaintingColor: String = "",
        val weapon: Weapon = Weapon()
    ) {
        @Serializable
        data class Equip(
            @SerialName("all_hit")
            val allHit: Boolean = false,
            @SerialName("equip_suit")
            val equipSuit: EquipSuit = EquipSuit(),
            @SerialName("equipment_type")
            val equipmentType: Int = 0,
            val icon: String = "",
            val id: Int = 0,
            @SerialName("invalid_property_cnt")
            val invalidPropertyCnt: Int = 0,
            val level: Int = 0,
            @SerialName("main_properties")
            val mainProperties: List<MainProperty> = listOf(),
            val name: String = "",
            val properties: List<Property> = listOf(),
            val rarity: String = ""
        ) {
            @Serializable
            data class EquipSuit(
                val desc1: String = "",
                val desc2: String = "",
                val name: String = "",
                val own: Int = 0,
                @SerialName("suit_id")
                val suitId: Int = 0
            )

            @Serializable
            data class MainProperty(
                val add: Int = 0,
                val base: String = "",
                val level: Int = 0,
                @SerialName("property_id")
                val propertyId: Int = 0,
                @SerialName("property_name")
                val propertyName: String = "",
                @SerialName("system_id")
                val systemId: Int = 0,
                val valid: Boolean = false
            )

            @Serializable
            data class Property(
                val add: Int = 0,
                val base: String = "",
                val level: Int = 0,
                @SerialName("property_id")
                val propertyId: Int = 0,
                @SerialName("property_name")
                val propertyName: String = "",
                @SerialName("system_id")
                val systemId: Int = 0,
                val valid: Boolean = false
            )
        }

        @Serializable
        data class EquipPlanInfo(
            @SerialName("cultivate_info")
            val cultivateInfo: CultivateInfo = CultivateInfo(),
            @SerialName("custom_info")
            val customInfo: CustomInfo = CustomInfo(),
            @SerialName("equip_rating")
            val equipRating: String = "",
            @SerialName("equip_rating_score")
            val equipRatingScore: Double = 0.0,
            @SerialName("game_default")
            val gameDefault: GameDefault = GameDefault(),
            @SerialName("plan_effective_property_list")
            val planEffectivePropertyList: List<PlanEffectiveProperty> = listOf(),
            @SerialName("plan_only_special_property")
            val planOnlySpecialProperty: Boolean = false,
            val type: Int = 0,
            @SerialName("valid_property_cnt")
            val validPropertyCnt: Int = 0
        ) {
            @Serializable
            data class CultivateInfo(
                @SerialName("is_delete")
                val isDelete: Boolean = false,
                val name: String = "",
                @SerialName("old_plan")
                val oldPlan: Boolean = false,
                @SerialName("plan_id")
                val planId: String = ""
            )

            @Serializable
            data class CustomInfo(
                @SerialName("property_list")
                val propertyList: List<Property> = listOf()
            ) {
                @Serializable
                data class Property(
                    @SerialName("full_name")
                    val fullName: String = "",
                    val id: Int = 0,
                    @SerialName("is_select")
                    val isSelect: Boolean = false,
                    val name: String = "",
                    @SerialName("system_id")
                    val systemId: Int = 0
                )
            }

            @Serializable
            data class GameDefault(
                @SerialName("property_list")
                val propertyList: List<Property> = listOf()
            ) {
                @Serializable
                data class Property(
                    @SerialName("full_name")
                    val fullName: String = "",
                    val id: Int = 0,
                    @SerialName("is_select")
                    val isSelect: Boolean = false,
                    val name: String = "",
                    @SerialName("system_id")
                    val systemId: Int = 0
                )
            }

            @Serializable
            data class PlanEffectiveProperty(
                @SerialName("full_name")
                val fullName: String = "",
                val id: Int = 0,
                @SerialName("is_select")
                val isSelect: Boolean = false,
                val name: String = "",
                @SerialName("system_id")
                val systemId: Int = 0
            )
        }

        @Serializable
        data class Property(
            val add: String = "",
            val base: String = "",
            val `final`: String = "",
            @SerialName("property_id")
            val propertyId: Int = 0,
            @SerialName("property_name")
            val propertyName: String = ""
        )

        @Serializable
        data class Rank(
            val desc: String = "",
            val id: Int = 0,
            @SerialName("is_unlocked")
            val isUnlocked: Boolean = false,
            val name: String = "",
            val pos: Int = 0
        )

        @Serializable
        data class Skill(
            @SerialName("awaken_state")
            val awakenState: String = "",
            val items: List<Item> = listOf(),
            val level: Int = 0,
            @SerialName("skill_type")
            val skillType: Int = 0
        ) {
            @Serializable
            data class Item(
                val awaken: Boolean = false,
                val text: String = "",
                val title: String = ""
            )
        }

        @Serializable
        data class Weapon(
            val icon: String = "",
            val id: Int = 0,
            val level: Int = 0,
            @SerialName("main_properties")
            val mainProperties: List<MainProperty> = listOf(),
            val name: String = "",
            val profession: Int = 0,
            val properties: List<Property> = listOf(),
            val rarity: String = "",
            val star: Int = 0,
            @SerialName("talent_content")
            val talentContent: String = "",
            @SerialName("talent_title")
            val talentTitle: String = ""
        ) {
            @Serializable
            data class MainProperty(
                val add: Int = 0,
                val base: String = "",
                val level: Int = 0,
                @SerialName("property_id")
                val propertyId: Int = 0,
                @SerialName("property_name")
                val propertyName: String = "",
                @SerialName("system_id")
                val systemId: Int = 0,
                val valid: Boolean = false
            )

            @Serializable
            data class Property(
                val add: Int = 0,
                val base: String = "",
                val level: Int = 0,
                @SerialName("property_id")
                val propertyId: Int = 0,
                @SerialName("property_name")
                val propertyName: String = "",
                @SerialName("system_id")
                val systemId: Int = 0,
                val valid: Boolean = false
            )
        }
    }
}
