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
 * @param weekday
 * @param items
 */
@Serializable

data class GetCalendar200ResponseInner(

    @SerialName(value = "weekday")
    val weekday: GetCalendar200ResponseInnerWeekday? = null,

    @SerialName(value = "items")
    val items: kotlin.collections.List<LegacySubjectSmall>? = null

) {


}

