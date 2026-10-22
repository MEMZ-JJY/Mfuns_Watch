package net.mfuns.watch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import net.mfuns.watch.ServiceLocator
import net.mfuns.watch.api.ApiException
import net.mfuns.watch.api.CategoryItem
import net.mfuns.watch.api.CommentItem
import net.mfuns.watch.api.ContentDetail
import net.mfuns.watch.api.ContentItem
import net.mfuns.watch.api.FavoriteFolderItem
import net.mfuns.watch.api.MessageItem
import net.mfuns.watch.api.MessageRecord
import net.mfuns.watch.api.NotifyItem
import net.mfuns.watch.api.NotifyCounts
import net.mfuns.watch.api.ResourceType
import net.mfuns.watch.api.SignInfo
import net.mfuns.watch.api.UserInfo
import net.mfuns.watch.api.UserItem
import net.mfuns.watch.api.VideoSource
import net.mfuns.watch.data.SessionStore

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun Throwable.friendlyMessage(): String = when (this) {
    is ApiException -> message.ifEmpty { "请求失败($code)" }
    is java.net.SocketTimeoutException -> "网络超时，请重试"
    is java.net.UnknownHostException -> "无法连接服务器，请检查网络"
    is java.io.IOException -> "网络异常：${message ?: "未知错误"}"
    else -> message ?: "未知错误"
}

class AppViewModel : ViewModel() {
    private val sessionStore = ServiceLocator.sessionStore

    val session = sessionStore.sessionFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, SessionStore.Session("", 0, "", ""),
    )

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    init {
        viewModelScope.launch {
            val s = sessionStore.current()
            if (s.loggedIn) {
                ServiceLocator.api.setToken(s.token)
                val ok = runCatching { ServiceLocator.repository.currentUser() }
                    .onSuccess { sessionStore.updateUser(it.id, it.name, it.avatar) }
                    .isSuccess
                if (!ok) sessionStore.clear()
            }
            _loginState.value = UiState.Success(Unit)
        }
    }

    fun login(account: String, password: String) {
        _loginState.value = UiState.Loading
        viewModelScope.launch {
            runCatching {
                val token = ServiceLocator.api.login(account, password)
                val info = ServiceLocator.api.userInfo()
                sessionStore.save(token, info.id, info.name, info.avatar)
            }
                .onSuccess { _loginState.value = UiState.Success(Unit) }
                .onFailure { _loginState.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionStore.clear()
            ServiceLocator.api.setToken("")
        }
    }

    fun refreshUser() {
        viewModelScope.launch {
            runCatching {
                val info = ServiceLocator.repository.currentUser()
                sessionStore.updateUser(info.id, info.name, info.avatar)
            }
        }
    }
}

class HomeViewModel : ViewModel() {
    private val repo = ServiceLocator.repository

    private val _recommend = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Loading)
    val recommend: StateFlow<UiState<List<ContentItem>>> = _recommend.asStateFlow()

    private val _hot = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Loading)
    val hot: StateFlow<UiState<List<ContentItem>>> = _hot.asStateFlow()

    private val _timeline = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Loading)
    val timeline: StateFlow<UiState<List<ContentItem>>> = _timeline.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        loadRecommend()
        loadHot()
        loadTimeline()
    }

    fun loadRecommend() = launchLoad(_recommend) { repo.recommendations(size = 20) }
    fun loadHot() = launchLoad(_hot) { repo.hotRank() }
    fun loadTimeline() = launchLoad(_timeline) { repo.timeline(page = 1) }

    private fun launchLoad(
        target: MutableStateFlow<UiState<List<ContentItem>>>,
        block: suspend () -> List<ContentItem>,
    ) {
        viewModelScope.launch {
            target.value = UiState.Loading
            runCatching { block() }
                .onSuccess { target.value = UiState.Success(it) }
                .onFailure { target.value = UiState.Error(it.friendlyMessage()) }
        }
    }
}

class SearchViewModel : ViewModel() {
    private val repo = ServiceLocator.repository

    private val _results = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Loading)
    val results: StateFlow<UiState<List<ContentItem>>> = _results.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _typeFilter = MutableStateFlow(-1)
    val typeFilter: StateFlow<Int> = _typeFilter.asStateFlow()

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setTypeFilter(type: Int) {
        _typeFilter.value = type
    }

    fun search() {
        val text = _query.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            _results.value = UiState.Loading
            runCatching { repo.search(text, _typeFilter.value) }
                .onSuccess { _results.value = UiState.Success(it) }
                .onFailure { _results.value = UiState.Error(it.friendlyMessage()) }
        }
    }
}

class CategoryViewModel : ViewModel() {
    private val repo = ServiceLocator.repository

    private val _categories = MutableStateFlow<UiState<List<CategoryItem>>>(UiState.Loading)
    val categories: StateFlow<UiState<List<CategoryItem>>> = _categories.asStateFlow()

    private val _contents = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Loading)
    val contents: StateFlow<UiState<List<ContentItem>>> = _contents.asStateFlow()

    private val _selected = MutableStateFlow<CategoryItem?>(null)
    val selected: StateFlow<CategoryItem?> = _selected.asStateFlow()

    init {
        viewModelScope.launch {
            _categories.value = UiState.Loading
            runCatching { repo.categories() }
                .onSuccess {
                    _categories.value = UiState.Success(it)
                    it.firstOrNull()?.let { category -> select(category) }
                }
                .onFailure { _categories.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun select(category: CategoryItem) {
        _selected.value = category
        viewModelScope.launch {
            _contents.value = UiState.Loading
            runCatching { repo.categoryContents(category.id) }
                .onSuccess { _contents.value = UiState.Success(it) }
                .onFailure { _contents.value = UiState.Error(it.friendlyMessage()) }
        }
    }
}

data class DetailUiState(
    val loading: Boolean = true,
    val detail: ContentDetail? = null,
    val like: Boolean = false,
    val likeCount: Int = 0,
    val favorited: Boolean = false,
    val sources: List<VideoSource> = emptyList(),
    val error: String? = null,
)

class DetailViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    private val sessionStore = ServiceLocator.sessionStore

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private val _action = MutableStateFlow<String?>(null)
    val action: StateFlow<String?> = _action.asStateFlow()

    private var loaded = false

    fun load(type: Int, id: Int) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _state.value = DetailUiState(loading = true)
            runCatching {
                val detail = repo.detail(type, id)
                val like = runCatching { repo.likeStatus(id, type) }.getOrNull()
                val fav = runCatching { repo.isFavorite(id, type) }.getOrDefault(false)
                val sources = if (type == ResourceType.VIDEO) {
                    runCatching { repo.videoSources(id) }.getOrDefault(emptyList())
                } else emptyList()
                _state.value = DetailUiState(
                    loading = false,
                    detail = detail,
                    like = like?.liked ?: false,
                    likeCount = like?.likes ?: detail.item.likes,
                    favorited = fav,
                    sources = sources,
                )
            }.onFailure {
                _state.value = DetailUiState(loading = false, error = it.friendlyMessage())
            }
        }
    }

    fun toggleLike() {
        val s = _state.value
        val detail = s.detail ?: return
        val next = !s.like
        viewModelScope.launch {
            runCatching { repo.setLike(detail.item.id, detail.item.type, next) }
                .onSuccess {
                    _state.value = s.copy(
                        like = next,
                        likeCount = (s.likeCount + if (next) 1 else -1).coerceAtLeast(0),
                    )
                    _action.value = if (next) "已点赞" else "已取消点赞"
                }
                .onFailure { _action.value = it.friendlyMessage() }
        }
    }

    fun toggleFavorite() {
        val s = _state.value
        val detail = s.detail ?: return
        viewModelScope.launch {
            val session = sessionStore.current()
            if (!session.loggedIn) {
                _action.value = "请先登录"
                return@launch
            }
            val next = !s.favorited
            runCatching {
                if (next) {
                    val folders = repo.favoriteFolders(session.userId)
                    val folder = folders.firstOrNull()
                    if (folder == null) {
                        _action.value = "未找到收藏夹"
                        return@launch
                    }
                    repo.addFavorite(folder.id, detail.item.id, detail.item.type)
                } else {
                    val folders = repo.favoriteFolders(session.userId)
                    val folder = folders.firstOrNull { f ->
                        runCatching { repo.favoriteItems(f).any { it.id == detail.item.id } }
                            .getOrDefault(false)
                    } ?: folders.firstOrNull()
                    if (folder == null) {
                        _action.value = "未找到收藏夹"
                        return@launch
                    }
                    repo.removeFavorite(folder.id, detail.item.id, detail.item.type)
                }
            }.onSuccess {
                _state.value = s.copy(favorited = next)
                _action.value = if (next) "已收藏" else "已取消收藏"
            }.onFailure {
                _action.value = it.friendlyMessage()
            }
        }
    }

    fun reward() {
        val s = _state.value
        val detail = s.detail ?: return
        viewModelScope.launch {
            runCatching { repo.reward(detail.item.id, detail.item.type, 1) }
                .onSuccess { _action.value = it.ifEmpty { "投币成功" } }
                .onFailure { _action.value = it.friendlyMessage() }
        }
    }

    fun consumeAction() {
        _action.value = null
    }
}

class CommentsViewModel : ViewModel() {
    private val repo = ServiceLocator.repository

    private val _comments = MutableStateFlow<UiState<List<CommentItem>>>(UiState.Loading)
    val comments: StateFlow<UiState<List<CommentItem>>> = _comments.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _sendState = MutableStateFlow<UiState<Unit>?>(null)
    val sendState: StateFlow<UiState<Unit>?> = _sendState.asStateFlow()

    private var loaded = false

    fun load(areaId: Int) {
        if (loaded) return
        loaded = true
        refresh(areaId)
    }

    fun refresh(areaId: Int) {
        viewModelScope.launch {
            _comments.value = UiState.Loading
            runCatching { repo.comments(areaId) }
                .onSuccess { _comments.value = UiState.Success(it) }
                .onFailure { _comments.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun setInput(text: String) {
        _input.value = text
    }

    fun send(areaId: Int) {
        val text = _input.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            _sendState.value = UiState.Loading
            runCatching { repo.createComment(areaId, text) }
                .onSuccess {
                    _input.value = ""
                    _sendState.value = UiState.Success(Unit)
                    refresh(areaId)
                }
                .onFailure { _sendState.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun consumeSend() {
        _sendState.value = null
    }
}

class SignViewModel : ViewModel() {
    private val repo = ServiceLocator.repository

    private val _info = MutableStateFlow<UiState<SignInfo>>(UiState.Loading)
    val info: StateFlow<UiState<SignInfo>> = _info.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _info.value = UiState.Loading
            runCatching { repo.signInfo() }
                .onSuccess { _info.value = UiState.Success(it) }
                .onFailure { _info.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun sign() {
        viewModelScope.launch {
            runCatching { repo.sign() }
                .onSuccess {
                    _message.value = it.ifEmpty { "签到成功" }
                    refresh()
                }
                .onFailure { _message.value = it.friendlyMessage() }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

class FavoriteViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    private val sessionStore = ServiceLocator.sessionStore

    private val _folders = MutableStateFlow<UiState<List<FavoriteFolderItem>>>(UiState.Loading)
    val folders: StateFlow<UiState<List<FavoriteFolderItem>>> = _folders.asStateFlow()

    private val _items = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Loading)
    val items: StateFlow<UiState<List<ContentItem>>> = _items.asStateFlow()

    private val _opened = MutableStateFlow<FavoriteFolderItem?>(null)
    val opened: StateFlow<FavoriteFolderItem?> = _opened.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionStore.current()
            _folders.value = UiState.Loading
            runCatching { repo.favoriteFolders(session.userId) }
                .onSuccess {
                    _folders.value = UiState.Success(it)
                    it.firstOrNull()?.let { folder -> open(folder) }
                }
                .onFailure { _folders.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun open(folder: FavoriteFolderItem) {
        _opened.value = folder
        viewModelScope.launch {
            _items.value = UiState.Loading
            runCatching { repo.favoriteItems(folder) }
                .onSuccess { _items.value = UiState.Success(it) }
                .onFailure { _items.value = UiState.Error(it.friendlyMessage()) }
        }
    }
}

class HistoryViewModel : ViewModel() {
    private val repo = ServiceLocator.repository

    private val _history = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Loading)
    val history: StateFlow<UiState<List<ContentItem>>> = _history.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _history.value = UiState.Loading
            runCatching { repo.history() }
                .onSuccess { _history.value = UiState.Success(it) }
                .onFailure { _history.value = UiState.Error(it.friendlyMessage()) }
        }
    }
}

class MessagesViewModel : ViewModel() {
    private val repo = ServiceLocator.repository

    private val _conversations = MutableStateFlow<UiState<List<MessageItem>>>(UiState.Loading)
    val conversations: StateFlow<UiState<List<MessageItem>>> = _conversations.asStateFlow()

    private val _counts = MutableStateFlow(NotifyCounts(0, 0, 0, 0, 0))
    val counts: StateFlow<NotifyCounts> = _counts.asStateFlow()

    private val _records = MutableStateFlow<UiState<List<MessageRecord>>>(UiState.Loading)
    val records: StateFlow<UiState<List<MessageRecord>>> = _records.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _chatTarget = MutableStateFlow<MessageItem?>(null)
    val chatTarget: StateFlow<MessageItem?> = _chatTarget.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                val counts = repo.notifyCounts()
                _counts.value = counts
                repo.messageConversations()
            }
                .onSuccess { _conversations.value = UiState.Success(it) }
                .onFailure { _conversations.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun openChat(item: MessageItem) {
        _chatTarget.value = item
        viewModelScope.launch {
            _records.value = UiState.Loading
            val self = sessionUserId()
            runCatching { repo.messageRecords(item.userId, self) }
                .onSuccess { _records.value = UiState.Success(it) }
                .onFailure { _records.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun closeChat() {
        _chatTarget.value = null
    }

    fun setChatInput(text: String) {
        _chatInput.value = text
    }

    fun send() {
        val target = _chatTarget.value ?: return
        val text = _chatInput.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.sendMessage(target.userId, text) }
                .onSuccess {
                    _chatInput.value = ""
                    openChat(target)
                }
                .onFailure { _chatTarget.value = target }
        }
    }

    private suspend fun sessionUserId(): Int =
        ServiceLocator.sessionStore.current().userId
}

data class UserUiState(
    val loading: Boolean = true,
    val user: UserItem? = null,
    val following: Boolean = false,
    val articles: List<ContentItem> = emptyList(),
    val videos: List<ContentItem> = emptyList(),
    val error: String? = null,
)

class UserViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    private val sessionStore = ServiceLocator.sessionStore

    private val _state = MutableStateFlow(UserUiState())
    val state: StateFlow<UserUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var loadedId = 0

    fun load(userId: Int) {
        if (loadedId == userId) return
        loadedId = userId
        viewModelScope.launch {
            _state.value = UserUiState(loading = true)
            runCatching {
                val user = repo.userProfile(userId)
                val following = runCatching { repo.followStatus(userId) }.getOrDefault(false)
                val articles = runCatching { repo.userArticles(userId) }.getOrDefault(emptyList())
                val videos = runCatching { repo.userVideos(userId) }.getOrDefault(emptyList())
                _state.value = UserUiState(
                    loading = false,
                    user = user,
                    following = following,
                    articles = articles,
                    videos = videos,
                )
            }.onFailure {
                _state.value = UserUiState(loading = false, error = it.friendlyMessage())
            }
        }
    }

    fun toggleFollow() {
        val s = _state.value
        val user = s.user ?: return
        val next = !s.following
        viewModelScope.launch {
            runCatching { repo.setFollow(user.id, next) }
                .onSuccess {
                    _state.value = s.copy(following = next)
                    _message.value = if (next) "已关注" else "已取消关注"
                }
                .onFailure { _message.value = it.friendlyMessage() }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
