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
 * @param type `1`, `2`, `3` 表示 `个人`, `公司`, `组合`
 * @param career
 * @param summary
 * @param locked
 * @param lastModified currently it's latest user comment time, it will be replaced by wiki modified date in the future
 * @param stat
 * @param images object with some size of images, this object maybe `null`
 * @param infobox
 * @param gender parsed from wiki, maybe null
 * @param bloodType parsed from wiki, maybe null, `1, 2, 3, 4` for `A, B, AB, O`
 * @param birthYear parsed from wiki, maybe `null`
 * @param birthMon parsed from wiki, maybe `null`
 * @param birthDay parsed from wiki, maybe `null`
 */
@Serializable

data class PersonDetail(

    @SerialName(value = "id")
    val id: kotlin.Int,

    @SerialName(value = "name")
    val name: kotlin.String,

    /* `1`, `2`, `3` 表示 `个人`, `公司`, `组合` */
    @Contextual @SerialName(value = "type")
    val type: PersonType,

    @SerialName(value = "career")
    val career: kotlin.collections.List<@Contextual PersonCareer>,

    @SerialName(value = "summary")
    val summary: kotlin.String,

    @SerialName(value = "locked")
    val locked: kotlin.Boolean,

    /* currently it's latest user comment time, it will be replaced by wiki modified date in the future */
    @Contextual @SerialName(value = "last_modified")
    val lastModified: java.time.OffsetDateTime,

    @SerialName(value = "stat")
    val stat: Stat,

    /* object with some size of images, this object maybe `null` */
    @SerialName(value = "images")
    val images: PersonImages? = null,

    @SerialName(value = "infobox")
    val infobox: kotlin.collections.List<InfoBox>? = null,

    /* parsed from wiki, maybe null */
    @SerialName(value = "gender")
    val gender: kotlin.String? = null,

    /* parsed from wiki, maybe null, `1, 2, 3, 4` for `A, B, AB, O` */
    @Contextual @SerialName(value = "blood_type")
    val bloodType: BloodType? = null,

    /* parsed from wiki, maybe `null` */
    @SerialName(value = "birth_year")
    val birthYear: kotlin.Int? = null,

    /* parsed from wiki, maybe `null` */
    @SerialName(value = "birth_mon")
    val birthMon: kotlin.Int? = null,

    /* parsed from wiki, maybe `null` */
    @SerialName(value = "birth_day")
    val birthDay: kotlin.Int? = null

) {


}

