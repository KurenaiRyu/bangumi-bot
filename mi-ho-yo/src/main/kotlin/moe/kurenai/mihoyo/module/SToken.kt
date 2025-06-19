package moe.kurenai.mihoyo.module


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SToken(
    @SerialName("need_realperson")
    val needRealperson: Boolean = false,
    @SerialName("realname_info")
    val realnameInfo: JsonElement? = null,
    val token: Token = Token(),
    @SerialName("user_info")
    val userInfo: UserInfo = UserInfo()
) {
    @Serializable
    data class Token(
        val token: String = "",
        @SerialName("token_type")
        val tokenType: Int = 0
    )

    @Serializable
    data class UserInfo(
        @SerialName("account_name")
        val accountName: String = "",
        val aid: String = "",
        @SerialName("area_code")
        val areaCode: String = "",
        val email: String = "",
        @SerialName("identity_code")
        val identityCode: String = "",
        @SerialName("is_email_verify")
        val isEmailVerify: Int = 0,
        val links: List<String> = listOf(),
        val mid: String = "",
        val mobile: String = "",
        val realname: String = "",
        @SerialName("rebind_area_code")
        val rebindAreaCode: String = "",
        @SerialName("rebind_mobile")
        val rebindMobile: String = "",
        @SerialName("rebind_mobile_time")
        val rebindMobileTime: String = "",
        @SerialName("safe_area_code")
        val safeAreaCode: String = "",
        @SerialName("safe_mobile")
        val safeMobile: String = ""
    )
}
