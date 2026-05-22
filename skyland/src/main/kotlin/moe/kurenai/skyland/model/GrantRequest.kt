package moe.kurenai.skyland.moe.kurenai.skyland.model

import moe.kurenai.skyland.moe.kurenai.skyland.SkylandClient

data class GrantRequest(
    val appCode: String = SkylandClient.APP_CODE,
    val token: String,
    val type: Int = 0,
) {
}
