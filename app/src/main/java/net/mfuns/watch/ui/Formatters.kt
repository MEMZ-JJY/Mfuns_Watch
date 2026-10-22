package net.mfuns.watch.ui

import java.util.Calendar
import java.util.Date

fun formatCount(n: Int): String = when {
    n <= 0 -> "0"
    n < 10000 -> "$n"
    n < 100000000 -> String.format("%.1f万", n / 10000.0)
    else -> String.format("%.1f亿", n / 100000000.0)
}

fun formatTime(millis: Long?): String {
    if (millis == null || millis <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - millis
    if (diff < 0) return formatDate(millis)
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        diff < 2592000_000L -> "${diff / 86400_000}天前"
        else -> formatDate(millis)
    }
}

private fun formatDate(millis: Long): String {
    val cal = Calendar.getInstance().apply { time = Date(millis) }
    return String.format(
        "%02d-%02d",
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}
