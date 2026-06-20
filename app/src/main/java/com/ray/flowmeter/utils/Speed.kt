// Utility class providing byte parsing, speed delta calculations, and formatted string outputs.
package com.ray.flowmeter.utils

import java.util.Locale

object SpeedFormatter {

    private fun localizeDigits(input: String, isAr: Boolean): String {
        if (!isAr) return input
        val builder = StringBuilder()
        for (char in input) {
            when (char) {
                in '0'..'9' -> {
                    builder.append((char.code - '0'.code + '٠'.code).toChar())
                }
                '.' -> {
                    builder.append('٫')
                }
                else -> {
                    builder.append(char)
                }
            }
        }
        return builder.toString()
    }

    fun formatBytes(bytesPerSecond: Long): String {
        val locale = Locale.getDefault()
        val isAr = locale.language == "ar"
        val formatted = when {
            bytesPerSecond >= 1_000_000_000 -> {
                val unit = if (isAr) "ج.ب/ث" else "GB/s"
                String.format(locale, "%.1f $unit", bytesPerSecond / 1_000_000_000.0)
            }

            bytesPerSecond >= 1_000_000 -> {
                val unit = if (isAr) "م.ب/ث" else "MB/s"
                String.format(locale, "%.1f $unit", bytesPerSecond / 1_000_000.0)
            }

            bytesPerSecond >= 1_000 -> {
                val unit = if (isAr) "ك.ب/ث" else "KB/s"
                String.format(locale, "%.0f $unit", bytesPerSecond / 1_000.0)
            }

            else -> {
                val unit = if (isAr) "ك.ب/ث" else "KB/s"
                String.format(locale, "0 $unit")
            }
        }
        return localizeDigits(formatted, isAr)
    }

    fun formatUsage(bytes: Long): String {
        val locale = Locale.getDefault()
        val isAr = locale.language == "ar"
        val formatted = when {
            (bytes >= (1024L * 1024L * 1024L)) -> {
                val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                val unit = if (isAr) "ج.ب" else "GB"
                if (gb % 1.0 == 0.0) String.format(locale, "%.0f $unit", gb)
                else String.format(locale, "%.2f $unit", gb)
            }
            (bytes >= (1024L * 1024L)) -> {
                val mb = bytes / (1024.0 * 1024.0)
                val unit = if (isAr) "م.ب" else "MB"
                if (mb % 1.0 == 0.0) String.format(locale, "%.0f $unit", mb)
                else String.format(locale, "%.2f $unit", mb)
            }
            (bytes >= 1024L) -> {
                val kb = bytes / 1024.0
                val unit = if (isAr) "ك.ب" else "KB"
                if (kb % 1.0 == 0.0) String.format(locale, "%.0f $unit", kb)
                else String.format(locale, "%.2f $unit", kb)
            }
            else -> {
                val unit = if (isAr) "ب" else "B"
                String.format(locale, "%d $unit", bytes)
            }
        }
        return localizeDigits(formatted, isAr)
    }
}
