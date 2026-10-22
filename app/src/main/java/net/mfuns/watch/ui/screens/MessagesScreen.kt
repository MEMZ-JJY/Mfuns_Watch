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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
import net.mfuns.watch.api.MessageItem
import net.mfuns.watch.api.MessageRecord
import net.mfuns.watch.ui.MessagesViewModel
import net.mfuns.watch.ui.UiState
import net.mfuns.watch.ui.components.AvatarImage
import net.mfuns.watch.ui.formatCount
import net.mfuns.watch.ui.formatTime

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    onOpenUser: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()

    val chatTarget by viewModel.chatTarget.collectAsStateWithLifecycle()
    if (chatTarget != null) {
        ChatScreen(viewModel = viewModel, target = chatTarget!!, onOpenUser = onOpenUser)
        return
    }

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ListHeader { Text("消息") } }

        item {
            Text(
                text = if (counts.total > 0) {
                    "未读通知 ${formatCount(counts.total)}（赞 ${counts.like} · 评论 ${counts.comment} · @ ${counts.mention} · 系统 ${counts.system}）"
                } else {
                    "暂无未读通知"
                },
                style = MaterialTheme.typography.caption2,
                color = if (counts.total > 0) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        when (val conversationState = conversations) {
            is UiState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> item {
                Text(
                    text = conversationState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp),
                )
            }

            is UiState.Success -> {
                if (conversationState.data.isEmpty()) {
                    item {
                        Text(
                            text = "暂无私信会话",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                items(conversationState.data) { item ->
                    ConversationChip(
                        item = item,
                        onClick = { viewModel.openChat(item) },
                        onOpenUser = { onOpenUser(item.userId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationChip(
    item: MessageItem,
    onClick: () -> Unit,
    onOpenUser: () -> Unit,
) {
    Chip(
        label = {
            Text(
                text = item.userName + if (item.unread > 0) " (${item.unread})" else "",
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        secondaryLabel = {
            Text(
                text = item.lastMessage.ifEmpty { "[图片]" } +
                    (item.lastTime?.let { " · ${formatTime(it)}" } ?: ""),
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        icon = {
            AvatarImage(url = item.avatar, modifier = Modifier.size(24.dp))
        },
    )
}

@Composable
private fun ChatScreen(
    viewModel: MessagesViewModel,
    target: MessageItem,
    onOpenUser: (Int) -> Unit,
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val input by viewModel.chatInput.collectAsStateWithLifecycle()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ListHeader { Text("与 ${target.userName} 的对话") } }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WatchTextField(
                    value = input,
                    onValueChange = viewModel::setChatInput,
                    placeholder = "发送私信…",
                    imeAction = ImeAction.Send,
                    onImeAction = { viewModel.send() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Chip(onClick = { viewModel.send() }, label = { Text("发送") })
            }
        }

        when (val recordState = records) {
            is UiState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> item {
                Text(
                    text = recordState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp),
                )
            }

            is UiState.Success -> {
                if (recordState.data.isEmpty()) {
                    item {
                        Text(
                            text = "暂无消息",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                items(recordState.data) { record ->
                    MessageBubble(record)
                }
            }
        }

        item {
            Chip(
                onClick = { viewModel.closeChat() },
                label = { Text("返回消息列表") },
            )
        }
    }
}

@Composable
private fun MessageBubble(record: MessageRecord) {
    val align = if (record.mine) Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = align,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (record.mine) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = record.message,
                style = MaterialTheme.typography.body2,
                textAlign = if (record.mine) TextAlign.End else TextAlign.Start,
            )
            if (record.time != null) {
                Text(
                    text = formatTime(record.time),
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}
