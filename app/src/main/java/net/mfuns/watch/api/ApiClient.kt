package net.mfuns.watch.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, override val message: String) : Exception(message)

class ApiClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var authToken: String = ""

    private fun url(path: String, query: Map<String, Any?> = emptyMap()): String {
        val q = query.entries
            .filter { it.value != null }
            .joinToString("&") { "${it.key}=${java.net.URLEncoder.encode("${it.value}", "UTF-8")}" }
        return if (q.isEmpty()) "$BASE_URL$path" else "$BASE_URL$path?$q"
    }

    private fun execute(request: Request): String {
        http.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (resp.code >= 400) throw ApiException(resp.code, "HTTP ${resp.code}")
            if (body.isBlank()) throw ApiException(0, "服务器返回了空数据")
            return body
        }
    }

    private fun request(path: String, query: Map<String, Any?> = emptyMap()): String {
        val fullUrl = url(path, query)
        android.util.Log.i("MFUNS_API", "GET $fullUrl")
        val req = Request.Builder()
            .url(fullUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .apply { if (authToken.isNotEmpty()) header("Authorization", authToken) }
            .get()
            .build()
        return execute(req)
    }

    private fun postForm(path: String, form: Map<String, Any?>): String {
        android.util.Log.i("MFUNS_API", "POST_FORM $path $form")
        val builder = FormBody.Builder()
        form.forEach { (k, v) -> if (v != null) builder.add(k, "$v") }
        val req = Request.Builder()
            .url("$BASE_URL$path")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .apply { if (authToken.isNotEmpty()) header("Authorization", authToken) }
            .post(builder.build())
            .build()
        return execute(req)
    }

    private fun postJson(path: String, body: Map<String, Any?>): String {
        android.util.Log.i("MFUNS_API", "POST_JSON $path $body")
        val payload = body.entries
            .filter { it.value != null }
            .joinToString(",", "{", "}") { (k, v) ->
                val value = when (val raw = v) {
                    is Number, is Boolean -> raw.toString()
                    else -> "\"${escapeJson("$raw")}\""
                }
                "\"$k\":$value"
            }
        val req = Request.Builder()
            .url("$BASE_URL$path")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .apply { if (authToken.isNotEmpty()) header("Authorization", authToken) }
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        return execute(req)
    }

    private fun String.toRequestBody(contentType: okhttp3.MediaType) =
        okhttp3.RequestBody.create(contentType, this)

    private fun escapeJson(raw: String): String = raw
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private class Envelope(val code: Int, val msg: String, val data: JsonElement?)

    private fun parse(body: String): Envelope {
        val root = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw ApiException(0, "服务器返回了无法识别的数据")
        }
        val code = root["code"].asInt() ?: 0
        val msg = root["msg"]?.asString() ?: "请求失败"
        val data = root["data"]
        if (code != 1) {
            android.util.Log.e("MFUNS_API", "ERR code=$code msg=$msg body=${body.take(200)}")
            throw ApiException(code, msg.ifEmpty { "请求失败($code)" })
        }
        return Envelope(code, msg, data)
    }

    private suspend fun get(path: String, query: Map<String, Any?> = emptyMap()): JsonElement? =
        withContext(Dispatchers.IO) { parse(request(path, query)).data }

    private suspend fun postFormData(path: String, form: Map<String, Any?>): JsonElement? =
        withContext(Dispatchers.IO) { parse(postForm(path, form)).data }

    private suspend fun postJsonData(path: String, body: Map<String, Any?>): JsonElement? =
        withContext(Dispatchers.IO) { parse(postJson(path, body)).data }

    suspend fun login(account: String, password: String): String {
        val data = postFormData("/v1/auth/login", mapOf("account" to account, "password" to password))
        val token = data.asObj()["access_token"]?.asString()
            ?: data.asObj()["token"]?.asString()
            ?: throw ApiException(0, "登录响应中没有 access_token")
        this.authToken = token
        return token
    }

    fun setToken(token: String) {
        this.authToken = token
    }

    suspend fun userInfo(): UserInfo {
        val data = get("/v1/user/info")
        val obj = data.asObj()
        val user = obj["user"]?.asObj()?.takeIf { it.isNotEmpty() }
            ?: obj["user_info"]?.asObj()?.takeIf { it.isNotEmpty() }
            ?: obj
        val login = obj["login"]?.asBool() ?: true
        if (!login) throw ApiException(0, "登录状态已失效")
        return UserInfo(
            id = user["id"]?.asInt() ?: user["user_id"]?.asInt() ?: 0,
            name = user["name"]?.asString() ?: user["username"]?.asString() ?: "Mfuns 用户",
            avatar = Parser.coverUrl(user["avatar"]?.asString() ?: user["face"]?.asString() ?: ""),
        )
    }

    suspend fun recommend(category: Int = -1, size: Int = 10): JsonElement? =
        get("/v1/recommend/get", mapOf("category" to category, "size" to size))

    suspend fun hotRank(): JsonElement? =
        get("/v1/leaderboards/hot")

    suspend fun timelineFeeds(page: Int = 1, size: Int = 20): JsonElement? =
        get("/v1/feeds/new_reply_list", mapOf("page" to page, "size" to size, "html" to 1))

    suspend fun feedDetail(feedId: Int): JsonElement =
        get("/v1/feeds/get", mapOf("id" to feedId, "html" to 1)).asObj()

    suspend fun search(
        text: String,
        type: Int = -1,
        page: Int = 1,
        size: Int = 20,
        sort: String = "all",
    ): JsonElement? =
        get(
            "/v1/search/resource",
            mapOf("text" to text, "type" to type, "sort" to sort, "page" to page, "size" to size),
        )

    suspend fun searchUsers(keyword: String, page: Int = 1, size: Int = 20): JsonElement? =
        get("/v1/search/user", mapOf("user" to keyword, "page" to page, "size" to size))

    suspend fun categories(): JsonElement? =
        get("/v1/category/all")

    suspend fun categoryContents(categoryId: Int, size: Int = 20): JsonElement? =
        get("/v1/recommend/get", mapOf("category" to categoryId, "size" to size))

    suspend fun userGet(userId: Int): JsonElement =
        get("/v1/user/get_user", mapOf("id" to userId)).asObj()

    suspend fun articleGet(id: Int): JsonElement =
        get("/v1/article/get", mapOf("id" to id, "html" to 1)).asObj()

    suspend fun videoGet(id: Int): JsonElement =
        get("/v1/video/get", mapOf("id" to id, "html" to 1)).asObj()

    suspend fun resourceDetail(type: Int, id: Int): JsonElement =
        when (type) {
            1 -> videoGet(id)
            3 -> feedDetail(id)
            else -> articleGet(id)
        }

    suspend fun videoPlayAddress(videoId: Int): List<VideoSource> {
        val data = get("/v1/video/getPlayAddress", mapOf("id" to videoId)).asObj()
        val parts = data["videos"].asList()
        val out = ArrayList<VideoSource>()
        parts.forEachIndexed { index, part ->
            part.asObj()["video_url"].asList().forEach { src ->
                val obj = src.asObj()
                val url = obj["url"]?.asString() ?: ""
                if (url.isNotEmpty()) {
                    out.add(
                        VideoSource(
                            part = index + 1,
                            name = obj["name"]?.asString() ?: "默认清晰度",
                            label = obj["label"]?.asString() ?: "",
                            url = url,
                        ),
                    )
                }
            }
        }
        return out
    }

    suspend fun likeStatus(resourceId: Int, resourceType: Int): LikeStatus {
        val data = get("/v1/like/status", mapOf("id" to resourceId, "type" to resourceType))
        val status = data.asObj()["status"].asObj()
        val like = status["like"].asObj()
        return LikeStatus(
            liked = like["is_active"]?.asBool() ?: false,
            likes = like["count"]?.asInt() ?: 0,
        )
    }

    suspend fun setLike(resourceId: Int, resourceType: Int, like: Boolean) {
        postJsonData(
            if (like) "/v1/like/like" else "/v1/like/cancel",
            mapOf("id" to resourceId, "type" to resourceType),
        )
    }

    suspend fun reward(resourceId: Int, resourceType: Int, count: Int = 1): String {
        val body = parse(
            withContext(Dispatchers.IO) {
                postForm(
                    "/v1/reward/reward",
                    mapOf("id" to resourceId, "type" to resourceType, "count" to count),
                )
            },
        )
        return body.msg
    }

    suspend fun favoriteFolders(userId: Int): JsonElement? =
        get("/v1/favorite/get_favorite_list", mapOf("user_id" to userId))

    suspend fun favoriteItems(favoriteId: Int, lastId: Int = 0): Pair<JsonElement?, Int?> {
        val data = get("/v1/favorite/get_favorite_item", mapOf("favorite_id" to favoriteId, "last_id" to lastId))
            .asObj()
        return data to data["last_id"]?.asInt()
    }

    suspend fun isFavorite(resourceId: Int, resourceType: Int): Boolean =
        get("/v1/favorite/is_favorite", mapOf("resource_id" to resourceId, "resource_type" to resourceType))
            .asObj()["is_favorite"]?.asBool() ?: false

    suspend fun addFavorite(listId: Int, resourceId: Int, resourceType: Int) {
        postJsonData(
            "/v1/favorite/add_favorite",
            mapOf("list_id" to listId, "resource_id" to resourceId, "type" to resourceType),
        )
    }

    suspend fun removeFavorite(listId: Int, resourceId: Int, resourceType: Int) {
        postJsonData(
            "/v1/favorite/remove_favorite_by_resource",
            mapOf("list_id" to listId, "resource_id" to resourceId, "type" to resourceType),
        )
    }

    suspend fun comments(areaId: Int, page: Int = 1): JsonElement? =
        get("/v1/comment/list", mapOf("area_id" to areaId, "page" to page, "order" to "desc", "html" to 0))

    suspend fun createComment(areaId: Int, content: String) {
        postJsonData(
            "/v1/comment/create",
            mapOf("area_id" to areaId, "content" to quill(content), "images" to "[]", "html" to 1),
        )
    }

    suspend fun sign(): String {
        val body = parse(
            withContext(Dispatchers.IO) { request("/v1/sign/sign") },
        )
        return body.msg
    }

    suspend fun signInfo(): SignInfo {
        val data = get("/v1/sign/sign_list").asObj()
        return SignInfo(
            signedDays = data["list"]?.let { raw ->
                raw.asList().mapIndexedNotNull { index, element ->
                    if (index > 0 && "${element.asString()}" == "1") index else null
                }
            } ?: emptyList(),
            monthTimes = data["month_times"]?.asInt() ?: 0,
            allTimes = data["all_times"]?.asInt() ?: 0,
        )
    }

    suspend fun history(startTime: String? = null): JsonElement? =
        get("/v1/history/get", mapOf("start_time" to startTime))

    suspend fun messageConversations(page: Int = 1): JsonElement? =
        get("/v1/message/list", mapOf("page" to page))

    suspend fun messageRecord(uid: Int, msgId: String? = null): JsonElement? =
        get(
            "/v1/message/record",
            mapOf("uid" to uid, "msg_id" to msgId, "html" to 1),
        )

    suspend fun notifyCounts(): NotifyCounts {
        val data = get("/v1/notify/count").asObj()
        return NotifyCounts(
            like = data["like"]?.asInt() ?: 0,
            comment = data["comment"]?.asInt() ?: 0,
            mention = data["mention"]?.asInt() ?: 0,
            system = data["system"]?.asInt() ?: 0,
            message = data["message"]?.asInt() ?: 0,
        )
    }

    suspend fun notifications(type: Int, page: Int = 1): JsonElement? =
        get("/v1/notify/get", mapOf("type" to type, "page" to page))

    suspend fun sendMessage(toUid: Int, content: String) {
        postFormData(
            "/v1/message/send",
            mapOf("to_uid" to toUid, "msg" to quill(content)),
        )
    }

    suspend fun followStatus(userId: Int): Boolean =
        get("/v1/follow/status", mapOf("user_id" to userId))
            .asObj()["status"]?.asBool() ?: false

    suspend fun setFollow(userId: Int, follow: Boolean) {
        postJsonData(
            "/v1/follow/follow",
            mapOf("user_id" to userId, "unfollow" to if (follow) null else 1),
        )
    }

    suspend fun userArticles(userId: Int, cursor: Int = 0): JsonElement? =
        get("/v1/article/user_list", mapOf("user_id" to userId, "aid" to cursor, "type" to "pass"))

    suspend fun userVideos(userId: Int, cursor: Int = 0): JsonElement? =
        get("/v1/video/user_list", mapOf("user_id" to userId, "vid" to cursor, "type" to "pass"))

    private fun quill(text: String): String =
        "{\"ops\":[{\"insert\":\"${escapeJson(text.trim())}\\n\"}]}"

    companion object {
        const val BASE_URL = "https://api.mfuns.net"
        const val USER_AGENT = "MfunsWatch/1.0 (Wear OS; Android)"
    }
}

data class UserInfo(val id: Int, val name: String, val avatar: String)

data class VideoSource(
    val part: Int,
    val name: String,
    val label: String,
    val url: String,
)

data class LikeStatus(val liked: Boolean, val likes: Int)

data class SignInfo(
    val signedDays: List<Int>,
    val monthTimes: Int,
    val allTimes: Int,
)

data class NotifyCounts(
    val like: Int,
    val comment: Int,
    val mention: Int,
    val system: Int,
    val message: Int,
) {
    val total: Int get() = like + comment + mention + system + message
}

fun JsonElement?.asObj(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())

fun JsonElement?.asList(): List<JsonElement> =
    (this as? kotlinx.serialization.json.JsonArray)?.toList() ?: emptyList()

fun JsonElement?.asString(): String = when (this) {
    is JsonPrimitive -> if (isString) content else content
    else -> ""
}

fun JsonElement?.asInt(): Int? = when (this) {
    is JsonPrimitive -> content.toIntOrNull() ?: content.trim().toIntOrNull()
    else -> null
}

fun JsonElement?.asBool(): Boolean = when (this) {
    is JsonPrimitive -> when (content.trim().lowercase()) {
        "true", "1" -> true
        "false", "0" -> false
        else -> false
    }
    else -> false
}
