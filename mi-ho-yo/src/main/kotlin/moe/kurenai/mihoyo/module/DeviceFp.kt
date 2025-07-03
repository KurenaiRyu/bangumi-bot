package moe.kurenai.mihoyo.module

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceFp(
    val code: Int,
    val msg: String,
    @SerialName("device_fp")
    val deviceFp: String
)
