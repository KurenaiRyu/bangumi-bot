package moe.kurenai.kuro

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class KuroServiceTest {

    @Test
    fun signTest() {
        val svc = KuroService(KuroClient())
        with(
            KuroContext(
                1,
                "1"
            )
        ) {
            runBlocking {
                svc.checkIn()
            }
        }
    }
}
