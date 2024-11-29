package moe.kurenai.bot.model.bilibili

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    val code: Int,
    val message: String,
    val ttl: Int,
    val `data`: Data
) {
    @Serializable
    data class Data(
        val bvid: String,
        val aid: Long,
        val videos: Int,
        val tid: Int,
        val tname: String,
        val copyright: Int,
        val pic: String,
        val title: String,
        val pubdate: Int,
        val ctime: Int,
        val desc: String,
        @SerialName("desc_v2")
        val descV2: List<DescV2>?,
        val state: Int,
        val duration: Int,
        @SerialName("mission_id")
        val missionId: Long? = null,
        val rights: Rights,
        val owner: Owner,
        val stat: Stat,
        val `dynamic`: String,
        val cid: Long,
        val dimension: Dimension,
        @SerialName("teenage_mode")
        val teenageMode: Int,
        @SerialName("is_chargeable_season")
        val isChargeableSeason: Boolean,
        @SerialName("is_story")
        val isStory: Boolean,
        @SerialName("no_cache")
        val noCache: Boolean,
        val pages: List<Page>,
        val subtitle: Subtitle,
        val staff: List<Staff>? = null,
        @SerialName("is_season_display")
        val isSeasonDisplay: Boolean,
        @SerialName("user_garb")
        val userGarb: UserGarb,
        @SerialName("honor_reply")
        val honorReply: HonorReply,
        @SerialName("like_icon")
        val likeIcon: String
    ) {
        @Serializable
        data class DescV2(
            @SerialName("raw_text")
            val rawText: String,
            val type: Int,
            @SerialName("biz_id")
            val bizId: Long
        )

        @Serializable
        data class Rights(
            val bp: Int,
            val elec: Int,
            val download: Int,
            val movie: Int,
            val pay: Int,
            val hd5: Int,
            @SerialName("no_reprint")
            val noReprint: Int,
            val autoplay: Int,
            @SerialName("ugc_pay")
            val ugcPay: Int,
            @SerialName("is_cooperation")
            val isCooperation: Int,
            @SerialName("ugc_pay_preview")
            val ugcPayPreview: Int,
            @SerialName("no_background")
            val noBackground: Int,
            @SerialName("clean_mode")
            val cleanMode: Int,
            @SerialName("is_stein_gate")
            val isSteinGate: Int,
            @SerialName("is_360")
            val is360: Int,
            @SerialName("no_share")
            val noShare: Int,
            @SerialName("arc_pay")
            val arcPay: Int,
            @SerialName("free_watch")
            val freeWatch: Int
        )

        @Serializable
        data class Owner(
            val mid: Long,
            val name: String,
            val face: String
        )

        @Serializable
        data class Stat(
            val aid: Long,
            val view: Int,
            val danmaku: Int,
            val reply: Int,
            val favorite: Int,
            val coin: Int,
            val share: Int,
            @SerialName("now_rank")
            val nowRank: Int,
            @SerialName("his_rank")
            val hisRank: Int,
            val like: Int,
            val dislike: Int,
            val evaluation: String
        )

        @Serializable
        data class Dimension(
            val width: Int,
            val height: Int,
            val rotate: Int
        )

        @Serializable
        data class Page(
            val cid: Long,
            val page: Long,
            val from: String,
            val part: String,
            val duration: Int,
            val vid: String,
            val weblink: String,
            val dimension: Dimension
        ) {
            @Serializable
            data class Dimension(
                val width: Int,
                val height: Int,
                val rotate: Int
            )
        }

        @Serializable
        data class Subtitle(
            @SerialName("allow_submit")
            val allowSubmit: Boolean,
            val list: List<SubtitleInfo>? = null
        ) {
            @Serializable
            data class SubtitleInfo(
                val id: Long,
                val lan: String,
                @SerialName("lan_doc")
                val lanDoc: String,
                @SerialName("is_lock")
                val isLock: Boolean,
                @SerialName("subtitle_url")
                val subtitleUrl: String,
                val type: Int,
                @SerialName("id_str")
                val idStr: String,
                @SerialName("ai_type")
                val aiType: Int,
                @SerialName("ai_status")
                val aiStatus: Int,
                val author: Author
            ) {
                @Serializable
                data class Author(
                    val mid: Long,
                    val name: String,
                    val sex: String,
                    val face: String,
                    val sign: String,
                    val rank: Int,
                    val birthday: Int,
                    @SerialName("is_fake_account")
                    val isFakeAccount: Int,
                    @SerialName("is_deleted")
                    val isDeleted: Int,
                    @SerialName("in_reg_audit")
                    val inRegAudit: Int,
                    @SerialName("is_senior_member")
                    val isSeniorMember: Int
                )
            }
        }

        @Serializable
        data class Staff(
            val mid: Long,
            val title: String,
            val name: String,
            val face: String,
            val vip: Vip,
            val official: Official,
            val follower: Int,
            @SerialName("label_style")
            val labelStyle: Int
        ) {
            @Serializable
            data class Vip(
                val type: Int,
                val status: Int,
                @SerialName("due_date")
                val dueDate: Long,
                @SerialName("vip_pay_type")
                val vipPayType: Int,
                @SerialName("theme_type")
                val themeType: Int,
                val label: Label,
                @SerialName("avatar_subscript")
                val avatarSubscript: Int,
                @SerialName("nickname_color")
                val nicknameColor: String,
                val role: Int,
                @SerialName("avatar_subscript_url")
                val avatarSubscriptUrl: String,
                @SerialName("tv_vip_status")
                val tvVipStatus: Int,
                @SerialName("tv_vip_pay_type")
                val tvVipPayType: Int
            ) {
                @Serializable
                data class Label(
                    val path: String,
                    val text: String,
                    @SerialName("label_theme")
                    val labelTheme: String,
                    @SerialName("text_color")
                    val textColor: String,
                    @SerialName("bg_style")
                    val bgStyle: Int,
                    @SerialName("bg_color")
                    val bgColor: String,
                    @SerialName("border_color")
                    val borderColor: String,
                    @SerialName("use_img_label")
                    val useImgLabel: Boolean,
                    @SerialName("img_label_uri_hans")
                    val imgLabelUriHans: String,
                    @SerialName("img_label_uri_hant")
                    val imgLabelUriHant: String,
                    @SerialName("img_label_uri_hans_static")
                    val imgLabelUriHansStatic: String,
                    @SerialName("img_label_uri_hant_static")
                    val imgLabelUriHantStatic: String
                )
            }

            @Serializable
            data class Official(
                val role: Int,
                val title: String,
                val desc: String,
                val type: Int
            )
        }

        @Serializable
        data class UserGarb(
            @SerialName("url_image_ani_cut")
            val urlImageAniCut: String
        )

        @Serializable
        data class HonorReply(
            val honor: List<Honor>? = null
        ) {
            @Serializable
            data class Honor(
                val aid: Long,
                val type: Int,
                val desc: String,
                @SerialName("weekly_recommend_num")
                val weeklyRecommendNum: Int
            )
        }
    }
}
