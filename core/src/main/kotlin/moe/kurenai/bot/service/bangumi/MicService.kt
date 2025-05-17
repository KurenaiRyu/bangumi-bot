package moe.kurenai.bot.service.bangumi

import moe.kurenai.bangumi.models.GetCalendar200ResponseInner
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi

internal object MicService {

    suspend fun getCalendar(userId: Long): List<GetCalendar200ResponseInner> {
        val token = TokenService.findById(userId)?.accessToken
        return useApi(token) {
            it.getCalendar().result()
        }
    }

}
