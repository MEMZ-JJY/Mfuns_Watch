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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Comment
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
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import net.mfuns.watch.ui.DetailViewModel
import net.mfuns.watch.ui.components.AvatarImage
import net.mfuns.watch.ui.components.CoverImage
import net.mfuns.watch.ui.formatCount

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    type: Int,
    id: Int,
    onPlayVideo: (Int) -> Unit,
    onOpenComments: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()

    LaunchedEffect(type, id) { viewModel.load(type, id) }

    LaunchedEffect(action) {
        if (action != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.consumeAction()
        }
    }

    if (state.loading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val detail = state.detail
    if (detail == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = state.error ?: "加载失败",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error,
            )
        }
        return
    }

    val item = detail.item

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { ListHeader { Text(item.typeLabel) } }

        if (item.isVideo) {
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CoverImage(
                        url = item.cover,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp),
                    )
                    Chip(
                        onClick = { onPlayVideo(item.id) },
                        modifier = Modifier.align(Alignment.Center),
                        label = { Text("播放") },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }

        item {
            Text(
                text = item.title,
                style = MaterialTheme.typography.title3,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(url = item.authorAvatar, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.author,
                        style = MaterialTheme.typography.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOf(
                            "${formatCount(item.views)}浏览",
                            "${formatCount(item.likes)}赞",
                            "${formatCount(item.comments)}评论",
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.caption3,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ActionChip(
                    icon = if (state.like) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    text = if (state.like) "已赞" else "点赞",
                    selected = state.like,
                    onClick = viewModel::toggleLike,
                )
                ActionChip(
                    icon = if (state.favorited) Icons.Filled.Star else Icons.Filled.StarBorder,
                    text = if (state.favorited) "已藏" else "收藏",
                    selected = state.favorited,
                    onClick = viewModel::toggleFavorite,
                )
                ActionChip(
                    icon = Icons.Filled.Send,
                    text = "投币",
                    selected = false,
                    onClick = viewModel::reward,
                )
                ActionChip(
                    icon = Icons.Filled.Comment,
                    text = "评论",
                    selected = false,
                    onClick = {
                        detail.commentAreaId?.let { onOpenComments(it) }
                    },
                )
            }
        }

        if (detail.tags.isNotEmpty()) {
            item {
                Text(
                    text = detail.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.primary,
                )
            }
        }

        if (detail.body.isNotEmpty() && detail.body != "暂无简介") {
            item {
                Text(
                    text = detail.body,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (action != null) {
            item {
                Text(
                    text = action!!,
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.primary,
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Chip(
        onClick = onClick,
        label = { Text(text) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurface
                },
            )
        },
        colors = if (selected) {
            androidx.wear.compose.material.ChipDefaults.primaryChipColors()
        } else {
            androidx.wear.compose.material.ChipDefaults.secondaryChipColors()
        },
    )
}
