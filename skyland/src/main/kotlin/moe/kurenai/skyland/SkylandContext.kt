package moe.kurenai.skyland

import moe.kurenai.skyland.model.CredInfo

class SkylandContext(val token: String) {
    lateinit var credInfo: CredInfo
}
