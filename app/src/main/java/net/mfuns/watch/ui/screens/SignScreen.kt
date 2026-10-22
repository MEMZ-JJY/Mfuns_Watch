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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import net.mfuns.watch.api.SignInfo
import net.mfuns.watch.ui.SignViewModel
import net.mfuns.watch.ui.UiState

@Composable
fun SignScreen(
    viewModel: SignViewModel,
    modifier: Modifier = Modifier,
) {
    val info by viewModel.info.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.consumeMessage()
        }
    }

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ListHeader { Text("每日签到") } }

        when (val infoState = info) {
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
                    text = infoState.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                )
            }

            is UiState.Success -> {
                val data = infoState.data
                item { SignSummary(data) }
                item { SignCalendar(data) }
                item {
                    Button(onClick = viewModel::sign) {
                        Text("立即签到")
                    }
                }
                if (message != null) {
                    item {
                        Text(
                            text = message!!,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignSummary(info: SignInfo) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatRow("本月签到", info.monthTimes)
        StatRow("累计签到", info.allTimes)
        StatRow("连签", info.signedDays.size)
    }
}

@Composable
private fun SignCalendar(info: SignInfo) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "本月已签",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (info.signedDays.isEmpty()) {
                "还没有签到记录"
            } else {
                info.signedDays.sorted().joinToString("、") { "${it}日" }
            },
            style = MaterialTheme.typography.body2,
        )
    }
}
