package moe.kurenai.bot.model.sakugabooru


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    @SerialName("actual_preview_height")
    val actualPreviewHeight: Int = 0,
    @SerialName("actual_preview_width")
    val actualPreviewWidth: Int = 0,
    @SerialName("approver_id")
    val approverId: Long? = null,
    val author: String = "",
    val change: Int = 0,
    @SerialName("created_at")
    val createdAt: Long = 0,
    @SerialName("creator_id")
    val creatorId: Long = 0,
    @SerialName("file_ext")
    val fileExt: String = "",
    @SerialName("file_size")
    val fileSize: Long = 0,
    @SerialName("file_url")
    val fileUrl: String = "",
//    val frames: List<Any?> = listOf(),
//    @SerialName("frames_pending")
//    val framesPending: List<Any?> = listOf(),
    @SerialName("frames_pending_string")
    val framesPendingString: String = "",
    @SerialName("frames_string")
    val framesString: String = "",
    @SerialName("has_children")
    val hasChildren: Boolean = false,
    val height: Int = 0,
    val id: Int = 0,
    @SerialName("is_held")
    val isHeld: Boolean = false,
    @SerialName("is_note_locked")
    val isNoteLocked: Boolean = false,
    @SerialName("is_pending")
    val isPending: Boolean = false,
    @SerialName("is_rating_locked")
    val isRatingLocked: Boolean = false,
    @SerialName("is_shown_in_index")
    val isShownInIndex: Boolean = false,
    @SerialName("jpeg_file_size")
    val jpegFileSize: Int = 0,
    @SerialName("jpeg_height")
    val jpegHeight: Int = 0,
    @SerialName("jpeg_url")
    val jpegUrl: String = "",
    @SerialName("jpeg_width")
    val jpegWidth: Int = 0,
    @SerialName("last_commented_at")
    val lastCommentedAt: Int = 0,
    @SerialName("last_noted_at")
    val lastNotedAt: Int = 0,
    val md5: String = "",
    @SerialName("parent_id")
    val parentId: Long? = null,
    @SerialName("preview_height")
    val previewHeight: Int = 0,
    @SerialName("preview_url")
    val previewUrl: String = "",
    @SerialName("preview_width")
    val previewWidth: Int = 0,
    val rating: String = "",
    @SerialName("sample_file_size")
    val sampleFileSize: Int = 0,
    @SerialName("sample_height")
    val sampleHeight: Int = 0,
    @SerialName("sample_url")
    val sampleUrl: String = "",
    @SerialName("sample_width")
    val sampleWidth: Int = 0,
    val score: Int = 0,
    val source: String = "",
    val status: String = "",
    val tags: String = "",
    @SerialName("updated_at")
    val updatedAt: Long = 0,
    val width: Int = 0
)
