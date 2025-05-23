/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package moe.kurenai.bangumi.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 收藏人数
 *
 * @param wish 想做
 * @param collect 做过
 * @param doing 在做
 * @param onHold 搁置
 * @param dropped 抛弃
 */
@Serializable

data class LegacySubjectSmallCollection(

    /* 想做 */
    @SerialName(value = "wish")
    val wish: kotlin.Int? = null,

    /* 做过 */
    @SerialName(value = "collect")
    val collect: kotlin.Int? = null,

    /* 在做 */
    @SerialName(value = "doing")
    val doing: kotlin.Int? = null,

    /* 搁置 */
    @SerialName(value = "on_hold")
    val onHold: kotlin.Int? = null,

    /* 抛弃 */
    @SerialName(value = "dropped")
    val dropped: kotlin.Int? = null

) {


}

