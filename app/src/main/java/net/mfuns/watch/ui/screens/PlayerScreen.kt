package net.mfuns.watch.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.wear.compose.material.Text
import net.mfuns.watch.ui.DetailViewModel

@Composable
fun PlayerScreen(
    viewModel: DetailViewModel,
    videoId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(videoId) { viewModel.load(1, videoId) }

    val source = state.sources.firstOrNull()

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            player = null
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(source) {
        if (source == null) return@LaunchedEffect
        val exo = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(source.url))
            playWhenReady = true
            prepare()
        }
        player = exo
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setBackgroundColor(Color.Black.value.toInt())
                }
            },
            update = { view ->
                view.player = player
            },
        )

        androidx.wear.compose.material.Chip(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            colors = androidx.wear.compose.material.ChipDefaults.secondaryChipColors(),
            label = { Text("返回") },
        )
    }
}
