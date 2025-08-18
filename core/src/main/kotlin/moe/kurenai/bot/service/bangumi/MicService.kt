package moe.kurenai.bot.service.bangumi

import moe.kurenai.bangumi.models.GetCalendar200ResponseInner
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi

internal object MicService {

    context(token: UserAccessToken?)
    suspend fun getCalendar(): List<GetCalendar200ResponseInner> {
        return useApi {
            it.getCalendar().result()
        }
    }

}
