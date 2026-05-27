package moe.kurenai.skyland.moe.kurenai.skyland

class SkylandService(
    private val client: SkylandClient,
) {

    context(ctx: SkylandContext)
    private suspend fun doSign() {
        val grantCode = client.grantCode()
        ctx.credInfo = client.authByCode(grantCode)
        client.getBindingList()
    }

}
