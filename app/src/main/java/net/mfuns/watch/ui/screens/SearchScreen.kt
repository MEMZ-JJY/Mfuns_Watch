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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import net.mfuns.watch.api.ContentItem
import net.mfuns.watch.ui.SearchViewModel
import net.mfuns.watch.ui.UiState
import net.mfuns.watch.ui.components.ContentCard

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenDetail: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ListHeader { Text("搜索") } }
        item {
            WatchTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = "输入关键词",
                imeAction = ImeAction.Search,
                onImeAction = { viewModel.search() },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TypeChip("全部", typeFilter == -1) {
                    viewModel.setTypeFilter(-1)
                }
                TypeChip("视频", typeFilter == 1) {
                    viewModel.setTypeFilter(1)
                }
                TypeChip("文章", typeFilter == 0) {
                    viewModel.setTypeFilter(0)
                }
                Spacer(Modifier.weight(1f))
                Chip(
                    onClick = { viewModel.search() },
                    label = { Text("搜索") },
                )
            }
        }

        when (val resultState = results) {
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
                    text = resultState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp),
                )
            }

            is UiState.Success -> {
                val list = resultState.data
                if (list.isEmpty()) {
                    item {
                        Text(
                            text = "没有找到相关内容",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                items(list) { item ->
                    ContentCard(
                        item = item,
                        onClick = { onOpenDetail(item.type, item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        label = { Text(label) },
        colors = if (selected) {
            ChipDefaults.primaryChipColors()
        } else {
            ChipDefaults.secondaryChipColors()
        },
    )
}
