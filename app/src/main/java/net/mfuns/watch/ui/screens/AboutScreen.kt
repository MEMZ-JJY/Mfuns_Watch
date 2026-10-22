package net.mfuns.watch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text

@Composable
fun AboutScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { ListHeader { Text("关于") } }
        item {
            Text(
                text = "Mfuns Watch",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.primary,
            )
        }
        item {
            Text(
                text = "版本 1.0.0\nWear OS 独立应用",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Text(
                text = "数据来源于 Mfuns 社区公开接口（api.mfuns.net），" +
                    "为非官方第三方客户端，仅供学习交流使用。",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        item {
            androidx.wear.compose.material.Chip(
                onClick = onLogout,
                label = { Text("退出登录", color = MaterialTheme.colors.error) },
            )
        }
    }
}
