package moe.kurenai.skyland.moe.kurenai.skyland.model

class SkylandResponse<T: SkylandData>(
    val status: Int,
    val msg: String?,
    val data: T?
)

sealed interface SkylandData

data class Grant(
    val code: String
): SkylandData

data class CredInfo(
    val token: String,
    val cred: String,
): SkylandData

