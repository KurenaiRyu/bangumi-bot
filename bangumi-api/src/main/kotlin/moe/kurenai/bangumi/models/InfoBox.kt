package moe.kurenai.bangumi.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * @author Kurenai
 * @since 2023/3/22 22:45
 */

@Serializable
data class InfoBox(
    val key: String,
    @Serializable(InfoBoxValueDeserializer::class)
    val value: List<Item>,
) {

    @Serializable
    data class Item(
        val k: String? = null,
        val v: String? = null
    )
}

object InfoBoxValueDeserializer : KSerializer<List<InfoBox.Item>> {
    override val descriptor = ListSerializer(InfoBox.Item.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<InfoBox.Item> {
        val input = decoder as? JsonDecoder
            ?: throw IllegalStateException(
                "This serializer can be used only with Json format." +
                    "Expected Decoder to be JsonDecoder, got ${this::class}"
            )
        val element = input.decodeJsonElement()
        return if (element is JsonArray) {
            element.map {
                InfoBox.Item(
                    it.jsonObject["k"]?.jsonPrimitive?.contentOrNull,
                    it.jsonObject["v"]?.jsonPrimitive?.contentOrNull
                )
            }
        } else {
            listOf(InfoBox.Item(v = element.jsonPrimitive.content))
        }
    }

    override fun serialize(encoder: Encoder, value: List<InfoBox.Item>) {
        ListSerializer(InfoBox.Item.serializer()).serialize(encoder, value)
    }
}
