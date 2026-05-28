package moe.kurenai.skyland.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
@OptIn(ExperimentalSerializationApi::class)
class SkylandResponse<T: SkylandData>(
    @JsonNames("code", "status")
    val code: Int,
    @JsonNames("message", "msg")
    val message: String?,
    val requestId: String?,
    @JsonNames("data", "detail")
    val data: T?
)

sealed interface SkylandData

@Serializable
data class Grant(
    val code: String
): SkylandData

@Serializable
data class CredInfo(
    val token: String,
    val cred: String,
): SkylandData

@Serializable
data class BindingList(
    val list: List<Binding>
): SkylandData {
    @Serializable
    data class Binding(
        val appCode: String,
        val bindingList: List<String>
    )
}

@Serializable
data class DidInfo(
    val deviceId: String
): SkylandData

