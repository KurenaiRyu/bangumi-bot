package moe.kurenai.skyland

import moe.kurenai.common.util.getLogger
import kotlin.collections.joinToString

class SkylandService(
    private val client: SkylandClient,
) {
    private val log = getLogger()

    context(ctx: SkylandContext)
    suspend fun doSign() {
        val grantCode = client.grantCode()
        log.info("Grant code: $grantCode")
        ctx.credInfo = client.authByCode(grantCode)
        log.info("Cred info: ${ctx.credInfo}")
        val bindingList = client.getBindingList()
        log.info("Binding list: ${bindingList.joinToString(", ")}")
    }

}
