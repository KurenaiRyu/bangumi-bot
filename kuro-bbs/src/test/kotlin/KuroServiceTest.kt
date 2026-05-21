import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class KuroServiceTest {

    @Test
    fun signTest() {
        val svc = KuroService(KuroClient())
        with(KuroContext("eyJhbGciOiJIUzI1NiJ9.eyJjcmVhdGVkIjoxNzMyMTI1MzAzMTkwLCJ1c2VySWQiOjE4NzYzNzg1fQ.txhTWC6RUNTYMQbabUoN4tI0lypiJy3SCVjJ1Bw9kiE")) {
            runBlocking {
                svc.checkIn()
            }
        }
    }
}
