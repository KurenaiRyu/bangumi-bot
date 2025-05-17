package moe.kurenai.bot.service.bangumi

import moe.kurenai.bangumi.models.*
import moe.kurenai.bot.service.bangumi.BangumiApi.result
import moe.kurenai.bot.service.bangumi.BangumiApi.useApi

internal object UserService {

    suspend fun getMe(userId: Long): User {
        val token = TokenService.findById(userId)?.accessToken ?: throw IllegalStateException("Token is null")
        return getMe(token)
    }

    suspend fun getMe(token: String): User {
        return useApi(token) {
            it.getMyself().result()
        }
    }

    suspend fun getCollections(
        userId: Long,
        subjectType: SubjectType? = null,
        type: SubjectCollectionType? = null,
        limit: Int? = null,
        offset: Int? = null
    ): PagedUserCollection {
        val token = TokenService.findById(userId)?.accessToken ?: throw IllegalStateException("Token is null")
        return getCollections(token, subjectType, type, limit, offset)
    }

    suspend fun getCollections(
        token: String,
        subjectType: SubjectType? = null,
        type: SubjectCollectionType? = null,
        limit: Int? = null,
        offset: Int? = null
    ): PagedUserCollection {
        return useApi(token) {
            val me = it.getMyself().result<GetMyself200Response>()
            it.getUserCollectionsByUsername(me.username, subjectType, type, limit, offset).result()
        }
    }

}
