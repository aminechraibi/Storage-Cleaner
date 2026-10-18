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
                    val uuid = try {
                        app.storageUuid ?: StorageManager.UUID_DEFAULT
                    } catch (e: Throwable) {
                        try {
                            storageManager?.getUuidForPath(File(app.sourceDir)) ?: StorageManager.UUID_DEFAULT
                        } catch (e2: Throwable) {
                            StorageManager.UUID_DEFAULT
                        }
                    }

                    val stats = try {
                        storageStatsManager.queryStatsForPackage(uuid, app.packageName, Process.myUserHandle())
                    } catch (e: Exception) {
                        if (uuid != StorageManager.UUID_DEFAULT) {
                            storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, app.packageName, Process.myUserHandle())
                        } else {
                            throw e
                        }
                    }

                    codeSize = stats.appBytes
                    cacheSize = stats.cacheBytes
                    val rawData = stats.dataBytes
                    dataSize = (rawData - cacheSize).coerceAtLeast(0L)
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
                } else if (!app.publicSourceDir.isNullOrEmpty()) {
                    val publicApk = File(app.publicSourceDir)
                    if (publicApk.exists()) {
                        codeSize = publicApk.length()
                    }
                }
                // Estimate app external cache directory if accessible
                try {
                    val externalCache = File(context.getExternalFilesDir(null)?.parentFile?.parentFile, "${app.packageName}/cache")
                    if (externalCache.exists()) {
                        cacheSize = getFolderSize(externalCache)
                    }
                } catch (e: Exception) {
                    // Restricted on newer Android without root
                }
            }

            val installTime = try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(app.packageName, 0)
                }
                packageInfo.firstInstallTime
            } catch (e: Exception) {
                0L
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
                    isProtected = isSystem || app.packageName == context.packageName,
                    installTime = installTime
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
        return Intent(Intent.ACTION_DELETE).apply {
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

    suspend fun clearAppData(packageName: String): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        if (packageName == context.packageName) {
            freedBytes += deleteDirContents(context.filesDir)
            freedBytes += deleteDirContents(context.cacheDir)
            freedBytes += deleteDirContents(context.codeCacheDir)
            context.externalCacheDir?.let { freedBytes += deleteDirContents(it) }
            context.getExternalFilesDir(null)?.let { freedBytes += deleteDirContents(it) }
        } else {
            try {
                val externalDir = File(context.getExternalFilesDir(null)?.parentFile?.parentFile, packageName)
                if (externalDir.exists()) {
                    freedBytes += deleteDirContents(externalDir)
                }
            } catch (e: Exception) {
                // Restricted on Android 11+
            }
        }
        freedBytes
    }

    suspend fun clearMultipleAppsData(packageNames: List<String>): Long = withContext(Dispatchers.IO) {
        var totalFreed = 0L
        for (pkg in packageNames) {
            totalFreed += clearAppData(pkg)
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
