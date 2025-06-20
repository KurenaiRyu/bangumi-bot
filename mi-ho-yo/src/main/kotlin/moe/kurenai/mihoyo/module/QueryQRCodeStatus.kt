package moe.kurenai.mihoyo.module

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class QueryQRCodeStatus(
    @SerialName("app_id")
    val appId: String,
    @SerialName("client_type")
    val clientType: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("need_realperson")
    val needRealPerson: Boolean,
    @SerialName("realname_info")
    val realNameInfo: RealNameInfo? = null,
    @SerialName("scanned_at")
    val scannedAt: String,
    val status: QRCodeStatus,
    val tokens: List<String>,
    @SerialName("user_info")
    val userInfo: UserInfo? = null
) {
    var cookie: String = ""

    @Serializable
    data class RealNameInfo(
        @SerialName("action_ticket")
        val actionTicket: String,
        @SerialName("action_type")
        val actionType: String,
        val required: Boolean
    )

    @Serializable
    data class UserInfo(
        @SerialName("account_name")
        val accountName: String,
        val aid: String,
        @SerialName("area_code")
        val areaCode: String,
        val country: String,
        val email: String,
        @SerialName("identity_code")
        val identityCode: String,
        @SerialName("is_adult")
        val isAdult: Int,
        @SerialName("is_email_verify")
        val isEmailVerify: Int,
        val links: List<String>,
        val mid: String,
        val mobile: String,
        @SerialName("password_time")
        val passwordTime: String,
        val realname: String,
        @SerialName("rebind_area_code")
        val rebindAreaCode: String,
        @SerialName("rebind_mobile")
        val rebindMobile: String,
        @SerialName("rebind_mobile_time")
        val rebindMobileTime: String,
        @SerialName("safe_area_code")
        val safeAreaCode: String,
        @SerialName("safe_mobile")
        val safeMobile: String,
        @SerialName("unmasked_email")
        val unmaskedEmail: String,
        @SerialName("unmasked_email_type")
        val unmaskedEmailType: Int
    )
}
