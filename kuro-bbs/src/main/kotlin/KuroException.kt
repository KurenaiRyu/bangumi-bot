import java.io.Serial

class KuroException(
    val code: Int,
    val msg: String,
    val case: Throwable? = null
) : Exception("[$code] $msg", case) {

    companion object {
        @Serial
        val serialVersionUID = 1L
    }

}
