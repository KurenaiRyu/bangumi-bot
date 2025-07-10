package moe.kurenai.mihoyo

import moe.kurenai.mihoyo.module.BindInfoList

class AccountContext(
    bindList: List<BindInfoList.BindInfo>,
    var cookie: String
) {

    var zzzInfo: BindInfoList.BindInfo = BindInfoList.BindInfo()

    init {
        for (bindInfo in bindList) {
            val gameBiz = when (bindInfo.gameBiz) {
                "nap_cn" -> {zzzInfo = bindInfo}
                else -> {}
            }
        }
    }

}
