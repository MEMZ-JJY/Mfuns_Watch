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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import net.mfuns.watch.api.ContentItem
import net.mfuns.watch.ui.UserViewModel
import net.mfuns.watch.ui.components.AvatarImage
import net.mfuns.watch.ui.components.ContentCard
import net.mfuns.watch.ui.formatCount

@Composable
fun UserScreen(
    viewModel: UserViewModel,
    userId: Int,
    onOpenDetail: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(userId) { viewModel.load(userId) }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(1500)
            viewModel.consumeMessage()
        }
    }

    if (state.loading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = state.user
    if (user == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = state.error ?: "加载失败",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error,
            )
        }
        return
    }

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ListHeader { Text(user.name) } }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(url = user.avatar, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.body1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (user.bio.isNotEmpty()) {
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.caption3,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatRow("粉丝", user.fans)
                StatRow("关注", user.follows)
                user.level?.let { StatRow("等级", it) }
            }
        }

        item {
            Chip(
                onClick = viewModel::toggleFollow,
                label = { Text(if (state.following) "取消关注" else "关注") },
                colors = if (state.following) {
                    androidx.wear.compose.material.ChipDefaults.secondaryChipColors()
                } else {
                    androidx.wear.compose.material.ChipDefaults.primaryChipColors()
                },
            )
        }

        if (message != null) {
            item {
                Text(
                    text = message!!,
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.primary,
                )
            }
        }

        if (state.articles.isNotEmpty()) {
            item {
                Text(
                    text = "文章",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
            items(state.articles) { item ->
                ContentCard(item = item, onClick = { onOpenDetail(item.type, item.id) })
            }
        }

        if (state.videos.isNotEmpty()) {
            item {
                Text(
                    text = "视频",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
            items(state.videos) { item ->
                ContentCard(item = item, onClick = { onOpenDetail(item.type, item.id) })
            }
        }

        if (state.articles.isEmpty() && state.videos.isEmpty()) {
            item {
                Text(
                    text = "TA 还没有投稿",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
