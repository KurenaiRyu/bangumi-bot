class KuroService(
    private val client: KuroClient
) {

    context(ctx: KuroContext)
    suspend fun checkIn() {
        val mineInfo = client.getMineInfo()
        val gamerRoleList = client.getGamerRoleList(mineInfo.mine.userId)
        val roleInfo = gamerRoleList.defaultRoleList[0]
        client.sign(roleInfo)
    }
}
