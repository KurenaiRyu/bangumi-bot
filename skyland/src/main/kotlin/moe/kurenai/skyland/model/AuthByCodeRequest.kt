package moe.kurenai.skyland.moe.kurenai.skyland.model

data class AuthByCodeRequest(
    val code: String,
    val kind: Int = 1
) {



}
