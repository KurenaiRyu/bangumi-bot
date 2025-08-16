package moe.kurenai.common.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeSerializer(val formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME) : KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val input = decoder as? JsonDecoder
            ?: throw IllegalStateException(
                "This serializer can be used only with Json format." +
                    "Expected Decoder to be JsonDecoder, got ${this::class}"
            )
        val element = input.decodeJsonElement()
        val content = element.jsonPrimitive.content
        return LocalDateTime.parse(content, formatter)
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        value.format(formatter)
    }
}
