package moe.kurenai.mihoyo

import moe.kurenai.mihoyo.module.BindInfoList

class AccountContext(
    bindList: List<BindInfoList.BindInfo>
) {

    init {
        for (bindInfo in bindList) {
            val gameBiz = bindInfo.gameBiz
            if (gameBiz.startsWith("nap")) {
                zzzInfo = bindInfo
            }
        }
    }

    var zzzInfo: BindInfoList.BindInfo = BindInfoList.BindInfo()
    var cookie: String = ""

}
