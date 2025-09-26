package moe.kurenai.bot

import moe.kurenai.bangumi.models.PersonDetail
import moe.kurenai.common.util.json
import kotlin.test.Test


class BangumiDeserializeTest {

    @Test
    fun name() {
        val personDetail = json.decodeFromString<PersonDetail>(this::class.java.getResource("/bangumi/person-detail.json").readText())
        println(personDetail)
    }
}
