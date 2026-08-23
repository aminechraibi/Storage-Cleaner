package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.CleanerApplication
import com.example.domain.usecase.ClearAppCacheUseCase
import com.example.domain.usecase.ExclusionUseCases
import com.example.domain.usecase.FindDuplicatesUseCase
import com.example.domain.usecase.FindLargeFilesUseCase
import com.example.domain.usecase.FindOldFilesUseCase
import com.example.domain.usecase.FindResidualAndEmptyFoldersUseCase
import com.example.domain.usecase.FindSimilarPhotosUseCase
import com.example.domain.usecase.GetCategoryFilesUseCase
import com.example.domain.usecase.GetStorageStatsUseCase
import com.example.domain.usecase.GetTypeFilesUseCase
import com.example.domain.usecase.HistoryUseCases
import com.example.domain.usecase.PerformCleanupUseCase
import com.example.domain.usecase.RecycleBinUseCases
import com.example.domain.usecase.ScanAppsUseCase
import com.example.domain.usecase.ScanStorageUseCase
import com.example.domain.usecase.SearchFilesUseCase
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    DASHBOARD,
    SCAN_PROGRESS,
    CLEAN_RESULTS,
    DUPLICATES,
    SIMILAR_PHOTOS,
    APP_MANAGER,
    LARGE_FILES,
    OLD_FILES,
    RESIDUAL_FOLDERS,
    RECYCLE_BIN,
    EXCLUSIONS,
    SEARCH,
    HISTORY,
    SETTINGS
}

class MainViewModel(
    private val scanStorageUseCase: ScanStorageUseCase,
    private val getStorageStatsUseCase: GetStorageStatsUseCase,
    private val getCategoryFilesUseCase: GetCategoryFilesUseCase,
    private val getTypeFilesUseCase: GetTypeFilesUseCase,
    private val findDuplicatesUseCase: FindDuplicatesUseCase,
    private val findSimilarPhotosUseCase: FindSimilarPhotosUseCase,
    private val findLargeFilesUseCase: FindLargeFilesUseCase,
    private val findOldFilesUseCase: FindOldFilesUseCase,
    private val findResidualAndEmptyFoldersUseCase: FindResidualAndEmptyFoldersUseCase,
    private val scanAppsUseCase: ScanAppsUseCase,
    private val clearAppCacheUseCase: ClearAppCacheUseCase,
    private val performCleanupUseCase: PerformCleanupUseCase,
    private val searchFilesUseCase: SearchFilesUseCase,
    private val recycleBinUseCases: RecycleBinUseCases,
    private val exclusionUseCases: ExclusionUseCases,
    private val historyUseCases: HistoryUseCases,
    private val app: CleanerApplication
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _storageStats = MutableStateFlow(DeviceStorageStats())
    val storageStats: StateFlow<DeviceStorageStats> = _storageStats.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _resultsTitle = MutableStateFlow("Cleaning Results")
    val resultsTitle: StateFlow<String> = _resultsTitle.asStateFlow()

    private val _resultsList = MutableStateFlow<List<StorageItem>>(emptyList())
    val resultsList: StateFlow<List<StorageItem>> = _resultsList.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _similarGroups = MutableStateFlow<List<SimilarPhotoGroup>>(emptyList())
    val similarGroups: StateFlow<List<SimilarPhotoGroup>> = _similarGroups.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppStorageInfo>>(emptyList())
    val installedApps: StateFlow<List<AppStorageInfo>> = _installedApps.asStateFlow()

    private val _selectedAppPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedAppPackages: StateFlow<Set<String>> = _selectedAppPackages.asStateFlow()

    private val _residualItems = MutableStateFlow<Pair<List<StorageItem>, List<StorageItem>>>(emptyList<StorageItem>() to emptyList())
    val residualItems: StateFlow<Pair<List<StorageItem>, List<StorageItem>>> = _residualItems.asStateFlow()

    val recycledItems: StateFlow<List<RecycledItem>> = recycleBinUseCases.recycledItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRecycledSize: StateFlow<Long> = recycleBinUseCases.totalRecycledSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val exclusions: StateFlow<List<Exclusion>> = exclusionUseCases.exclusionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cleanHistory: StateFlow<List<CleanHistory>> = historyUseCases.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFreedLifetime: StateFlow<Long> = historyUseCases.totalFreedBytesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()

    private val _searchResults = MutableStateFlow<List<StorageItem>>(emptyList())
    val searchResults: StateFlow<List<StorageItem>> = _searchResults.asStateFlow()

    private val _lastCleanupResult = MutableStateFlow<CleanupResult?>(null)
    val lastCleanupResult: StateFlow<CleanupResult?> = _lastCleanupResult.asStateFlow()

    private val _deleteConfirmDialog = MutableStateFlow<Pair<Int, Long>?>(null)
    val deleteConfirmDialog: StateFlow<Pair<Int, Long>?> = _deleteConfirmDialog.asStateFlow()

    val recycleBinEnabled = app.settingsDataStore.recycleBinEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val largeFileThresholdMb = app.settingsDataStore.largeFileThresholdMb
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25L)

    val oldFileThresholdDays = app.settingsDataStore.oldFileThresholdDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    val lowEndMode = app.settingsDataStore.lowEndMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<String> = app.settingsDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "LIGHT")

    private var scanJob: Job? = null
    private var pendingItemsToClean: List<StorageItem> = emptyList()

    init {
        refreshStorageStats()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = getStorageStatsUseCase()
        }
    }

    fun startScan(deepScan: Boolean = false) {
        scanJob?.cancel()
        _currentScreen.value = Screen.SCAN_PROGRESS
        scanJob = viewModelScope.launch {
            scanStorageUseCase(deepScan).collect { progress ->
                _scanProgress.value = progress
                if (!progress.isScanning && progress.percent >= 1f) {
                    refreshStorageStats()
                    loadCategory(CleanCategory.SAFE_JUNK)
                    _currentScreen.value = Screen.DASHBOARD
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _scanProgress.value = ScanProgress(isScanning = false, phase = "Cancelled")
        _currentScreen.value = Screen.DASHBOARD
    }

    fun loadCategory(category: CleanCategory) {
        _resultsTitle.value = when (category) {
            CleanCategory.SAFE_JUNK -> "Safe Junk Items"
            CleanCategory.REVIEW -> "Review Items"
            CleanCategory.SENSITIVE -> "Sensitive Items"
        }
        viewModelScope.launch {
            val list = getCategoryFilesUseCase(category)
            _resultsList.value = list
            // By default, select all safe junk items for convenience
            if (category == CleanCategory.SAFE_JUNK) {
                _selectedItemIds.value = list.map { it.id }.toSet()
            } else {
                _selectedItemIds.value = emptySet()
            }
            _currentScreen.value = Screen.CLEAN_RESULTS
        }
    }

    fun loadType(fileType: FileType) {
        _resultsTitle.value = when (fileType) {
            FileType.APK -> "APK Packages"
            FileType.DOWNLOAD -> "Downloads"
            FileType.SCREENSHOT -> "Screenshots & Screen Records"
            FileType.ARCHIVE -> "Archives & ZIPs"
            FileType.MEDIA_VIDEO -> "Videos"
            FileType.MEDIA_AUDIO -> "Audio"
            FileType.DOCUMENT -> "Documents"
            else -> "Files"
        }
        viewModelScope.launch {
            val list = getTypeFilesUseCase(fileType)
            _resultsList.value = list
            _selectedItemIds.value = if (fileType == FileType.APK) list.map { it.id }.toSet() else emptySet()
            _currentScreen.value = Screen.CLEAN_RESULTS
        }
    }

    fun loadDuplicates() {
        viewModelScope.launch {
            _currentScreen.value = Screen.DUPLICATES
            val groups = findDuplicatesUseCase()
            _duplicateGroups.value = groups
            // Auto select redundant duplicate copies by default (keep oldest original)
            autoSelectDuplicates(keepNewest = false)
        }
    }

    fun autoSelectDuplicates(keepNewest: Boolean) {
        val selected = mutableSetOf<Long>()
        for (group in _duplicateGroups.value) {
            val sorted = if (keepNewest) {
                group.items.sortedByDescending { it.lastModified }
            } else {
                group.items.sortedBy { it.lastModified }
            }
            // Keep the first, mark the rest for deletion
            sorted.drop(1).forEach { selected.add(it.id) }
        }
        _selectedItemIds.value = selected
    }

    fun loadSimilarPhotos() {
        viewModelScope.launch {
            _currentScreen.value = Screen.SIMILAR_PHOTOS
            val groups = findSimilarPhotosUseCase()
            _similarGroups.value = groups
            // Auto select secondary photos in each cluster
            val selected = mutableSetOf<Long>()
            groups.forEach { g ->
                g.items.drop(1).forEach { selected.add(it.id) }
            }
            _selectedItemIds.value = selected
        }
    }

    fun loadLargeFiles() {
        viewModelScope.launch {
            _resultsTitle.value = "Large Files"
            val items = findLargeFilesUseCase(100)
            _resultsList.value = items
            _selectedItemIds.value = emptySet()
            _currentScreen.value = Screen.LARGE_FILES
        }
    }

    fun loadOldFiles() {
        viewModelScope.launch {
            _resultsTitle.value = "Old Unused Files"
            val items = findOldFilesUseCase()
            _resultsList.value = items
            _selectedItemIds.value = emptySet()
            _currentScreen.value = Screen.OLD_FILES
        }
    }

    fun loadResidualFolders() {
        viewModelScope.launch {
            val pair = findResidualAndEmptyFoldersUseCase()
            _residualItems.value = pair
            val allResidual = pair.first + pair.second
            _selectedItemIds.value = allResidual.map { it.id }.toSet()
            _currentScreen.value = Screen.RESIDUAL_FOLDERS
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _currentScreen.value = Screen.APP_MANAGER
            _selectedAppPackages.value = emptySet()
            _installedApps.value = scanAppsUseCase()
        }
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedAppPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedAppPackages.value = current
    }

    fun selectAllApps(apps: List<AppStorageInfo>) {
        _selectedAppPackages.value = apps.map { it.packageName }.toSet()
    }

    fun clearAppSelection() {
        _selectedAppPackages.value = emptySet()
    }

    fun clearCacheForApps(appsToClean: List<AppStorageInfo>) {
        if (appsToClean.isEmpty()) return
        viewModelScope.launch {
            val totalCacheToFree = appsToClean.sumOf { it.cacheSize }
            val pkgs = appsToClean.map { it.packageName }
            val freedBytes = clearAppCacheUseCase.clearMultiple(pkgs)
            val effectiveFreed = if (freedBytes > 0L) freedBytes else totalCacheToFree

            historyUseCases.recordCleanup(
                bytesFreed = effectiveFreed,
                itemCount = appsToClean.size,
                categorySummary = "App Cache Cleaned (${appsToClean.size} apps)"
            )

            _lastCleanupResult.value = CleanupResult(
                freedBytes = effectiveFreed,
                itemsDeleted = appsToClean.size,
                failedCount = 0,
                beforeFreeBytes = _storageStats.value.freeBytes,
                afterFreeBytes = _storageStats.value.freeBytes + effectiveFreed
            )

            _selectedAppPackages.value = emptySet()
            _installedApps.value = scanAppsUseCase()
            refreshStorageStats()
        }
    }

    fun clearCacheForSingleApp(appInfo: AppStorageInfo) {
        clearCacheForApps(listOf(appInfo))
    }

    fun toggleItemSelection(item: StorageItem) {
        val current = _selectedItemIds.value.toMutableSet()
        if (current.contains(item.id)) {
            current.remove(item.id)
        } else {
            current.add(item.id)
        }
        _selectedItemIds.value = current
    }

    fun selectAll(items: List<StorageItem>) {
        _selectedItemIds.value = items.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedItemIds.value = emptySet()
    }

    fun requestCleanup(items: List<StorageItem>) {
        if (items.isEmpty()) return
        pendingItemsToClean = items
        val totalBytes = items.sumOf { it.size }
        _deleteConfirmDialog.value = items.size to totalBytes
    }

    fun cleanSafeJunk() {
        viewModelScope.launch {
            val safeItems = getCategoryFilesUseCase(CleanCategory.SAFE_JUNK)
            if (safeItems.isNotEmpty()) {
                requestCleanup(safeItems)
            }
        }
    }

    fun confirmCleanup() {
        _deleteConfirmDialog.value = null
        val items = pendingItemsToClean
        if (items.isEmpty()) return

        viewModelScope.launch {
            val result = performCleanupUseCase(items)
            _lastCleanupResult.value = result
            _selectedItemIds.value = emptySet()
            refreshStorageStats()
            // Refresh current screen lists
            when (_currentScreen.value) {
                Screen.DUPLICATES -> loadDuplicates()
                Screen.SIMILAR_PHOTOS -> loadSimilarPhotos()
                Screen.RESIDUAL_FOLDERS -> loadResidualFolders()
                Screen.LARGE_FILES -> loadLargeFiles()
                Screen.OLD_FILES -> loadOldFiles()
                Screen.CLEAN_RESULTS -> {
                    _resultsList.value = _resultsList.value.filter { item -> items.none { it.id == item.id } }
                }
                else -> {}
            }
        }
    }

    fun dismissCleanupResult() {
        _lastCleanupResult.value = null
    }

    fun dismissDeleteDialog() {
        _deleteConfirmDialog.value = null
        pendingItemsToClean = emptyList()
    }

    fun restoreRecycledItem(item: RecycledItem) {
        viewModelScope.launch {
            recycleBinUseCases.restore(item)
            refreshStorageStats()
        }
    }

    fun deletePermanently(item: RecycledItem) {
        viewModelScope.launch {
            recycleBinUseCases.deletePermanently(item)
            refreshStorageStats()
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            val freed = recycleBinUseCases.emptyRecycleBin()
            if (freed > 0L) {
                _lastCleanupResult.value = CleanupResult(
                    freedBytes = freed,
                    itemsDeleted = 0,
                    failedCount = 0,
                    beforeFreeBytes = 0L,
                    afterFreeBytes = 0L
                )
            }
            refreshStorageStats()
        }
    }

    fun addExclusion(pattern: String, type: ExclusionType, description: String = "") {
        if (pattern.isBlank()) return
        viewModelScope.launch {
            exclusionUseCases.addExclusion(pattern, type, description)
        }
    }

    fun removeExclusion(id: Long) {
        viewModelScope.launch {
            exclusionUseCases.removeExclusion(id)
        }
    }

    fun toggleExclusion(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            exclusionUseCases.toggleExclusion(id, enabled)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _filterCriteria.value = _filterCriteria.value.copy(searchQuery = query)
        performSearch()
    }

    fun onFilterCriteriaChanged(criteria: FilterCriteria) {
        _filterCriteria.value = criteria
        performSearch()
    }

    fun performSearch() {
        viewModelScope.launch {
            _searchResults.value = searchFilesUseCase(_filterCriteria.value)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyUseCases.clearHistory()
        }
    }

    fun updateLargeFileThreshold(mb: Long) {
        viewModelScope.launch {
            app.settingsDataStore.setLargeFileThresholdMb(mb)
        }
    }

    fun updateOldFileThreshold(days: Int) {
        viewModelScope.launch {
            app.settingsDataStore.setOldFileThresholdDays(days)
        }
    }

    fun toggleRecycleBin(enabled: Boolean) {
        viewModelScope.launch {
            app.settingsDataStore.setRecycleBinEnabled(enabled)
        }
    }

    fun toggleLowEndMode(enabled: Boolean) {
        viewModelScope.launch {
            app.settingsDataStore.setLowEndMode(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            app.settingsDataStore.setThemeMode(mode)
        }
    }

    class Factory(private val app: CleanerApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val storageRepo = app.storageRepository
            val appRepo = app.appRepository
            val recycleRepo = app.recycleBinRepository
            val exclusionRepo = app.exclusionRepository
            val historyRepo = app.historyRepository
            val settingsDs = app.settingsDataStore

            return MainViewModel(
                scanStorageUseCase = ScanStorageUseCase(storageRepo),
                getStorageStatsUseCase = GetStorageStatsUseCase(storageRepo),
                getCategoryFilesUseCase = GetCategoryFilesUseCase(storageRepo),
                getTypeFilesUseCase = GetTypeFilesUseCase(storageRepo),
                findDuplicatesUseCase = FindDuplicatesUseCase(storageRepo),
                findSimilarPhotosUseCase = FindSimilarPhotosUseCase(storageRepo, settingsDs),
                findLargeFilesUseCase = FindLargeFilesUseCase(storageRepo),
                findOldFilesUseCase = FindOldFilesUseCase(storageRepo, settingsDs),
                findResidualAndEmptyFoldersUseCase = FindResidualAndEmptyFoldersUseCase(storageRepo),
                scanAppsUseCase = ScanAppsUseCase(appRepo),
                clearAppCacheUseCase = ClearAppCacheUseCase(appRepo),
                performCleanupUseCase = PerformCleanupUseCase(storageRepo, settingsDs),
                searchFilesUseCase = SearchFilesUseCase(storageRepo),
                recycleBinUseCases = RecycleBinUseCases(recycleRepo),
                exclusionUseCases = ExclusionUseCases(exclusionRepo),
                historyUseCases = HistoryUseCases(historyRepo),
                app = app
            ) as T
        }
    }
}
