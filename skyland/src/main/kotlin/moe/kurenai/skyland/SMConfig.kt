package moe.kurenai.skyland

import kotlinx.serialization.Serializable

/**
 * 数美配置
 */
@Serializable
data class SMConfig(
    val organization: String = "UWXspnCCJN4sfYlNfqps",
    val appId: String = "default",
    val publicKey: String =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCmxMNr7n8ZeT0tE1R9j/mPixoinPkeM+k4VGIn/s0k7N5rJAfnZ0eMER+QhwFvshzo0LNmeUkpR8uIlU/GEVr8mN28sKmwd2gpygqj0ePnBmOW4v0ZVwbSYK+izkhVFk2V/doLoMbWy6b+UnA8mkjvg0iYWRByfRsK2gdl7llqCwIDAQAB",
    val apiHost: String = "fp-it.portal101.cn",
) {


}
