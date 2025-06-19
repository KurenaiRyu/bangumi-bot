package moe.kurenai.mihoyo.module

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T> (
    val retcode: Int,
    val message: String,
    val data: T? = null
)
