package moe.kurenai.mihoyo.module

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class QRCodeStatus {
    @SerialName("Created") CREATED,
    @SerialName("Scanned") SCANNED,
    @SerialName("Confirmed") CONFIRMED,
    @SerialName("Init") INIT,
}
