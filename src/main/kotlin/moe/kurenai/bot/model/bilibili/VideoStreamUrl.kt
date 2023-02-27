package moe.kurenai.bot.model.bilibili


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoStreamUrl(
    val code: Int,
    val message: String,
    val ttl: Int,
    val `data`: Data
) {
    @Serializable
    data class Data(
        val from: String,
        val result: String,
        val message: String,
        val quality: Int,
        val format: String,
        val timelength: Int,
        @SerialName("accept_format")
        val acceptFormat: String,
        @SerialName("accept_description")
        val acceptDescription: List<String>,
        @SerialName("accept_quality")
        val acceptQuality: List<Int>,
        @SerialName("video_codecid")
        val videoCodecid: Int,
        @SerialName("seek_param")
        val seekParam: String,
        @SerialName("seek_type")
        val seekType: String,
        val durl: List<Durl>,
        @SerialName("support_formats")
        val supportFormats: List<SupportFormat>,
        @SerialName("last_play_time")
        val lastPlayTime: Int,
        @SerialName("last_play_cid")
        val lastPlayCid: Int,
    ) {
        @Serializable
        data class Durl(
            val order: Int,
            val length: Int,
            val size: Int,
            val ahead: String,
            val vhead: String,
            val url: String,
            @SerialName("backup_url")
            val backupUrl: List<String>? = null
        )

        @Serializable
        data class SupportFormat(
            val quality: Int,
            val format: String,
            @SerialName("new_description")
            val newDescription: String,
            @SerialName("display_desc")
            val displayDesc: String,
            val superscript: String,
            val codecs: String? = null
        )
    }
}
