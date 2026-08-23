package com.example.model

enum class CleanCategory {
    SAFE_JUNK,
    REVIEW,
    SENSITIVE
}

enum class FileType {
    APK,
    DOWNLOAD,
    LARGE,
    OLD,
    DUPLICATE,
    SIMILAR_PHOTO,
    RESIDUAL,
    EMPTY_FOLDER,
    CACHE,
    SCREENSHOT,
    MEDIA_IMAGE,
    MEDIA_VIDEO,
    MEDIA_AUDIO,
    DOCUMENT,
    ARCHIVE,
    OTHER
}

data class StorageItem(
    val id: Long = 0,
    val uri: String = "",
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String = "",
    val extension: String = "",
    val category: CleanCategory = CleanCategory.REVIEW,
    val type: FileType = FileType.OTHER,
    val hash: String? = null,
    val partialHash: String? = null,
    val perceptualHash: Long? = null,
    val isProtected: Boolean = false,
    val isSelected: Boolean = false,
    val groupId: String? = null,
    val isDirectory: Boolean = false,
    val itemCount: Int = 0
)

data class DeviceStorageStats(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val safeCleanableBytes: Long = 0L,
    val reviewCleanableBytes: Long = 0L,
    val sensitiveCleanableBytes: Long = 0L,
    val scannedFilesCount: Int = 0,
    val duplicatesCount: Int = 0,
    val duplicateBytes: Long = 0L,
    val similarPhotosCount: Int = 0,
    val largeFilesCount: Int = 0,
    val oldFilesCount: Int = 0,
    val residualCount: Int = 0,
    val emptyFoldersCount: Int = 0,
    val apksCount: Int = 0
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class AppStorageInfo(
    val packageName: String,
    val appName: String,
    val codeSize: Long = 0L,
    val dataSize: Long = 0L,
    val cacheSize: Long = 0L,
    val totalSize: Long = 0L,
    val isSystemApp: Boolean = false,
    val isProtected: Boolean = false,
    val lastUsedTime: Long = 0L,
    val isSelected: Boolean = false
)

data class DuplicateGroup(
    val hash: String,
    val singleSize: Long,
    val wastedBytes: Long,
    val items: List<StorageItem>
)

data class SimilarPhotoGroup(
    val groupId: String,
    val similarityScore: Int,
    val totalBytes: Long,
    val items: List<StorageItem>
)

data class CleanupResult(
    val freedBytes: Long,
    val itemsDeleted: Int,
    val failedCount: Int,
    val beforeFreeBytes: Long,
    val afterFreeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val deletedCategories: Map<CleanCategory, Long> = emptyMap()
)

enum class ExclusionType {
    PATH,
    EXTENSION,
    FOLDER_NAME,
    KEYWORD
}

data class Exclusion(
    val id: Long = 0,
    val pattern: String,
    val type: ExclusionType,
    val enabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val description: String = ""
)

data class RecycledItem(
    val id: Long = 0,
    val originalPath: String,
    val trashPath: String,
    val name: String,
    val size: Long,
    val recycledAt: Long = System.currentTimeMillis(),
    val mimeType: String = "",
    val isSelected: Boolean = false
)

data class CleanHistory(
    val id: Long = 0,
    val timestamp: Long,
    val bytesFreed: Long,
    val itemCount: Int,
    val categorySummary: String
)

data class ScanProgress(
    val isScanning: Boolean = false,
    val currentPath: String = "",
    val scannedCount: Int = 0,
    val scannedBytes: Long = 0L,
    val phase: String = "Idle",
    val percent: Float = 0f,
    val isDeepScan: Boolean = false
)

enum class SortField {
    SIZE,
    DATE,
    NAME
}

enum class SortDirection {
    DESCENDING,
    ASCENDING
}

data class FilterCriteria(
    val searchQuery: String = "",
    val category: CleanCategory? = null,
    val fileType: FileType? = null,
    val minSizeBytes: Long = 0L,
    val maxSizeBytes: Long = Long.MAX_VALUE,
    val sortField: SortField = SortField.SIZE,
    val sortDirection: SortDirection = SortDirection.DESCENDING
)
