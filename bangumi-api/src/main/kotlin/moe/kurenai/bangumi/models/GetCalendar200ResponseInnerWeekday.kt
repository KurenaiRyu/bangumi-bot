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
 *
 *
 * @param en
 * @param cn
 * @param ja
 * @param id
 */
@Serializable

data class GetCalendar200ResponseInnerWeekday(

    @SerialName(value = "en")
    val en: kotlin.String? = null,

    @SerialName(value = "cn")
    val cn: kotlin.String? = null,

    @SerialName(value = "ja")
    val ja: kotlin.String? = null,

    @SerialName(value = "id")
    val id: kotlin.Int? = null

) {


}

