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
 * 用户组 <br> 1 = 管理员 <br> 2 = Bangumi 管理猿 <br> 3 = 天窗管理猿 <br> 4 = 禁言用户 <br> 5 = 禁止访问用户 <br> 8 = 人物管理猿 <br> 9 = 维基条目管理猿 <br> 10 = 用户 <br> 11 = 维基人
 *
 * Values: _1,_2,_3,_4,_5,_8,_9,_10,_11
 */
@Serializable
enum class LegacyUserGroup(val value: kotlin.Int) {

    @SerialName(value = "1")
    _1(1),

    @SerialName(value = "2")
    _2(2),

    @SerialName(value = "3")
    _3(3),

    @SerialName(value = "4")
    _4(4),

    @SerialName(value = "5")
    _5(5),

    @SerialName(value = "8")
    _8(8),

    @SerialName(value = "9")
    _9(9),

    @SerialName(value = "10")
    _10(10),

    @SerialName(value = "11")
    _11(11);

    /**
     * Override [toString()] to avoid using the enum variable name as the value, and instead use
     * the actual value defined in the API spec file.
     *
     * This solves a problem when the variable name and its value are different, and ensures that
     * the client sends the correct enum values to the server always.
     */
    override fun toString(): kotlin.String = value.toString()

    companion object {
        /**
         * Converts the provided [data] to a [String] on success, null otherwise.
         */
        fun encode(data: kotlin.Any?): kotlin.String? = if (data is LegacyUserGroup) "$data" else null

        /**
         * Returns a valid [LegacyUserGroup] for [data], null otherwise.
         */
        fun decode(data: kotlin.Any?): LegacyUserGroup? = data?.let {
            val normalizedData = "$it".lowercase()
            values().firstOrNull { value ->
                it == value || normalizedData == "$value".lowercase()
            }
        }
    }
}

