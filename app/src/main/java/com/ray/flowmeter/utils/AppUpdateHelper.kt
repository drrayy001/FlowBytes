package com.ray.flowmeter.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.ray.flowmeter.BuildConfig
import com.ray.flowmeter.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

sealed class UpdateResult {
    object NoUpdateAvailable : UpdateResult()
    data class PlayStoreUpdateAvailable(val appUpdateInfo: AppUpdateInfo) : UpdateResult()
    data class GitHubUpdateAvailable(val tag: String, val downloadUrl: String, val releaseNotes: String?) : UpdateResult()
    data class Error(val exception: Throwable) : UpdateResult()
}

@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val body: String? = null
)

class AppUpdateHelper(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {

    fun checkForUpdates(
        forceGitHubCheck: Boolean = false,
        callback: (UpdateResult) -> Unit
    ) {
        val installer = getInstallerPackageName(context)
        Log.d("AppUpdateHelper", "Detected installer package: $installer")

        if (installer == "com.android.vending" && !forceGitHubCheck) {
            checkPlayStore(callback)
        } else {
            checkGitHub(callback)
        }
    }

    private fun checkPlayStore(callback: (UpdateResult) -> Unit) {
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(context)
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    callback(UpdateResult.PlayStoreUpdateAvailable(appUpdateInfo))
                } else {
                    callback(UpdateResult.NoUpdateAvailable)
                }
            }.addOnFailureListener { e ->
                Log.e("AppUpdateHelper", "Play Store update check failed", e)
                callback(UpdateResult.Error(e))
            }
        } catch (e: Exception) {
            Log.e("AppUpdateHelper", "Play Store AppUpdateManager instantiation failed", e)
            callback(UpdateResult.Error(e))
        }
    }

    private fun checkGitHub(callback: (UpdateResult) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.github.com/repos/drrayy001/FlowBytes/releases/latest")
                    .header("User-Agent", "FlowBytes-Android-App")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Server returned code ${response.code}")
                    }
                    val jsonStr = response.body.string()
                    val json = Json { ignoreUnknownKeys = true }
                    val release = json.decodeFromString<GitHubRelease>(jsonStr)
                    
                    val latestVersion = release.tag_name.trim().removePrefix("v")
                    val currentVersion = BuildConfig.VERSION_NAME

                    Log.d("AppUpdateHelper", "GitHub version check: current=$currentVersion, latest=$latestVersion")

                    if (isNewerVersion(currentVersion, latestVersion)) {
                        val ignored = preferencesRepository.ignoredUpdateVersion.first()
                        if (ignored != release.tag_name) {
                            withContext(Dispatchers.Main) {
                                callback(UpdateResult.GitHubUpdateAvailable(
                                    tag = release.tag_name,
                                    downloadUrl = release.html_url,
                                    releaseNotes = release.body
                                ))
                            }
                            return@launch
                        }
                    }
                    withContext(Dispatchers.Main) {
                        callback(UpdateResult.NoUpdateAvailable)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppUpdateHelper", "GitHub update check failed", e)
                withContext(Dispatchers.Main) {
                    callback(UpdateResult.Error(e))
                }
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val currVal = currentParts.getOrElse(i) { 0 }
            val latVal = latestParts.getOrElse(i) { 0 }
            if (latVal > currVal) return true
            if (latVal < currVal) return false
        }
        return false
    }

    internal fun getInstallerPackageName(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (e: Exception) {
            null
        }
    }
}
