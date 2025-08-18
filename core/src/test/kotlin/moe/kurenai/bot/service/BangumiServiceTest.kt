package moe.kurenai.bot.service

import kotlinx.coroutines.runBlocking
import moe.kurenai.bot.service.bangumi.CharacterService
import moe.kurenai.bot.service.bangumi.SubjectService
import org.junit.Test

class BangumiServiceTest {

    @Test
    fun testSubject() = runBlocking { with(null) {
        val content = SubjectService.getContent(SubjectService.findById(25952), "test")
        for (result in content) {
            println(result)
        }
    } }

    @Test
    fun testCharacter() = runBlocking { with(null) {
        val content = CharacterService.getContent(CharacterService.findById(178552), "test", CharacterService.findPersons(178552))
        for (result in content) {
            println(result)
        }
    } }

}
