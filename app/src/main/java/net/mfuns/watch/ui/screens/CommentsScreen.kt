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
import androidx.compose.ui.text.input.ImeAction
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
import net.mfuns.watch.api.CommentItem
import net.mfuns.watch.ui.CommentsViewModel
import net.mfuns.watch.ui.UiState
import net.mfuns.watch.ui.components.AvatarImage
import net.mfuns.watch.ui.formatCount
import net.mfuns.watch.ui.formatTime

@Composable
fun CommentsScreen(
    viewModel: CommentsViewModel,
    areaId: Int,
    modifier: Modifier = Modifier,
) {
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val sendState by viewModel.sendState.collectAsStateWithLifecycle()

    LaunchedEffect(areaId) { viewModel.load(areaId) }

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ListHeader { Text("评论") } }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WatchTextField(
                    value = input,
                    onValueChange = viewModel::setInput,
                    placeholder = "写评论…",
                    imeAction = ImeAction.Send,
                    onImeAction = { viewModel.send(areaId) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Chip(
                    onClick = { viewModel.send(areaId) },
                    label = { Text("发送") },
                )
            }
        }

        item {
            val currentSend = sendState
            Text(
                text = when (currentSend) {
                    is UiState.Loading -> "发送中…"
                    is UiState.Error -> currentSend.message
                    is UiState.Success -> "发送成功"
                    null -> ""
                },
                style = MaterialTheme.typography.caption2,
                color = if (currentSend is UiState.Error) {
                    MaterialTheme.colors.error
                } else {
                    MaterialTheme.colors.primary
                },
            )
        }

        when (val commentState = comments) {
            is UiState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> item {
                Text(
                    text = commentState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp),
                )
            }

            is UiState.Success -> {
                if (commentState.data.isEmpty()) {
                    item {
                        Text(
                            text = "还没有评论",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                items(commentState.data) { comment ->
                    CommentRow(comment)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommentItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AvatarImage(url = comment.avatar, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.userName,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = comment.content,
                style = MaterialTheme.typography.body2,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(formatTime(comment.createdAt))
                    if (comment.likes > 0) append(" · ${formatCount(comment.likes)}赞")
                    if (comment.replyCount > 0) append(" · ${comment.replyCount}回复")
                },
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.onSurfaceVariant,
            )
        }
    }
}
