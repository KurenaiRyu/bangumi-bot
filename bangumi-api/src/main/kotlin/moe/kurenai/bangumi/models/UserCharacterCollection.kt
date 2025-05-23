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

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param id
 * @param name
 * @param type 角色，机体，舰船，组织...
 * @param createdAt
 * @param images object with some size of images, this object maybe `null`
 */
@Serializable

data class UserCharacterCollection(

    @SerialName(value = "id")
    val id: kotlin.Int,

    @SerialName(value = "name")
    val name: kotlin.String,

    /* 角色，机体，舰船，组织... */
    @Contextual @SerialName(value = "type")
    val type: CharacterType,

    @Contextual @SerialName(value = "created_at")
    val createdAt: java.time.OffsetDateTime,

    /* object with some size of images, this object maybe `null` */
    @SerialName(value = "images")
    val images: PersonImages? = null

) {


}

