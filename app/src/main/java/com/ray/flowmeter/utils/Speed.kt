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
        return when {
            (bytes >= (1024L * 1024L * 1024L)) -> {
                val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                if (gb % 1.0 == 0.0) String.format(Locale.getDefault(), "%.0f GB", gb)
                else String.format(Locale.getDefault(), "%.2f GB", gb)
            }
            (bytes >= (1024L * 1024L)) -> {
                val mb = bytes / (1024.0 * 1024.0)
                if (mb % 1.0 == 0.0) String.format(Locale.getDefault(), "%.0f MB", mb)
                else String.format(Locale.getDefault(), "%.2f MB", mb)
            }
            (bytes >= 1024L) -> {
                val kb = bytes / 1024.0
                if (kb % 1.0 == 0.0) String.format(Locale.getDefault(), "%.0f KB", kb)
                else String.format(Locale.getDefault(), "%.2f KB", kb)
            }
            else -> "$bytes B"
        }
    }
}
