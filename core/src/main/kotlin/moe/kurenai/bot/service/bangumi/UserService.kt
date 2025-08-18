package moe.kurenai.bot.service.bangumi

import moe.kurenai.bangumi.models.*
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi

internal object UserService {

    context(token: UserAccessToken)
    suspend fun getMe(): User {
        return useApi {
            it.getMyself().result()
        }
    }

    context(token: UserAccessToken)
    suspend fun getCollections(
        subjectType: SubjectType? = null,
        type: SubjectCollectionType? = null,
        limit: Int? = null,
        offset: Int? = null
    ): PagedUserCollection {
        return useApi {
            val me = it.getMyself().result<GetMyself200Response>()
            it.getUserCollectionsByUsername(me.username, subjectType, type, limit, offset).result()
        }
    }

}
