package moe.kurenai.bot.service.bangumi

import moe.kurenai.bangumi.models.AccessTokenRequest
import moe.kurenai.bangumi.models.UserAccessToken
import moe.kurenai.bot.Config.Companion.CONFIG
import moe.kurenai.bot.service.bangumi.BangumiApi.oauthApi
import moe.kurenai.bot.service.bangumi.BangumiApi.result

internal object OauthService {

    suspend fun grantToken(code: String, state: String? = null): UserAccessToken {
        return oauthApi.getAccessToken(
            AccessTokenRequest(
                code = code,
                grantType = AccessTokenRequest.GrantType.AUTHORIZATION_CODE,
                clientId = CONFIG.bgm.appId,
                clientSecret = CONFIG.bgm.appSecret,
                redirectUri = CONFIG.bgm.redirectUrl,
                state = state
            )
        ).result()
    }

    suspend fun refreshToken(token: UserAccessToken): UserAccessToken {
        return oauthApi.getAccessToken(
            AccessTokenRequest(
                grantType = AccessTokenRequest.GrantType.REFRESH_TOKEN,
                clientId = CONFIG.bgm.appId,
                clientSecret = CONFIG.bgm.appSecret,
                redirectUri = CONFIG.bgm.redirectUrl,
                refreshToken = token.refreshToken
            )
        ).result()
    }


}
