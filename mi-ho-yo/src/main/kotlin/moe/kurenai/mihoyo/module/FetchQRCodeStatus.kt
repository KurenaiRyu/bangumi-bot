package moe.kurenai.mihoyo.module


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchQRCodeStatus(
    val payload: Payload,
    val stat: QRCodeStatus
) {

    @Serializable
    data class Payload(
        val ext: String,
        val proto: String,
        val raw: String
    )

}
