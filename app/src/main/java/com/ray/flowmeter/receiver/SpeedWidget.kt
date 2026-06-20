package com.ray.flowmeter.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.os.Bundle
import android.widget.RemoteViews
import com.ray.flowmeter.MainActivity
import com.ray.flowmeter.R
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.utils.SpeedFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class SpeedWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SpeedWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val rxSpeed = intent.getLongExtra(EXTRA_RX_SPEED, 0L)
            val txSpeed = intent.getLongExtra(EXTRA_TX_SPEED, 0L)
            val usageBytes = intent.getLongExtra(EXTRA_USAGE, 0L)
            val usageType = intent.getStringExtra(EXTRA_USAGE_TYPE) ?: "DAILY"
            val showSpeed = intent.getBooleanExtra(EXTRA_SHOW_SPEED, true)

            for (appWidgetId in appWidgetIds) {
                updateWidgetData(context, appWidgetManager, appWidgetId, rxSpeed, txSpeed, usageBytes, usageType, showSpeed)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.ray.flowmeter.ACTION_UPDATE_WIDGET"
        const val EXTRA_RX_SPEED = "extra_rx_speed"
        const val EXTRA_TX_SPEED = "extra_tx_speed"
        const val EXTRA_USAGE = "extra_usage"
        const val EXTRA_USAGE_TYPE = "extra_usage_type"
        const val EXTRA_SHOW_SPEED = "extra_show_speed"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72)
            
            val layoutResId = if (minHeight < 100) {
                R.layout.widget_speed_usage_compact
            } else {
                R.layout.widget_speed_usage
            }
            val views = RemoteViews(context.packageName, layoutResId)
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val repository = UserPreferencesRepository(context)
                val showSpeed = repository.widgetShowSpeed.first()
                
                withContext(Dispatchers.Main) {
                    views.setViewVisibility(R.id.widget_speed_layout, if (showSpeed) View.VISIBLE else View.GONE)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun updateWidgetData(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            rxSpeed: Long,
            txSpeed: Long,
            usageBytes: Long,
            usageType: String,
            showSpeed: Boolean
        ) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72)
            
            val layoutResId = if (minHeight < 100) {
                R.layout.widget_speed_usage_compact
            } else {
                R.layout.widget_speed_usage
            }
            val views = RemoteViews(context.packageName, layoutResId)
            
            views.setTextViewText(R.id.widget_text_down, SpeedFormatter.formatBytes(rxSpeed))
            views.setTextViewText(R.id.widget_text_up, SpeedFormatter.formatBytes(txSpeed))
            views.setTextViewText(R.id.widget_text_usage, SpeedFormatter.formatUsage(usageBytes))
            
            val labelRes = if (usageType == "MONTHLY") R.string.label_this_month else R.string.label_todays_usage
            views.setTextViewText(R.id.widget_label_usage, context.getString(labelRes).uppercase())

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            views.setViewVisibility(R.id.widget_speed_layout, if (showSpeed) View.VISIBLE else View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
