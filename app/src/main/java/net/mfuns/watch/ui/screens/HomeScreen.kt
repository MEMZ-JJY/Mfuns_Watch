        package net.mfuns.watch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import coil.compose.AsyncImage
import net.mfuns.watch.api.ContentItem
import net.mfuns.watch.api.FeedItem
import net.mfuns.watch.api.FeedReference
import net.mfuns.watch.data.SessionStore
import net.mfuns.watch.ui.HomeViewModel
import net.mfuns.watch.ui.UiState
import net.mfuns.watch.ui.components.AvatarImage
import net.mfuns.watch.ui.components.ContentCard
import net.mfuns.watch.ui.formatCount
import net.mfuns.watch.ui.formatTime

private val TABS = listOf("推荐", "热榜", "动态", "我的")

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    session: SessionStore.Session,
    onOpenDetail: (Int, Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onOpenComments: (Int) -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { TABS.size })

    Scaffold(
        modifier = modifier,
        timeText = { TimeText() },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageDots(
                pageCount = TABS.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp),
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> FeedTab(
                        title = "推荐",
                        state = viewModel.recommend.collectAsStateWithLifecycle().value,
                        onRetry = viewModel::loadRecommend,
                        onOpenDetail = onOpenDetail,
                    )
                    1 -> FeedTab(
                        title = "热榜",
                        state = viewModel.hot.collectAsStateWithLifecycle().value,
                        onRetry = viewModel::loadHot,
                        onOpenDetail = onOpenDetail,
                    )
                    2 -> DynamicTab(
                        state = viewModel.timeline.collectAsStateWithLifecycle().value,
                        onRetry = viewModel::loadTimeline,
                        onOpenDetail = onOpenDetail,
                        onOpenUser = onOpenUser,
                        onOpenComments = onOpenComments,
                    )
                    else -> MineTab(
                        session = session,
                        onNavigate = onNavigate,
                        onOpenUser = onOpenUser,
                        onLogout = onLogout,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun FeedTab(
    title: String,
    state: UiState<List<ContentItem>>,
    onRetry: () -> Unit,
    onOpenDetail: (Int, Int) -> Unit,
) {
    when (state) {
        is UiState.Success -> ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { ListHeader { Text(title) } }
            if (state.data.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无内容",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }
            items(state.data) { item ->
                ContentCard(item = item, onClick = { onOpenDetail(item.type, item.id) })
            }
        }

        is UiState.Error -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                Chip(onClick = onRetry, label = { Text("重试") })
            }
        }

        is UiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun DynamicTab(
    state: UiState<List<FeedItem>>,
    onRetry: () -> Unit,
    onOpenDetail: (Int, Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onOpenComments: (Int) -> Unit,
) {
    when (state) {
        is UiState.Success -> ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { ListHeader { Text("动态") } }
            if (state.data.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无动态",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }
            items(state.data) { feed ->
                FeedPost(
                    feed = feed,
                    onOpenDetail = onOpenDetail,
                    onOpenUser = onOpenUser,
                    onOpenComments = onOpenComments,
                )
            }
        }

        is UiState.Error -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                Chip(onClick = onRetry, label = { Text("重试") })
            }
        }

        is UiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun FeedPost(
    feed: FeedItem,
    onOpenDetail: (Int, Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onOpenComments: (Int) -> Unit,
) {
    Card(
        onClick = { feed.commentAreaId?.let(onOpenComments) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(
                    url = feed.userAvatar,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onOpenUser(feed.userId) },
                )
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feed.userName,
                        style = MaterialTheme.typography.body2,
                        color = nameColor(feed.nameColor) ?: MaterialTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (feed.createdAt != null) {
                        Text(
                            text = formatTime(feed.createdAt),
                            style = MaterialTheme.typography.caption3,
                            color = MaterialTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }

            if (feed.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = feed.content,
                    style = MaterialTheme.typography.body2,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (feed.images.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                FeedImages(feed.images)
            }

            feed.reference?.let { ref ->
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colors.surface)
                        .clickable { onOpenDetail(ref.type, ref.id) }
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FeedPicture(url = ref.cover, Modifier.size(width = 48.dp, height = 36.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ref.title,
                            style = MaterialTheme.typography.body2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(
                                ref.author.takeIf { it.isNotEmpty() },
                                "${formatCount(ref.views)}浏览",
                                "${formatCount(ref.likes)}赞",
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.caption3,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = listOf(
                        "${formatCount(feed.likeCount)}赞",
                        "${formatCount(feed.commentCount)}评论",
                        "${formatCount(feed.viewCount)}浏览",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (feed.commentAreaId != null) {
                    Chip(
                        onClick = { onOpenComments(feed.commentAreaId) },
                        label = { Text("评论") },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedImages(images: List<String>) {
    if (images.size == 1) {
        FeedPicture(url = images[0], Modifier.fillMaxWidth().height(110.dp))
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            images.take(3).forEach { url ->
                FeedPicture(url = url, Modifier.size(56.dp).weight(1f))
            }
        }
    }
}

@Composable
private fun FeedPicture(url: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.surface),
    ) {
        if (url.isEmpty()) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).size(20.dp),
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val NAME_COLORS = mapOf(
    "red" to 0xFFFF5C5C,
    "blue" to 0xFF5CA8FF,
    "green" to 0xFF5CD65C,
    "orange" to 0xFFFFB04C,
    "purple" to 0xFFB97CFF,
    "pink" to 0xFFFB7299,
)

private fun nameColor(raw: String): androidx.compose.ui.graphics.Color? {
    val v = NAME_COLORS[raw.lowercase()] ?: return null
    return androidx.compose.ui.graphics.Color(v)
}

@Composable
private fun MineTab(
    session: SessionStore.Session,
    onNavigate: (String) -> Unit,
    onOpenUser: (Int) -> Unit,
    onLogout: () -> Unit,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ListHeader { Text("我的") } }
        item {
            Chip(
                label = {
                    Text(
                        text = session.name.ifEmpty { "未登录" },
                        style = MaterialTheme.typography.body1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onClick = { if (session.userId > 0) onOpenUser(session.userId) },
                modifier = Modifier.fillMaxWidth(),
                secondaryLabel = if (session.userId > 0) {
                    {
                        Text(
                            text = "UID: ${session.userId}",
                            style = MaterialTheme.typography.caption3,
                            color = MaterialTheme.colors.onSurfaceVariant,
                        )
                    }
                } else {
                    null
                },
                icon = {
                    AvatarImage(
                        url = session.avatar,
                        modifier = Modifier.size(28.dp),
                    )
                },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        item { MenuChip("搜索", Icons.Filled.Search) { onNavigate("search") } }
        item { MenuChip("分区", Icons.Filled.List) { onNavigate("category") } }
        item { MenuChip("签到", Icons.Filled.CheckCircle) { onNavigate("sign") } }
        item { MenuChip("收藏", Icons.Filled.Star) { onNavigate("favorites") } }
        item { MenuChip("历史", Icons.Filled.History) { onNavigate("history") } }
        item { MenuChip("消息", Icons.Filled.Mail) { onNavigate("messages") } }
        item { MenuChip("关于喵御宅手表", Icons.Filled.Info) { onNavigate("about") } }
        item {
            MenuChip("退出登录", Icons.Filled.ExitToApp, tint = MaterialTheme.colors.error) {
                onLogout()
            }
        }
    }
}

@Composable
private fun MenuChip(
    label: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colors.primary,
    onClick: () -> Unit,
) {
    Chip(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, maxLines = 1) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        },
    )
}

@Composable
fun StatRow(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCount(value),
            style = MaterialTheme.typography.body1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption3,
            color = MaterialTheme.colors.onSurfaceVariant,
        )
    }
}
