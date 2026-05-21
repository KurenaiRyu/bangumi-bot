package moe.kurenai.kuro

class KuroService(
    private val client: KuroClient
) {

    context(ctx: KuroContext)
    suspend fun checkIn() {
        val mineInfo = client.getMineInfo(ctx.userId)
        val gamerRoleList = client.getGamerRoleList(mineInfo.mine.userId)
        val roleInfo = gamerRoleList.defaultRoleList[0]
        client.sign(roleInfo)
    }
}
