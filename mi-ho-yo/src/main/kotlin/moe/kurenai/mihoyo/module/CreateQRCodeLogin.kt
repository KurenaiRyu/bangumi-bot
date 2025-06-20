package moe.kurenai.mihoyo.module

import kotlinx.serialization.Serializable

@Serializable
data class CreateQRCodeLogin(
    val ticket: String,
    val url: String
)
