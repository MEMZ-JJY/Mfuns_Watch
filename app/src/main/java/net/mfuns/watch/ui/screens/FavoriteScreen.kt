package net.mfuns.watch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import net.mfuns.watch.api.ContentItem
import net.mfuns.watch.api.FavoriteFolderItem
import net.mfuns.watch.ui.FavoriteViewModel
import net.mfuns.watch.ui.UiState
import net.mfuns.watch.ui.components.ContentCard

@Composable
fun FavoriteScreen(
    viewModel: FavoriteViewModel,
    onOpenDetail: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val opened by viewModel.opened.collectAsStateWithLifecycle()

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ListHeader { Text("我的收藏") } }

        when (val folderState = folders) {
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
                    text = folderState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp),
                )
            }

            is UiState.Success -> {
                if (folderState.data.isEmpty()) {
                    item {
                        Text(
                            text = "还没有收藏夹",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        folderState.data.take(3).forEach { folder ->
                            FolderChip(
                                folder = folder,
                                selected = opened?.id == folder.id,
                                onClick = { viewModel.open(folder) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        when (val itemState = items) {
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
                    text = itemState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp),
                )
            }

            is UiState.Success -> {
                if (itemState.data.isEmpty()) {
                    item {
                        Text(
                            text = "收藏夹是空的",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                items(itemState.data) { item ->
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
private fun FolderChip(
    folder: FavoriteFolderItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Chip(
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = "${folder.name}(${folder.count})",
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
