package com.example.data.repository

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import com.example.model.AppStorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppStorageInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = ArrayList<AppStorageInfo>(packages.size)

        var storageStatsManager: StorageStatsManager? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
            } catch (e: Exception) {
                // Ignore if unavailable
            }
        }

        for (app in packages) {
            // Ignore system framework apps unless requested
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val appName = try {
                pm.getApplicationLabel(app).toString()
            } catch (e: Exception) {
                app.packageName
            }

            var codeSize = 0L
            var dataSize = 0L
            var cacheSize = 0L

            // 1. Try StorageStatsManager
            var statsRetrieved = false
            if (storageStatsManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
                    val uuid = storageManager?.getUuidForPath(File(app.sourceDir)) ?: StorageManager.UUID_DEFAULT
                    val stats = storageStatsManager.queryStatsForPackage(uuid, app.packageName, Process.myUserHandle())
                    codeSize = stats.appBytes
                    dataSize = stats.dataBytes
                    cacheSize = stats.cacheBytes
                    statsRetrieved = true
                } catch (e: Exception) {
                    statsRetrieved = false
                }
            }

            // 2. Fallback to APK file length
            if (!statsRetrieved) {
                val apkFile = File(app.sourceDir)
                if (apkFile.exists()) {
                    codeSize = apkFile.length()
                }
                // Estimate app external cache directory if available
                val externalCache = File(context.getExternalFilesDir(null)?.parentFile?.parentFile, app.packageName + "/cache")
                if (externalCache.exists()) {
                    cacheSize = getFolderSize(externalCache)
                }
            }

            val totalSize = codeSize + dataSize + cacheSize

            result.add(
                AppStorageInfo(
                    packageName = app.packageName,
                    appName = appName,
                    codeSize = codeSize,
                    dataSize = dataSize,
                    cacheSize = cacheSize,
                    totalSize = totalSize,
                    isSystemApp = isSystem,
                    isProtected = isSystem || app.packageName == context.packageName
                )
            )
        }

        result.sortedByDescending { it.totalSize }
    }

    fun openAppDetailsSettings(packageName: String): Intent {
        return Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun openInternalStorageSettings(): Intent {
        return Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getUninstallIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    suspend fun clearAppCache(packageName: String): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        if (packageName == context.packageName) {
            freedBytes += deleteDirContents(context.cacheDir)
            freedBytes += deleteDirContents(context.codeCacheDir)
            context.externalCacheDir?.let { freedBytes += deleteDirContents(it) }
        } else {
            // Attempt to clean accessible external cache folders
            try {
                val externalCache = File(context.getExternalFilesDir(null)?.parentFile?.parentFile, "$packageName/cache")
                if (externalCache.exists()) {
                    freedBytes += deleteDirContents(externalCache)
                }
            } catch (e: Exception) {
                // Restricted on newer Android without root
            }
        }
        freedBytes
    }

    suspend fun clearMultipleAppsCache(packageNames: List<String>): Long = withContext(Dispatchers.IO) {
        var totalFreed = 0L
        for (pkg in packageNames) {
            totalFreed += clearAppCache(pkg)
        }
        totalFreed
    }

    private fun deleteDirContents(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var deletedBytes = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            if (file.isDirectory) {
                deletedBytes += deleteDirContents(file)
                file.delete()
            } else {
                val len = file.length()
                if (file.delete()) {
                    deletedBytes += len
                }
            }
        }
        return deletedBytes
    }

    private fun getFolderSize(folder: File): Long {
        var size = 0L
        val files = folder.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return size
    }
}
