package net.mfuns.watch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val MfunsPink = Color(0xFFFB7299)
val MfunsPinkDark = Color(0xFFC7537E)
val MfunsPinkLight = Color(0xFFFFB3C9)

private val MfunsColors = Colors(
    primary = MfunsPink,
    primaryVariant = MfunsPinkDark,
    secondary = MfunsPinkLight,
    background = Color.Black,
    surface = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB8B8BE),
    error = Color(0xFFFF6B6B),
)

@Composable
fun MfunsWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = MfunsColors,
        content = content,
    )
}
