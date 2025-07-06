package moe.kurenai.mihoyo.module.common

import kotlinx.serialization.Serializable

@Serializable
data class DateTime(
    val day: Int = 0,
    val hour: Int = 0,
    val minute: Int = 0,
    val month: Int = 0,
    val second: Int = 0,
    val year: Int = 0
)
