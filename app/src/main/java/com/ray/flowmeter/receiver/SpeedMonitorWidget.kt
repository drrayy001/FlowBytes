package com.ray.flowmeter.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.widget.RemoteViews
import com.ray.flowmeter.MainActivity
import com.ray.flowmeter.R
import java.util.Locale

// Speed Monitor Widget displaying live download and upload speeds with curve graphs
class SpeedMonitorWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == DailyUsageWidget.ACTION_UPDATE_WIDGET) {
            val rxSpeed = intent.getLongExtra(DailyUsageWidget.EXTRA_RX_SPEED, 0L)
            val txSpeed = intent.getLongExtra(DailyUsageWidget.EXTRA_TX_SPEED, 0L)
            
            val rxMbps = (rxSpeed * 8f) / 1_000_000f
            val txMbps = (txSpeed * 8f) / 1_000_000f

            synchronized(lock) {
                downloadHistory.add(rxMbps)
                if (downloadHistory.size > 12) downloadHistory.removeAt(0)

                uploadHistory.add(txMbps)
                if (uploadHistory.size > 12) uploadHistory.removeAt(0)
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SpeedMonitorWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateWidgetData(context, appWidgetManager, appWidgetId, rxMbps, txMbps)
            }
        }
    }

    companion object {
        private val lock = Any()
        private val downloadHistory = mutableListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        private val uploadHistory = mutableListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val lastRx = synchronized(lock) { downloadHistory.lastOrNull() ?: 0f }
            val lastTx = synchronized(lock) { uploadHistory.lastOrNull() ?: 0f }
            updateWidgetData(context, appWidgetManager, appWidgetId, lastRx, lastTx)
        }

        private fun updateWidgetData(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            rxMbps: Float,
            txMbps: Float
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_speed_monitor)

            // Set main launch intent
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            // Format values exactly like the screenshot: e.g., "45.2" and "12.8"
            views.setTextViewText(R.id.widget_monitor_text_down, String.format(Locale.US, "%.1f", rxMbps))
            views.setTextViewText(R.id.widget_monitor_text_up, String.format(Locale.US, "%.1f", txMbps))

            // Draw wave graphs
            val downHistoryCopy = synchronized(lock) { downloadHistory.toList() }
            val upHistoryCopy = synchronized(lock) { uploadHistory.toList() }

            val downBitmap = drawSpeedGraph(downHistoryCopy, "#4DE8F4")
            val upBitmap = drawSpeedGraph(upHistoryCopy, "#DDA7FF")

            views.setImageViewBitmap(R.id.widget_monitor_graph_down, downBitmap)
            views.setImageViewBitmap(R.id.widget_monitor_graph_up, upBitmap)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun drawSpeedGraph(speeds: List<Float>, colorHex: String): Bitmap {
            val width = 240
            val height = 90
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawColor(android.graphics.Color.TRANSPARENT)

            if (speeds.isEmpty()) return bitmap

            val maxVal = speeds.maxOrNull()?.coerceAtLeast(1.0f) ?: 1.0f

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(colorHex)
                style = Paint.Style.STROKE
                strokeWidth = 5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val path = Path()
            val dx = width.toFloat() / (speeds.size - 1).coerceAtLeast(1)

            fun getY(value: Float): Float {
                val fraction = value / maxVal
                return height - 10f - fraction * (height - 20f)
            }

            path.moveTo(0f, getY(speeds[0]))
            for (i in 1 until speeds.size) {
                val x0 = (i - 1) * dx
                val y0 = getY(speeds[i - 1])
                val x1 = i * dx
                val y1 = getY(speeds[i])

                val cx1 = x0 + dx / 2f
                val cy1 = y0
                val cx2 = x1 - dx / 2f
                val cy2 = y1
                path.cubicTo(cx1, cy1, cx2, cy2, x1, y1)
            }

            // Draw filled gradient area under the curve
            val fillPath = Path(path)
            fillPath.lineTo(width.toFloat(), height.toFloat())
            fillPath.lineTo(0f, height.toFloat())
            fillPath.close()

            val colorInt = android.graphics.Color.parseColor(colorHex)
            val fillAlphaColor = (colorInt and 0x00FFFFFF) or 0x22000000 // 13% opacity gradient fill

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = android.graphics.LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    fillAlphaColor,
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(fillPath, fillPaint)

            // Draw the main curve line
            canvas.drawPath(path, paint)

            return bitmap
        }
    }
}
