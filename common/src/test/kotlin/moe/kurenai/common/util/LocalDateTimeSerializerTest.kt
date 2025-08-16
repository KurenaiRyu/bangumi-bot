package moe.kurenai.common.util

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeSerializerTest {

    @Test
    fun test() {
        LocalDateTime.parse("2021-01-01T08:00:00Z",DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
//        println(LocalDateTime.of(1, 1, 1, 8, 0, 0).format(DateTimeFormatter.ISO_INSTANT))
//        println(Instant.parse("0001-01-01T08:00:00Z"))
    }
}
