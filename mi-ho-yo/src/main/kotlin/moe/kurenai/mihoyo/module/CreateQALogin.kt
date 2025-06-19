package moe.kurenai.mihoyo.module

import kotlinx.serialization.Serializable

@Serializable
data class CreateQALogin(
    val ticket: String,
    val url: String
)
