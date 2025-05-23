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
 * 头像地址
 *
 * @param large
 * @param medium
 * @param small
 */
@Serializable

data class LegacyUserAvatar(

    @SerialName(value = "large")
    val large: kotlin.String? = null,

    @SerialName(value = "medium")
    val medium: kotlin.String? = null,

    @SerialName(value = "small")
    val small: kotlin.String? = null

) {


}

