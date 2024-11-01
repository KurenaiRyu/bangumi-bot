package moe.kurenai.bot.model.bilibili


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.kurenai.bot.model.bilibili.DynamicInfo.Data.Item.Modules.ModuleDynamic.Major.Opus.TextNode

@Serializable
data class DynamicInfo(
    val code: Int,
    val `data`: Data,
    val message: String,
    val ttl: Int
) {
    @Serializable
    data class Data(
        val item: Item
    ) {
        @Serializable
        data class Item(
            val basic: Basic,
            @SerialName("id_str")
            val idStr: String,
            val modules: Modules,
            val orig: Orig? = null,
            val type: String,
            val visible: Boolean
        ) {

            @Serializable
            data class Orig(
                val basic: Basic,
                val modules: Modules,
                @SerialName("id_str")
                val idStr: String,
            )

            @Serializable
            data class Basic(
                @SerialName("comment_id_str")
                val commentIdStr: String,
                @SerialName("comment_type")
                val commentType: Int,
                @SerialName("jump_url")
                val jumpUrl: String? = null,
                @SerialName("like_icon")
                val likeIcon: LikeIcon,
                @SerialName("rid_str")
                val ridStr: String,
            ) {
                @Serializable
                data class LikeIcon(
                    @SerialName("action_url")
                    val actionUrl: String,
                    @SerialName("end_url")
                    val endUrl: String,
                    val id: Int,
                    @SerialName("start_url")
                    val startUrl: String
                )
            }

            @Serializable
            data class Modules(
                @SerialName("module_author")
                val moduleAuthor: ModuleAuthor,
                @SerialName("module_dynamic")
                val moduleDynamic: ModuleDynamic,
                @SerialName("module_more")
                val moduleMore: ModuleMore? = null,
                @SerialName("module_stat")
                val moduleStat: ModuleStat? = null
            ) {
                @Serializable
                data class ModuleAuthor(
                    val face: String,
                    @SerialName("face_nft")
                    val faceNft: Boolean,
                    @SerialName("jump_url")
                    val jumpUrl: String? = null,
                    val label: String,
                    val mid: Int,
                    val name: String,
                    @SerialName("official_verify")
                    val officialVerify: OfficialVerify,
                    val pendant: Pendant,
                    @SerialName("pub_action")
                    val pubAction: String,
                    @SerialName("pub_location_text")
                    val pubLocationText: String? = null,
                    @SerialName("pub_time")
                    val pubTime: String,
                    @SerialName("pub_ts")
                    val pubTs: Int,
                    val type: String,
                ) {

                    @Serializable
                    data class OfficialVerify(
                        val desc: String,
                        val type: Int
                    )

                    @Serializable
                    data class Pendant(
                        val expire: Int,
                        val image: String,
                        @SerialName("image_enhance")
                        val imageEnhance: String,
                        @SerialName("image_enhance_frame")
                        val imageEnhanceFrame: String,
                        @SerialName("n_pid")
                        val nPid: Int,
                        val name: String,
                        val pid: Int
                    )

                }

                @Serializable
                data class ModuleDynamic(
                    val desc: TextNode? = null,
                    val major: Major? = null,
                    val topic: String? = null,
                ) {

                    @Serializable
                    data class Major(
                        val opus: Opus,
                        val type: String
                    ) {
                        @Serializable
                        data class Opus(
                            @SerialName("fold_action")
                            val foldAction: List<String>,
                            @SerialName("jump_url")
                            val jumpUrl: String? = null,
                            val pics: List<Pic>,
                            val summary: TextNode,
                            val title: String? = null
                        ) {
                            @Serializable
                            data class Pic(
                                val height: Int,
                                val size: Double,
                                val url: String,
                                val width: Int
                            )

                            @Serializable
                            data class TextNode(
                                @SerialName("rich_text_nodes")
                                val richTextNodes: List<RichTextNode>,
                                val text: String
                            ) {
                                @Serializable
                                data class RichTextNode(
                                    @SerialName("orig_text")
                                    val origText: String,
                                    val text: String,
                                    val type: String
                                )
                            }
                        }
                    }
                }

                @Serializable
                data class ModuleMore(
                    @SerialName("three_point_items")
                    val threePointItems: List<ThreePointItem>
                ) {
                    @Serializable
                    data class ThreePointItem(
                        val label: String,
                        val type: String
                    )
                }

                @Serializable
                data class ModuleStat(
                    val comment: Comment,
                    val forward: Forward,
                    val like: Like
                ) {
                    @Serializable
                    data class Comment(
                        val count: Int,
                        val forbidden: Boolean
                    )

                    @Serializable
                    data class Forward(
                        val count: Int,
                        val forbidden: Boolean
                    )

                    @Serializable
                    data class Like(
                        val count: Int,
                        val forbidden: Boolean,
                        val status: Boolean
                    )
                }
            }
        }
    }
}
