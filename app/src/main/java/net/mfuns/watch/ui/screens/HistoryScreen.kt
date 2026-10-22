package net.mfuns.watch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import net.mfuns.watch.api.ContentItem
import net.mfuns.watch.ui.HistoryViewModel
import net.mfuns.watch.ui.UiState
import net.mfuns.watch.ui.components.ContentCard

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenDetail: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ListHeader { Text("浏览历史") } }

        when (val historyState = history) {
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
                    text = historyState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp),
                )
            }

            is UiState.Success -> {
                if (historyState.data.isEmpty()) {
                    item {
                        Text(
                            text = "还没有浏览记录",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                items(historyState.data) { item ->
                    ContentCard(
                        item = item,
                        onClick = { onOpenDetail(item.type, item.id) },
                    )
                }
            }
        }
    }
}
