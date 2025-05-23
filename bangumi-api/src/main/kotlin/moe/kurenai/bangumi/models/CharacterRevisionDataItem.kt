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
 * @param infobox
 * @param summary
 * @param name
 * @param extra
 */
@Serializable

data class CharacterRevisionDataItem(

    @SerialName(value = "infobox")
    val infobox: kotlin.String,

    @SerialName(value = "summary")
    val summary: kotlin.String,

    @SerialName(value = "name")
    val name: kotlin.String,

    @SerialName(value = "extra")
    val extra: RevisionExtra

) {


}

