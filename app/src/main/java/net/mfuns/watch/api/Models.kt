package net.mfuns.watch.api

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object ResourceType {
    const val ARTICLE = 0
    const val VIDEO = 1
    const val FEED = 3
    const val COMMENT = 4
}

data class ContentItem(
    val id: Int,
    val title: String,
    val summary: String,
    val cover: String,
    val author: String,
    val authorId: Int?,
    val authorAvatar: String,
    val category: String,
    val type: Int,
    val likes: Int,
    val comments: Int,
    val views: Int,
    val createdAt: Long?,
) {
    val isVideo: Boolean get() = type == ResourceType.VIDEO
    val typeLabel: String
        get() = when (type) {
            ResourceType.VIDEO -> "视频"
            ResourceType.FEED -> "动态"
            else -> "文章"
        }
}

data class UserItem(
    val id: Int,
    val name: String,
    val avatar: String,
    val bio: String = "",
    val level: Int? = null,
    val fans: Int = 0,
    val follows: Int = 0,
)

data class CommentItem(
    val id: Int,
    val userName: String,
    val avatar: String,
    val content: String,
    val likes: Int,
    val liked: Boolean,
    val replyCount: Int,
    val createdAt: Long?,
)

data class MessageItem(
    val userId: Int,
    val userName: String,
    val avatar: String,
    val lastMessage: String,
    val unread: Int,
    val lastTime: Long?,
)

data class MessageRecord(
    val uid: Int,
    val mine: Boolean,
    val message: String,
    val time: Long?,
)

data class NotifyItem(
    val type: Int,
    val senderName: String,
    val avatar: String,
    val text: String,
    val createdAt: Long?,
    val resourceId: Int?,
    val resourceType: Int?,
)

data class CategoryItem(
    val id: Int,
    val name: String,
    val parentId: Int?,
)

data class FavoriteFolderItem(
    val id: Int,
    val name: String,
    val description: String,
    val count: Int,
)

data class ContentDetail(
    val item: ContentItem,
    val body: String,
    val tags: List<String>,
    val commentAreaId: Int?,
)

object Parser {

    fun contentItem(raw: JsonElement): ContentItem? {
        val obj = raw.asObj()
        val resourceInfo = obj["resource_info"]?.asObj() ?: JsonObject(emptyMap())
        val extra = obj["extra"]?.asObj() ?: JsonObject(emptyMap())
        val referenced = extra["resource"]?.asObj() ?: JsonObject(emptyMap())
        val autoSync = obj["is_auto_sync"]?.asBool() ?: false
        val feedResourceId = obj["resource_id"]?.asInt()
        val feedResourceType = obj["resource_type"]?.asInt()

        if (referenced.isNotEmpty() || (autoSync && feedResourceId != null && feedResourceId > 0)) {
            val source = if (referenced.isNotEmpty()) referenced else obj
            val user = (source["user"]?.asObj()?.takeIf { it.isNotEmpty() }
                ?: obj["user"]?.asObj() ?: JsonObject(emptyMap()))
            val category = source["category"]?.asObj() ?: JsonObject(emptyMap())
            val id = source["id"]?.asInt() ?: feedResourceId ?: return null
            if (id == 0) return null
            return ContentItem(
                id = id,
                title = source["title"]?.asString()?.ifEmpty { "未命名内容" } ?: "未命名内容",
                summary = plainText(
                    source["summary"]?.asString() ?: source["content"]?.asString() ?: "",
                ),
                cover = coverUrl(source["cover"]?.asString() ?: ""),
                author = user["name"]?.asString() ?: user["username"]?.asString()
                    ?: obj["user_name"]?.asString() ?: "",
                authorId = user["id"]?.asInt() ?: user["user_id"]?.asInt(),
                authorAvatar = coverUrl(user["avatar"]?.asString() ?: user["face"]?.asString() ?: ""),
                category = category["name"]?.asString() ?: "",
                type = source["type"]?.asInt() ?: feedResourceType ?: ResourceType.ARTICLE,
                likes = source["like_count"]?.asInt() ?: 0,
                comments = source["comment_count"]?.asInt() ?: 0,
                views = source["view_count"]?.asInt() ?: 0,
                createdAt = parseTime(source["created_at"] ?: obj["created_at"]),
            )
        }

        val source = if (resourceInfo.isNotEmpty()) obj + resourceInfo else obj
        val user = source["user"]?.asObj() ?: JsonObject(emptyMap())
        val category = source["category"]?.asObj() ?: JsonObject(emptyMap())
        val id = source["id"]?.asInt() ?: source["resource_id"]?.asInt() ?: return null
        if (id == 0) return null
        val isFeedEntry = obj["resource_type"]?.asInt() == ResourceType.FEED ||
            obj["extra_type"] != null
        val type = when {
            isFeedEntry -> ResourceType.FEED
            else -> resourceType(
                (source["type"]?.asString() ?: source["resource_type"]?.asString()).orEmpty(),
            )
        }
        val feedCover = feedCoverUrl(extra["images"])
        return ContentItem(
            id = id,
            title = source["title"]?.asString()?.ifEmpty { "未命名内容" } ?: "未命名内容",
            summary = plainText(
                source["summary"]?.asString() ?: source["content"]?.asString() ?: "",
            ),
            cover = coverUrl(source["cover"]?.asString() ?: "").ifEmpty { feedCover },
            author = user["name"]?.asString() ?: user["username"]?.asString()
                ?: source["user_name"]?.asString() ?: "",
            authorId = user["id"]?.asInt() ?: user["user_id"]?.asInt() ?: source["user_id"]?.asInt(),
            authorAvatar = coverUrl(user["avatar"]?.asString() ?: user["face"]?.asString() ?: ""),
            category = category["name"]?.asString() ?: source["category_name"]?.asString()
                ?: source["tag"]?.asString() ?: "",
            type = type,
            likes = source["like_count"]?.asInt() ?: 0,
            comments = source["comment_count"]?.asInt() ?: 0,
            views = source["view_count"]?.asInt() ?: 0,
            createdAt = parseTime(source["created_at"] ?: source["time"]),
        )
    }

    private fun feedCoverUrl(raw: JsonElement?): String {
        val first = raw.asList().firstOrNull() ?: return ""
        val url = first.asObj()["url"]?.asString() ?: first.asString()
        return coverUrl(url)
    }

    fun contentList(data: JsonElement?): List<ContentItem> {
        val rawList = data.asList()
            .ifEmpty { data.asObj()["list"].asList() }
            .ifEmpty { data.asObj()["data"]?.asObj()?.get("list").asList() }
        return rawList.mapNotNull { contentItem(it) }
    }

    fun userItem(raw: JsonElement): UserItem? {
        val obj = raw.asObj()
        val source = obj["user"]?.asObj()?.takeIf { it.isNotEmpty() } ?: obj
        val id = source["id"]?.asInt() ?: source["user_id"]?.asInt() ?: return null
        if (id == 0) return null
        val info = source["info"]?.asObj() ?: JsonObject(emptyMap())
        return UserItem(
            id = id,
            name = source["name"]?.asString() ?: source["username"]?.asString() ?: "Mfuns 用户",
            avatar = coverUrl(source["avatar"]?.asString() ?: source["face"]?.asString() ?: ""),
            bio = plainText(source["bio"]?.asString() ?: source["signature"]?.asString() ?: ""),
            level = source["level_id"]?.asInt() ?: info["level_id"]?.asInt(),
            fans = source["fans"]?.asInt() ?: info["fans"]?.asInt() ?: 0,
            follows = source["follows"]?.asInt() ?: info["follows"]?.asInt() ?: 0,
        )
    }

    fun userList(data: JsonElement?): List<UserItem> {
        val rawList = data.asList().ifEmpty { data.asObj()["list"].asList() }
        return rawList.mapNotNull { userItem(it) }
    }

    fun commentItem(raw: JsonElement): CommentItem? {
        val obj = raw.asObj()
        val user = obj["user"]?.asObj()?.takeIf { it.isNotEmpty() }
            ?: obj["user_info"]?.asObj() ?: JsonObject(emptyMap())
        val id = obj["id"]?.asInt() ?: return null
        if (id == 0) return null
        val like = obj["like_status"]?.asObj()?.get("like")?.asObj() ?: JsonObject(emptyMap())
        return CommentItem(
            id = id,
            userName = user["name"]?.asString() ?: user["username"]?.asString()
                ?: user["nickname"]?.asString() ?: obj["user_name"]?.asString() ?: "",
            avatar = coverUrl(user["avatar"]?.asString() ?: user["face"]?.asString() ?: ""),
            content = plainText(obj["content"]?.asString() ?: ""),
            likes = like["count"]?.asInt() ?: obj["like_count"]?.asInt() ?: 0,
            liked = like["is_active"]?.asBool() ?: false,
            replyCount = obj["reply_count"]?.asInt() ?: 0,
            createdAt = parseTime(obj["created_at"]),
        )
    }

    fun commentList(data: JsonElement?): List<CommentItem> {
        val list = data.asList()
        return if (list.isEmpty()) {
            data.asObj()["list"].asList().mapNotNull { commentItem(it) }
        } else {
            list.mapNotNull { commentItem(it) }
        }
    }

    fun messageItem(raw: JsonElement): MessageItem? {
        val obj = raw.asObj()
        val user = obj["user"]?.asObj() ?: JsonObject(emptyMap())
        val last = obj["last_msg"]?.asObj()?.get("data")?.asObj()
            ?: obj["last_message"]?.asObj()?.get("data")?.asObj()
            ?: JsonObject(emptyMap())
        val userId = user["id"]?.asInt() ?: user["user_id"]?.asInt() ?: obj["user_id"]?.asInt()
            ?: return null
        if (userId == 0) return null
        var lastText = plainText(last["message"]?.asString() ?: last["msg"]?.asString() ?: "")
        if (lastText.isEmpty() && last["images"] != null) lastText = "[图片]"
        return MessageItem(
            userId = userId,
            userName = user["name"]?.asString() ?: user["username"]?.asString() ?: "用户 $userId",
            avatar = coverUrl(user["avatar"]?.asString() ?: user["face"]?.asString() ?: ""),
            lastMessage = lastText,
            unread = obj["no_read"]?.asInt() ?: obj["unread"]?.asInt() ?: 0,
            lastTime = parseTime(last["time"] ?: obj["updated_at"] ?: obj["time"]),
        )
    }

    fun messageList(data: JsonElement?): List<MessageItem> {
        val rawList = data.asList().ifEmpty { data.asObj()["list"].asList() }
        return rawList.mapNotNull { messageItem(it) }
    }

    fun messageRecords(data: JsonElement?, selfUid: Int): List<MessageRecord> {
        val out = ArrayList<MessageRecord>()
        fun add(raw: JsonElement, streamId: String? = null) {
            if (raw is kotlinx.serialization.json.JsonArray) {
                raw.forEach { add(it) }
                return
            }
            val obj = raw.asObj()
            val data = obj["data"]?.asObj() ?: JsonObject(emptyMap())
            val message = data["message"]?.asString() ?: data["msg"]?.asString()
                ?: obj["message"]?.asString() ?: obj["msg"]?.asString() ?: return
            val uid = data["uid"]?.asInt() ?: data["user_id"]?.asInt() ?: obj["uid"]?.asInt() ?: 0
            out.add(
                MessageRecord(
                    uid = uid,
                    mine = uid == selfUid,
                    message = plainText(message),
                    time = parseTime(data["time"] ?: obj["time"] ?: obj["created_at"]),
                ),
            )
        }
        data.asList().forEach { add(it) }
        data.asObj().forEach { (_, value) -> add(value) }
        return out.sortedBy { it.time ?: 0L }
    }

    fun notifyItem(raw: JsonElement, fallbackType: Int): NotifyItem? {
        val obj = raw.asObj()
        val params = obj["notify_params"]?.asObj() ?: obj["params"]?.asObj()
            ?: JsonObject(emptyMap())
        val sender = obj["sender"]?.asObj()?.takeIf { it.isNotEmpty() }
            ?: obj["user"]?.asObj()?.takeIf { it.isNotEmpty() }
            ?: obj["user_info"]?.asObj() ?: JsonObject(emptyMap())
        val resource = params["resource"]?.asObj() ?: JsonObject(emptyMap())
        return NotifyItem(
            type = obj["type"]?.asInt() ?: fallbackType,
            senderName = sender["name"]?.asString() ?: sender["username"]?.asString() ?: "Mfuns",
            avatar = coverUrl(sender["avatar"]?.asString() ?: sender["face"]?.asString() ?: ""),
            text = plainText(
                params["reply_text"]?.asString() ?: params["text"]?.asString()
                    ?: params["content"]?.asString() ?: obj["content"]?.asString()
                    ?: obj["text"]?.asString() ?: obj["title"]?.asString() ?: "",
            ),
            createdAt = parseTime(obj["created_at"] ?: obj["time"]),
            resourceId = params["resource_id"]?.asInt() ?: resource["id"]?.asInt()
                ?: obj["resource_id"]?.asInt() ?: obj["content_id"]?.asInt(),
            resourceType = params["resource_type"]?.asInt() ?: resource["type"]?.asInt()
                ?: obj["resource_type"]?.asInt(),
        )
    }

    fun notifyList(data: JsonElement?, type: Int): List<NotifyItem> {
        val rawList = data.asList()
            .ifEmpty { data.asObj()["list"].asList() }
            .ifEmpty { data.asObj()["items"].asList() }
            .ifEmpty { data.asObj()["data"]?.asObj()?.get("list").asList() }
        return rawList.mapNotNull { notifyItem(it, type) }
    }

    fun categoryItem(raw: JsonElement): CategoryItem? {
        val obj = raw.asObj()
        val id = obj["id"]?.asInt() ?: return null
        val name = obj["name"]?.asString() ?: return null
        if (id == 0 || name.isEmpty()) return null
        return CategoryItem(id = id, name = name, parentId = obj["parent_id"]?.asInt())
    }

    fun categories(data: JsonElement?): List<CategoryItem> {
        val out = LinkedHashMap<Int, CategoryItem>()
        fun walk(raw: JsonElement?) {
            raw.asList().forEach { element ->
                val obj = element.asObj()
                categoryItem(obj)?.let { out[it.id] = it }
                walk(obj["children"])
                walk(obj["list"])
            }
        }
        walk(data)
        return out.values.sortedBy { it.name }
    }

    fun favoriteFolder(raw: JsonElement): FavoriteFolderItem? {
        val obj = raw.asObj()
        val id = obj["id"]?.asInt() ?: return null
        if (id == 0) return null
        return FavoriteFolderItem(
            id = id,
            name = obj["name"]?.asString() ?: "未命名收藏夹",
            description = obj["desc"]?.asString() ?: "",
            count = obj["count"]?.asInt() ?: 0,
        )
    }

    fun favoriteFolders(data: JsonElement?): List<FavoriteFolderItem> {
        val rawList = data.asList().ifEmpty { data.asObj()["list"].asList() }
        return rawList.mapNotNull { favoriteFolder(it) }
    }

    fun contentDetail(type: Int, id: Int, root: JsonElement): ContentDetail {
        val obj = root.asObj()
        val base = contentItem(obj) ?: ContentItem(
            id = id,
            title = "",
            summary = "",
            cover = "",
            author = "",
            authorId = null,
            authorAvatar = "",
            category = "",
            type = type,
            likes = 0,
            comments = 0,
            views = 0,
            createdAt = null,
        )
        val resource = when (type) {
            ResourceType.VIDEO -> obj
            ResourceType.FEED -> obj
            else -> obj["article"]?.asObj()?.takeIf { it.isNotEmpty() } ?: obj
        }
        val user = (obj["user"]?.asObj()?.takeIf { it.isNotEmpty() }
            ?: resource["user"]?.asObj() ?: JsonObject(emptyMap()))
        val category = (obj["category"]?.asObj()?.takeIf { it.isNotEmpty() }
            ?: resource["category"]?.asObj() ?: JsonObject(emptyMap()))
        val like = obj["like_status"]?.asObj()?.get("like")?.asObj() ?: JsonObject(emptyMap())
        val commentsObj = obj["comments"]?.asObj() ?: JsonObject(emptyMap())
        val commentCount = obj["comment_count"]?.asInt()
            ?: resource["comment_count"]?.asInt()
            ?: commentsObj["floor_count"]?.asInt()
            ?: commentsObj["floor_num"]?.asInt()
            ?: obj["floor_num"]?.asInt()
            ?: 0
        val body = plainText(resource["content"]?.asString() ?: resource["summary"]?.asString() ?: "")
        val tags = when (val raw = obj["tags"] ?: resource["tags"] ?: resource["tag"]) {
            is kotlinx.serialization.json.JsonPrimitive -> raw.asString()
                .split(',').map { it.trim() }.filter { it.isNotEmpty() }
            else -> raw.asList().map { it.asString() }.filter { it.isNotEmpty() }
        }
        return ContentDetail(
            item = base.copy(
                type = type,
                author = user["name"]?.asString() ?: user["username"]?.asString() ?: base.author,
                authorAvatar = coverUrl(
                    user["avatar"]?.asString() ?: user["face"]?.asString() ?: "",
                ).ifEmpty { base.authorAvatar },
                authorId = user["id"]?.asInt() ?: user["user_id"]?.asInt() ?: base.authorId,
                category = category["name"]?.asString() ?: base.category,
                likes = like["count"]?.asInt() ?: resource["like_count"]?.asInt() ?: base.likes,
                comments = commentCount,
                views = obj["view_count"]?.asInt() ?: resource["view_count"]?.asInt() ?: base.views,
            ),
            body = body,
            tags = tags,
            commentAreaId = resource["comment_area_id"]?.asInt()
                ?: obj["commentId"]?.asInt() ?: obj["comment_area_id"]?.asInt(),
        )
    }

    fun coverUrl(raw: String): String {
        val cover = raw.trim()
        return when {
            cover.isEmpty() -> ""
            cover.startsWith("//") -> "https:$cover"
            cover.startsWith("/") -> "https://cdn2.mfuns.net$cover"
            cover.startsWith("static/") -> "https://cdn2.mfuns.net/$cover"
            else -> cover
        }
    }

    fun plainText(raw: String): String {
        if (raw.isEmpty()) return ""
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) {
            val ops = try {
                kotlinx.serialization.json.Json.parseToJsonElement(trimmed).asObj()["ops"]?.asList()
            } catch (_: Exception) {
                null
            }
            if (ops != null) {
                val text = ops.mapNotNull { op ->
                    val insert = op.asObj()["insert"]
                    when (insert) {
                        is kotlinx.serialization.json.JsonPrimitive ->
                            if (insert.isString) insert.content else null
                        else -> null
                    }
                }.joinToString("").trim()
                if (text.isNotEmpty()) return text
            }
        }
        return htmlToText(raw)
    }

    private fun htmlToText(raw: String): String {
        var text = raw
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")
            .replace(Regex("<[^>]*>"), "")
        if (text.contains('&')) {
            text = text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
        }
        return text
            .replace(' ', ' ')
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split('\n')
            .joinToString("\n") { line ->
                line.replace(Regex("[\\t\\f\\v ]+"), " ").trim()
            }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    fun parseTime(raw: JsonElement?): Long? {
        if (raw == null) return null
        val str = raw.asString()
        if (str.isNotEmpty()) {
            str.replaceFirst(' ', 'T').toLongOrNull()?.let { seconds ->
                return if (seconds > 100000000000L) seconds else seconds * 1000
            }
            val match = Regex("^(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2}):(\\d{2})").find(str)
            if (match != null) {
                val g = match.groupValues
                val cal = java.util.Calendar.getInstance().apply {
                    clear()
                    set(g[1].toInt(), g[2].toInt() - 1, g[3].toInt(), g[4].toInt(), g[5].toInt(), g[6].toInt())
                }
                return cal.timeInMillis
            }
        }
        val seconds = raw.asInt() ?: return null
        if (seconds <= 0) return null
        return if (seconds > 100000000000L) seconds.toLong() else seconds.toLong() * 1000
    }

    private fun resourceType(raw: String): Int {
        val n = raw.trim().toIntOrNull()
        if (n != null) return n
        return when {
            raw.contains("video", true) -> ResourceType.VIDEO
            raw.contains("feed", true) || raw.contains("dynamic", true) -> ResourceType.FEED
            else -> ResourceType.ARTICLE
        }
    }
}

class MfunsRepository(val api: ApiClient) {

    suspend fun recommendations(category: Int = -1, size: Int = 10): List<ContentItem> =
        Parser.contentList(api.recommend(category, size))

    suspend fun hotRank(): List<ContentItem> =
        Parser.contentList(api.hotRank())

    suspend fun timeline(page: Int, size: Int = 20): List<ContentItem> =
        Parser.contentList(api.timelineFeeds(page, size))

    suspend fun search(text: String, type: Int = -1, page: Int = 1): List<ContentItem> =
        Parser.contentList(api.search(text, type, page))

    suspend fun searchUsers(keyword: String): List<UserItem> =
        Parser.userList(api.searchUsers(keyword))

    suspend fun categories(): List<CategoryItem> =
        Parser.categories(api.categories())

    suspend fun categoryContents(categoryId: Int): List<ContentItem> =
        Parser.contentList(api.categoryContents(categoryId))

    suspend fun detail(type: Int, id: Int): ContentDetail =
        Parser.contentDetail(type, id, api.resourceDetail(type, id))

    suspend fun videoSources(videoId: Int): List<VideoSource> = api.videoPlayAddress(videoId)

    suspend fun likeStatus(resourceId: Int, resourceType: Int): LikeStatus =
        api.likeStatus(resourceId, resourceType)

    suspend fun setLike(resourceId: Int, resourceType: Int, like: Boolean) =
        api.setLike(resourceId, resourceType, like)

    suspend fun reward(resourceId: Int, resourceType: Int, count: Int = 1): String =
        api.reward(resourceId, resourceType, count)

    suspend fun comments(areaId: Int, page: Int = 1): List<CommentItem> =
        Parser.commentList(api.comments(areaId, page))

    suspend fun createComment(areaId: Int, content: String) =
        api.createComment(areaId, content)

    suspend fun favoriteFolders(userId: Int): List<FavoriteFolderItem> =
        Parser.favoriteFolders(api.favoriteFolders(userId))

    suspend fun favoriteItems(folder: FavoriteFolderItem): List<ContentItem> =
        Parser.contentList(api.favoriteItems(folder.id).first)

    suspend fun isFavorite(resourceId: Int, resourceType: Int): Boolean =
        api.isFavorite(resourceId, resourceType)

    suspend fun addFavorite(listId: Int, resourceId: Int, resourceType: Int) =
        api.addFavorite(listId, resourceId, resourceType)

    suspend fun removeFavorite(listId: Int, resourceId: Int, resourceType: Int) =
        api.removeFavorite(listId, resourceId, resourceType)

    suspend fun sign(): String = api.sign()

    suspend fun signInfo(): SignInfo = api.signInfo()

    suspend fun history(): List<ContentItem> =
        Parser.contentList(api.history())

    suspend fun messageConversations(): List<MessageItem> =
        Parser.messageList(api.messageConversations())

    suspend fun messageRecords(uid: Int, selfUid: Int): List<MessageRecord> =
        Parser.messageRecords(api.messageRecord(uid), selfUid)

    suspend fun sendMessage(toUid: Int, content: String) =
        api.sendMessage(toUid, content)

    suspend fun notifyCounts(): NotifyCounts = api.notifyCounts()

    suspend fun notifications(type: Int, page: Int = 1): List<NotifyItem> =
        Parser.notifyList(api.notifications(type, page), type)

    suspend fun followStatus(userId: Int): Boolean = api.followStatus(userId)

    suspend fun setFollow(userId: Int, follow: Boolean) = api.setFollow(userId, follow)

    suspend fun userProfile(userId: Int): UserItem? =
        Parser.userItem(api.userGet(userId))

    suspend fun userArticles(userId: Int): List<ContentItem> =
        Parser.contentList(api.userArticles(userId))

    suspend fun userVideos(userId: Int): List<ContentItem> =
        Parser.contentList(api.userVideos(userId))

    suspend fun currentUser(): UserInfo = api.userInfo()
}
