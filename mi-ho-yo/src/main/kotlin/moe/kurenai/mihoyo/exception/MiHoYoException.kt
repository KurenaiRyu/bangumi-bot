package moe.kurenai.mihoyo.exception

class MiHoYoException(
    val code: Int,
    message: String,
    val throwable: Throwable? = null
): Exception("[$code] $message", throwable) {
}
