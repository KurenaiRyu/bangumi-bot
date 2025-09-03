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
                @SerialName("rid_str")
                val ridStr: String,
            )

            @Serializable
            data class Modules(
                @SerialName("module_author")
                val moduleAuthor: ModuleAuthor,
                @SerialName("module_dynamic")
                val moduleDynamic: ModuleDynamic,
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
                        val expire: Long,
                        val image: String,
                        @SerialName("image_enhance")
                        val imageEnhance: String,
                        @SerialName("image_enhance_frame")
                        val imageEnhanceFrame: String,
                        @SerialName("n_pid")
                        val nPid: Long,
                        val name: String,
                        val pid: Long
                    )

                }

                @Serializable
                data class ModuleDynamic(
                    val desc: TextNode? = null,
                    val major: Major? = null,
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
            }
        }
    }
}
