package moe.kurenai.skyland.moe.kurenai.skyland.model

data class DidResponse(
    val code: Int,
    val detail: DidDetail,
) {

    data class DidDetail(
        val deviceId: String
    )
}
