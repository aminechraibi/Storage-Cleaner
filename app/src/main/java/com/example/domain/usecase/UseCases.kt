package com.example.domain.usecase

import com.example.data.local.SettingsDataStore
import com.example.data.repository.AppRepository
import com.example.data.repository.ExclusionRepository
import com.example.data.repository.HistoryRepository
import com.example.data.repository.RecycleBinRepository
import com.example.data.repository.StorageRepository
import com.example.model.AppStorageInfo
import com.example.model.CleanCategory
import com.example.model.CleanHistory
import com.example.model.CleanupResult
import com.example.model.DeviceStorageStats
import com.example.model.DuplicateGroup
import com.example.model.Exclusion
import com.example.model.ExclusionType
import com.example.model.FileType
import com.example.model.FilterCriteria
import com.example.model.RecycledItem
import com.example.model.ScanProgress
import com.example.model.SimilarPhotoGroup
import com.example.model.StorageItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ScanStorageUseCase(private val storageRepository: StorageRepository) {
    operator fun invoke(deepScan: Boolean = false): Flow<ScanProgress> {
        return storageRepository.scanStorage(deepScan)
    }
}

class GetStorageStatsUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(): DeviceStorageStats {
        return storageRepository.getStorageStats()
    }
}

class FindDuplicatesUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(): List<DuplicateGroup> {
        return storageRepository.findDuplicates()
    }
}

class FindSimilarPhotosUseCase(
    private val storageRepository: StorageRepository,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(): List<SimilarPhotoGroup> {
        val threshold = settingsDataStore.similarPhotoThreshold.first()
        return storageRepository.findSimilarPhotos(threshold)
    }
}

class FindLargeFilesUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(limit: Int = 100): List<StorageItem> {
        return storageRepository.getTopKLargeFiles(limit)
    }
}

class FindOldFilesUseCase(
    private val storageRepository: StorageRepository,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(): List<StorageItem> {
        val days = settingsDataStore.oldFileThresholdDays.first()
        val timestamp = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        return storageRepository.getOldFiles(timestamp)
    }
}

class FindResidualAndEmptyFoldersUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(): Pair<List<StorageItem>, List<StorageItem>> {
        val residuals = storageRepository.getFilesByType(FileType.RESIDUAL)
        val emptyFolders = storageRepository.getFilesByType(FileType.EMPTY_FOLDER)
        return residuals to emptyFolders
    }
}

class GetCategoryFilesUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(category: CleanCategory): List<StorageItem> {
        return storageRepository.getFilesByCategory(category)
    }
}

class GetTypeFilesUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(fileType: FileType): List<StorageItem> {
        return storageRepository.getFilesByType(fileType)
    }
}

class PerformCleanupUseCase(
    private val storageRepository: StorageRepository,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(items: List<StorageItem>): CleanupResult {
        val recycleBinEnabled = settingsDataStore.recycleBinEnabled.first()
        return storageRepository.performCleanup(items, useRecycleBin = recycleBinEnabled)
    }
}

class SearchFilesUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(criteria: FilterCriteria): List<StorageItem> {
        return storageRepository.searchFiles(criteria)
    }
}

class ScanAppsUseCase(private val appRepository: AppRepository) {
    suspend operator fun invoke(): List<AppStorageInfo> {
        return appRepository.getInstalledApps()
    }
}

class ClearAppCacheUseCase(private val appRepository: AppRepository) {
    suspend fun clearSingle(packageName: String): Long {
        return appRepository.clearAppCache(packageName)
    }

    suspend fun clearMultiple(packageNames: List<String>): Long {
        return appRepository.clearMultipleAppsCache(packageNames)
    }
}

class RecycleBinUseCases(private val recycleBinRepository: RecycleBinRepository) {
    val recycledItemsFlow: Flow<List<RecycledItem>> = recycleBinRepository.recycledItemsFlow
    val totalRecycledSizeFlow: Flow<Long> = recycleBinRepository.totalRecycledSizeFlow

    suspend fun restore(item: RecycledItem): Boolean = recycleBinRepository.restoreItem(item)
    suspend fun deletePermanently(item: RecycledItem): Boolean = recycleBinRepository.deletePermanently(item)
    suspend fun emptyRecycleBin(): Long = recycleBinRepository.emptyRecycleBin()
}

class ExclusionUseCases(private val exclusionRepository: ExclusionRepository) {
    val exclusionsFlow: Flow<List<Exclusion>> = exclusionRepository.exclusionsFlow

    suspend fun addExclusion(pattern: String, type: ExclusionType, description: String = ""): Long {
        return exclusionRepository.addExclusion(pattern, type, description)
    }

    suspend fun removeExclusion(id: Long) {
        exclusionRepository.removeExclusion(id)
    }

    suspend fun toggleExclusion(id: Long, enabled: Boolean) {
        exclusionRepository.setExclusionEnabled(id, enabled)
    }
}

class HistoryUseCases(private val historyRepository: HistoryRepository) {
    val historyFlow: Flow<List<CleanHistory>> = historyRepository.historyFlow
    val totalFreedBytesFlow: Flow<Long> = historyRepository.totalFreedBytesFlow

    suspend fun recordCleanup(bytesFreed: Long, itemCount: Int, categorySummary: String): Long {
        return historyRepository.recordCleanup(bytesFreed, itemCount, categorySummary)
    }

    suspend fun clearHistory() {
        historyRepository.clearHistory()
    }
}
