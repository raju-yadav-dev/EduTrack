package com.raju.edutrack.update

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val assetName: String,
    val downloadUrl: String,
    val publishedAt: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val release: ReleaseInfo,
        val currentVersion: String
    ) : UpdateCheckResult()

    data class UpToDate(
        val currentVersion: String
    ) : UpdateCheckResult()

    data class NotConfigured(
        val message: String
    ) : UpdateCheckResult()

    data class Failed(
        val message: String
    ) : UpdateCheckResult()
}

sealed class UpdateInstallResult {
    object Started : UpdateInstallResult()
    object NeedsPermission : UpdateInstallResult()
    data class Failed(val message: String) : UpdateInstallResult()
}

object UpdateManager {

    suspend fun checkForUpdate(
        context: Context
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!UpdateConfig.isConfigured) {
            return@withContext UpdateCheckResult.NotConfigured(
                "Set UpdateConfig.owner and UpdateConfig.repo"
            )
        }

        val currentVersion = getCurrentVersionName(context)

        val release = try {
            fetchLatestRelease(
                UpdateConfig.owner,
                UpdateConfig.repo,
                UpdateConfig.assetNameContains
            )
        } catch (error: IOException) {
            return@withContext UpdateCheckResult.Failed(
                "Network error: ${error.message ?: "unknown"}"
            )
        } catch (error: IllegalStateException) {
            return@withContext UpdateCheckResult.Failed(
                error.message ?: "Invalid release data"
            )
        }

        return@withContext if (
            isNewerVersion(release.tagName, currentVersion)
        ) {
            UpdateCheckResult.UpdateAvailable(
                release = release,
                currentVersion = currentVersion
            )
        } else {
            UpdateCheckResult.UpToDate(currentVersion)
        }
    }

    suspend fun installUpdate(
        activity: Activity,
        downloadUrl: String
    ): UpdateInstallResult = withContext(Dispatchers.IO) {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
            return@withContext UpdateInstallResult.NeedsPermission
        }

        val packageInstaller =
            activity.packageManager.packageInstaller

        val sessionParams = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(activity.packageName)
        }

        val sessionId = try {
            packageInstaller.createSession(sessionParams)
        } catch (error: IOException) {
            return@withContext UpdateInstallResult.Failed(
                "Failed to create install session"
            )
        }

        val session = packageInstaller.openSession(sessionId)

        try {
            val connection = URL(downloadUrl)
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true

            if (connection.responseCode !in 200..299) {
                session.abandon()
                return@withContext UpdateInstallResult.Failed(
                    "Download failed: HTTP ${connection.responseCode}"
                )
            }

            BufferedInputStream(connection.inputStream).use { input ->
                session.openWrite("update", 0, -1)
                    .use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
            }
        } catch (error: IOException) {
            session.abandon()
            return@withContext UpdateInstallResult.Failed(
                "Download failed: ${error.message ?: "unknown"}"
            )
        }

        val intent = Intent(
            activity,
            UpdateInstallReceiver::class.java
        )
        val flags = if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            sessionId,
            intent,
            flags
        )

        session.commit(pendingIntent.intentSender)
        session.close()

        UpdateInstallResult.Started
    }

    private fun fetchLatestRelease(
        owner: String,
        repo: String,
        assetNameContains: String
    ): ReleaseInfo {
        val url = URL(
            "https://api.github.com/repos/$owner/$repo/releases/latest"
        )
        val connection = url
            .openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty(
            "Accept",
            "application/vnd.github+json"
        )

        if (connection.responseCode !in 200..299) {
            throw IOException(
                "GitHub API error: ${connection.responseCode}"
            )
        }

        val body = connection.inputStream
            .bufferedReader()
            .readText()
        val json = JSONObject(body)
        val assets = json.getJSONArray("assets")

        var assetName = ""
        var downloadUrl = ""

        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.getString("name")
            if (
                name.contains(assetNameContains) &&
                    name.endsWith(".apk")
            ) {
                assetName = name
                downloadUrl =
                    asset.getString("browser_download_url")
                break
            }
        }

        if (downloadUrl.isBlank()) {
            throw IllegalStateException(
                "No APK asset found in latest release"
            )
        }

        return ReleaseInfo(
            tagName = json.getString("tag_name"),
            name = json.optString("name", ""),
            assetName = assetName,
            downloadUrl = downloadUrl,
            publishedAt = json.optString("published_at", "")
        )
    }

    private fun getCurrentVersionName(context: Context): String {
        val packageManager = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
        return info.versionName ?: "0"
    }

    private fun isNewerVersion(
        latest: String,
        current: String
    ): Boolean {
        val latestParts = parseVersion(latest)
        val currentParts = parseVersion(current)
        val maxSize = maxOf(latestParts.size, currentParts.size)

        for (index in 0 until maxSize) {
            val latestValue = latestParts.getOrElse(index) { 0 }
            val currentValue = currentParts.getOrElse(index) { 0 }
            if (latestValue != currentValue) {
                return latestValue > currentValue
            }
        }

        return false
    }

    private fun parseVersion(value: String): List<Int> {
        val cleaned = value.trim()
            .removePrefix("v")
            .removePrefix("V")
        val matches = Regex("\\d+")
            .findAll(cleaned)
            .map { it.value.toInt() }
            .toList()
        return if (matches.isEmpty()) listOf(0) else matches
    }
}
