package com.ray.flowmeter.utils

import java.util.Locale

// Converts raw byte values into readable speed
object SpeedFormatter {

    // Converts bytes/sec to byte-based units (KB/s, MB/s, GB/s)
    fun formatBytes(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1_000_000_000 ->
                String.format(Locale.getDefault(), "%.1f GB/s", bytesPerSecond / 1_000_000_000.0)

            bytesPerSecond >= 1_000_000 ->
                String.format(Locale.getDefault(), "%.1f MB/s", bytesPerSecond / 1_000_000.0)

            bytesPerSecond >= 1_000 ->
                String.format(Locale.getDefault(), "%.0f KB/s", bytesPerSecond / 1_000.0)

            else -> "0 KB/s"
        }
    }

    fun formatUsage(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            else -> String.format(Locale.getDefault(), "%.1f KB", kb)
        }
    }
}
