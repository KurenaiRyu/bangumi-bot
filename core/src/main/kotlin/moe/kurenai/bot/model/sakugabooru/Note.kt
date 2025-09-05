package moe.kurenai.bot.model.sakugabooru


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val body: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("creator_id")
    val creatorId: Int = 0,
    val height: Int = 0,
    val id: Int = 0,
    @SerialName("is_active")
    val isActive: Boolean = false,
    @SerialName("post_id")
    val postId: Int = 0,
    @SerialName("updated_at")
    val updatedAt: String = "",
    val version: Int = 0,
    val width: Int = 0,
    val x: Int = 0,
    val y: Int = 0
)
