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
 * type of a character 角色，机体，舰船，组织...
 *
 * Values: Character,Mechanic,Ship,Organization
 */
@Serializable
enum class CharacterType(val value: kotlin.Int) {

    @SerialName(value = "1")
    Character(1),

    @SerialName(value = "2")
    Mechanic(2),

    @SerialName(value = "3")
    Ship(3),

    @SerialName(value = "4")
    Organization(4);

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
        fun encode(data: kotlin.Any?): kotlin.String? = if (data is CharacterType) "$data" else null

        /**
         * Returns a valid [CharacterType] for [data], null otherwise.
         */
        fun decode(data: kotlin.Any?): CharacterType? = data?.let {
            val normalizedData = "$it".lowercase()
            values().firstOrNull { value ->
                it == value || normalizedData == "$value".lowercase()
            }
        }
    }
}

