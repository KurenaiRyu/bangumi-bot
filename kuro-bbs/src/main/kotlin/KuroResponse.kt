import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class KuroResponse<T: KuroResponseData> (
    @SerialName("code")
    val code: Int,
    @SerialName("msg")
    val msg: String,
    @SerialName("success")
    val success: Boolean? = null,
    @SerialName("traceId")
    val traceId: String,
    @SerialName("data")
    val data: T? = null
)

sealed interface KuroResponseData

@Serializable
class DefaultKuroResponseData: KuroResponseData

@Serializable
data class MineInfo(
    val mine: Mine,
): KuroResponseData {
    @Serializable
    data class Mine(
        val userId: Int,
    )
}

@Serializable
data class GamerRoleList(
    val defaultRoleList: List<RoleInfo>,
): KuroResponseData {
    @Serializable
    data class RoleInfo(
        val gameId: Int,
        val serverId: String,
        val roleId: Int,
        val userId: Int,
    )
}
