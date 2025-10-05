package moe.kurenai.bot.model.bilibili


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoStreamUrl(
    val code: Int,
    val message: String,
    val ttl: Int,
    val `data`: Data? = null
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
        val durl: List<Durl> = emptyList(),
        @SerialName("support_formats")
        val supportFormats: List<SupportFormat>,
        @SerialName("last_play_time")
        val lastPlayTime: Long,
        @SerialName("last_play_cid")
        val lastPlayCid: Long,
        val dash: Dash? = null
    ) {
        @Serializable
        data class Durl(
            val order: Long,
            val length: Long,
            val size: Long,
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
            val codecs: List<String>? = null
        )

        @Serializable
        data class Dash(
            val audio: List<Audio>,
            val duration: Long,
            val minBufferTime: Double,
            val video: List<Video>
        ) {
            @Serializable
            data class Audio(
                val backupUrl: List<String>,
                val bandwidth: Int,
                val baseUrl: String,
                val codecid: Long,
                val codecs: String,
                val frameRate: String,
                val height: Int,
                val id: Long,
                val mimeType: String,
                val sar: String,
                @SerialName("segment_base")
                val segmentBase: SegmentBase,
                val startWithSap: Int,
                val width: Int
            ) {

                @Serializable
                data class SegmentBase(
                    @SerialName("index_range")
                    val indexRange: String,
                    val initialization: String
                )
            }

            @Serializable
            data class Video(
                val backupUrl: List<String>,
                val bandwidth: Int,
                val baseUrl: String,
                val codecid: Long,
                val codecs: String,
                val frameRate: String,
                val height: Int,
                val id: Long,
                val mimeType: String,
                val sar: String,
                @SerialName("segment_base")
                val segmentBase: SegmentBase,
                val startWithSap: Int,
                val width: Int
            ) {

                @Serializable
                data class SegmentBase(
                    @SerialName("index_range")
                    val indexRange: String,
                    val initialization: String
                )
            }
        }
    }
}
