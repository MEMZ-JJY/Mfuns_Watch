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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import net.mfuns.watch.api.CategoryItem
import net.mfuns.watch.api.ContentItem
import net.mfuns.watch.ui.CategoryViewModel
import net.mfuns.watch.ui.UiState
import net.mfuns.watch.ui.components.ContentCard

@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
    onOpenDetail: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val contents by viewModel.contents.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ListHeader { Text("分区") } }

        when (val categoryState = categories) {
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
                    text = categoryState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp),
                )
            }

            is UiState.Success -> item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    categoryState.data.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { category ->
                                CategoryChip(
                                    category = category,
                                    selected = selected?.id == category.id,
                                    onClick = { viewModel.select(category) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = selected?.name?.let { "「$it」" } ?: "",
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        when (val contentState = contents) {
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
                    text = contentState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp),
                )
            }

            is UiState.Success -> {
                val list = contentState.data
                if (list.isEmpty()) {
                    item {
                        Text(
                            text = "该分区暂无内容",
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
private fun CategoryChip(
    category: CategoryItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Chip(
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = category.name,
                style = MaterialTheme.typography.caption2,
                maxLines = 1,
            )
        },
        colors = if (selected) {
            ChipDefaults.primaryChipColors()
        } else {
            ChipDefaults.secondaryChipColors()
        },
    )
}
