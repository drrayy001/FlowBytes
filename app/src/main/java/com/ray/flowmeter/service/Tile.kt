package com.ray.flowmeter.service

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ray.flowmeter.R
import com.ray.flowmeter.data.UserPreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class SpeedTileService : TileService() {

    override fun attachBaseContext(newBase: Context) {
        val repository = UserPreferencesRepository(newBase)
        val languageCode = kotlinx.coroutines.runBlocking {
            try {
                repository.language.first()
            } catch (e: Exception) {
                ""
            }
        }
        val context = com.ray.flowmeter.utils.LocaleHelper.applyLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        repository = UserPreferencesRepository(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val currentState = repository.monitoringEnabled.first()
            val newState = !currentState
            repository.setMonitoringEnabled(newState)

            val intent = Intent(this@SpeedTileService, NetworkMonitoringService::class.java)
            if (newState) {
                startForegroundService(intent)
            } else {
                stopService(intent)
            }

            updateTile()
        }
    }

    private fun updateTile() {
        serviceScope.launch {
            val isEnabled = repository.monitoringEnabled.first()
            val tile = qsTile ?: return@launch

            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = if (isEnabled) getString(R.string.tile_monitoring_on) else getString(R.string.tile_monitoring_off)
            tile.subtitle = getString(R.string.app_name)
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
