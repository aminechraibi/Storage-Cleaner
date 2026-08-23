package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.data.local.SettingsDataStore
import com.example.data.local.dao.FileDao
import com.example.data.local.entity.FileEntity
import com.example.model.CleanCategory
import com.example.model.CleanupResult
import com.example.model.DeviceStorageStats
import com.example.model.DuplicateGroup
import com.example.model.FileType
import com.example.model.FilterCriteria
import com.example.model.ScanProgress
import com.example.model.SimilarPhotoGroup
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.StorageItem
import com.example.util.FileClassifier
import com.example.util.HashUtils
import com.example.util.StorageUtils
import com.example.util.TopKMinHeap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque
import java.util.UUID

class StorageRepository(
    private val context: Context,
    private val fileDao: FileDao,
    private val exclusionRepository: ExclusionRepository,
    private val recycleBinRepository: RecycleBinRepository,
    private val historyRepository: HistoryRepository,
    private val settingsDataStore: SettingsDataStore
) {

    /**
     * Iterative BFS scanning with batching, incremental cache, and progress updates.
     * Guaranteed to only run when explicitly called.
     */
    fun scanStorage(deepScan: Boolean = false): Flow<ScanProgress> = flow {
        emit(ScanProgress(isScanning = true, phase = "Preparing scanner...", percent = 0.05f))

        val activeExclusions = exclusionRepository.getActiveExclusions()
        val largeThresholdMb = settingsDataStore.largeFileThresholdMb.first()
        val largeThresholdBytes = largeThresholdMb * 1024 * 1024L
        val oldDays = settingsDataStore.oldFileThresholdDays.first()
        val oldThresholdTimestamp = System.currentTimeMillis() - (oldDays.toLong() * 24 * 60 * 60 * 1000)

        val rootDirs = mutableListOf<File>()
        try {
            val externalDir = Environment.getExternalStorageDirectory()
            if (externalDir != null && externalDir.exists()) rootDirs.add(externalDir)
            val externalFilesDirs = context.getExternalFilesDirs(null)
            externalFilesDirs?.filterNotNull()?.forEach { rootDirs.add(it) }
            context.cacheDir?.let { rootDirs.add(it) }
            context.externalCacheDir?.let { rootDirs.add(it) }
        } catch (e: Exception) {
            // fallback to cache dir
            context.cacheDir?.let { rootDirs.add(it) }
        }

        val queue = ArrayDeque<File>()
        for (root in rootDirs) {
            if (root.exists()) queue.add(root)
        }

        var scannedCount = 0
        var scannedBytes = 0L
        val batch = ArrayList<FileEntity>(200)

        // Load existing index for incremental scan comparison
        val existingMap = mutableMapOf<String, FileEntity>()
        val existingList = fileDao.getAllFilesList()
        for (item in existingList) {
            existingMap[item.path] = item
        }

        emit(ScanProgress(isScanning = true, phase = "Scanning directories...", percent = 0.15f))

        while (queue.isNotEmpty() && currentCoroutineContext().isActive) {
            val current = queue.poll() ?: continue

            // Exclusions check
            if (exclusionRepository.isExcluded(current, activeExclusions)) {
                continue
            }

            if (current.isDirectory) {
                val children = try {
                    current.listFiles()
                } catch (e: SecurityException) {
                    null
                }

                if (children == null || children.isEmpty()) {
                    // Empty folder detection
                    val (cat, type) = CleanCategory.SAFE_JUNK to FileType.EMPTY_FOLDER
                    val entity = FileEntity(
                        uri = Uri.fromFile(current).toString(),
                        path = current.absolutePath,
                        name = current.name,
                        size = 0L,
                        lastModified = current.lastModified(),
                        mimeType = "",
                        extension = "",
                        category = cat.name,
                        fileType = type.name,
                        isDirectory = true,
                        itemCount = 0
                    )
                    batch.add(entity)
                } else {
                    for (child in children) {
                        queue.add(child)
                    }
                }
            } else if (current.isFile) {
                scannedCount++
                val fileLen = current.length()
                val lastMod = current.lastModified()
                scannedBytes += fileLen

                // Incremental check: if size & lastModified match cached entry, reuse!
                val cached = existingMap[current.absolutePath]
                val entity = if (cached != null && cached.size == fileLen && cached.lastModified == lastMod) {
                    cached
                } else {
                    val (cat, type) = FileClassifier.classify(current, largeThresholdBytes, oldThresholdTimestamp)
                    val ext = current.extension.lowercase()
                    val mime = FileClassifier.getMimeType(ext)
                    FileEntity(
                        uri = Uri.fromFile(current).toString(),
                        path = current.absolutePath,
                        name = current.name,
                        size = fileLen,
                        lastModified = lastMod,
                        mimeType = mime,
                        extension = ext,
                        category = cat.name,
                        fileType = type.name,
                        isProtected = false,
                        isDirectory = false
                    )
                }
                batch.add(entity)
            }

            if (batch.size >= 150) {
                fileDao.insertBatch(batch)
                batch.clear()
                emit(
                    ScanProgress(
                        isScanning = true,
                        currentPath = current.name,
                        scannedCount = scannedCount,
                        scannedBytes = scannedBytes,
                        phase = "Indexed $scannedCount files...",
                        percent = 0.5f
                    )
                )
            }
        }

        if (batch.isNotEmpty()) {
            fileDao.insertBatch(batch)
            batch.clear()
        }

        // Phase 2: If deep scan is requested, perform candidate hashing
        if (deepScan && currentCoroutineContext().isActive) {
            emit(ScanProgress(isScanning = true, phase = "Analyzing duplicate candidates...", percent = 0.75f, isDeepScan = true))
            processDuplicateCandidatesInternal()

            emit(ScanProgress(isScanning = true, phase = "Analyzing photo similarities...", percent = 0.90f, isDeepScan = true))
            processSimilarPhotoCandidatesInternal()
        }

        settingsDataStore.setLastScanTimestamp(System.currentTimeMillis())

        emit(
            ScanProgress(
                isScanning = false,
                currentPath = "Scan complete",
                scannedCount = scannedCount,
                scannedBytes = scannedBytes,
                phase = "Finished",
                percent = 1.0f
            )
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Duplicate detection:
     * 1. Find size duplicates (> 1KB)
     * 2. Calculate partial hash (first 4KB)
     * 3. Calculate full SHA-256 for collision candidates
     */
    suspend fun findDuplicates(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        processDuplicateCandidatesInternal()

        val duplicateEntities = fileDao.getDuplicates()
        val grouped = duplicateEntities.groupBy { it.hash.orEmpty() }.filter { it.key.isNotEmpty() && it.value.size > 1 }

        grouped.map { entry ->
            val items = entry.value.map { it.toModel() }
            val singleSize = items.firstOrNull()?.size ?: 0L
            val wasted = singleSize * (items.size - 1)
            DuplicateGroup(
                hash = entry.key,
                singleSize = singleSize,
                wastedBytes = wasted,
                items = items
            )
        }.sortedByDescending { it.wastedBytes }
    }

    private suspend fun processDuplicateCandidatesInternal() {
        val sizeCandidates = fileDao.getPotentialDuplicateSizeCandidates()
        val sizeBuckets = sizeCandidates.groupBy { it.size }

        for ((_, items) in sizeBuckets) {
            if (items.size < 2) continue

            // 1. Compute partial hash
            val partialHashBuckets = mutableMapOf<String, MutableList<FileEntity>>()
            for (entity in items) {
                var pHash = entity.partialHash
                if (pHash == null) {
                    val file = File(entity.path)
                    pHash = HashUtils.calculatePartialHash(file)
                    if (pHash != null) {
                        val updated = entity.copy(partialHash = pHash)
                        fileDao.update(updated)
                        partialHashBuckets.getOrPut(pHash) { mutableListOf() }.add(updated)
                    }
                } else {
                    partialHashBuckets.getOrPut(pHash) { mutableListOf() }.add(entity)
                }
            }

            // 2. Compute full SHA-256 only for partial hash collisions
            for ((_, pGroup) in partialHashBuckets) {
                if (pGroup.size < 2) continue
                for (entity in pGroup) {
                    if (entity.hash == null) {
                        val file = File(entity.path)
                        val fullHash = HashUtils.calculateSha256(file)
                        if (fullHash != null) {
                            fileDao.update(entity.copy(hash = fullHash, fileType = FileType.DUPLICATE.name, category = CleanCategory.REVIEW.name))
                        }
                    }
                }
            }
        }
    }

    /**
     * Similar photo detection using dHash + Hamming distance
     */
    suspend fun findSimilarPhotos(hammingThreshold: Int = 8): List<SimilarPhotoGroup> = withContext(Dispatchers.IO) {
        processSimilarPhotoCandidatesInternal()

        val images = fileDao.getSimilarCandidates()
        val groups = mutableListOf<SimilarPhotoGroup>()
        val processed = mutableSetOf<Long>()

        for (i in images.indices) {
            val imgA = images[i]
            if (processed.contains(imgA.id)) continue
            val hashA = imgA.perceptualHash ?: continue

            val matched = mutableListOf(imgA.toModel())
            for (j in i + 1 until images.size) {
                val imgB = images[j]
                if (processed.contains(imgB.id)) continue
                val hashB = imgB.perceptualHash ?: continue

                val dist = HashUtils.hammingDistance(hashA, hashB)
                if (dist <= hammingThreshold) {
                    matched.add(imgB.toModel())
                    processed.add(imgB.id)
                }
            }

            if (matched.size > 1) {
                processed.add(imgA.id)
                val totalBytes = matched.sumOf { it.size }
                groups.add(
                    SimilarPhotoGroup(
                        groupId = UUID.randomUUID().toString(),
                        similarityScore = 90,
                        totalBytes = totalBytes,
                        items = matched
                    )
                )
            }
        }

        groups.sortedByDescending { it.totalBytes }
    }

    private suspend fun processSimilarPhotoCandidatesInternal() {
        val imageFiles = fileDao.getFilesByType(FileType.MEDIA_IMAGE.name)
        for (img in imageFiles) {
            if (img.perceptualHash == null) {
                val file = File(img.path)
                val dHash = HashUtils.calculateDHash(file)
                if (dHash != null) {
                    fileDao.update(img.copy(perceptualHash = dHash))
                }
            }
        }
    }

    /**
     * Memory-bounded Top-K large files
     */
    suspend fun getTopKLargeFiles(k: Int = 100): List<StorageItem> = withContext(Dispatchers.IO) {
        val entities = fileDao.getTopKLargeFiles(k)
        val heap = TopKMinHeap<StorageItem>(k, compareBy { it.size })
        entities.forEach { heap.add(it.toModel()) }
        heap.toSortedList()
    }

    suspend fun getOldFiles(thresholdTimestamp: Long): List<StorageItem> = withContext(Dispatchers.IO) {
        fileDao.getOldFiles(thresholdTimestamp).map { it.toModel() }
    }

    suspend fun getFilesByCategory(category: CleanCategory): List<StorageItem> = withContext(Dispatchers.IO) {
        fileDao.getFilesByCategory(category.name).map { it.toModel() }
    }

    suspend fun getFilesByType(fileType: FileType): List<StorageItem> = withContext(Dispatchers.IO) {
        fileDao.getFilesByType(fileType.name).map { it.toModel() }
    }

    suspend fun searchFiles(filter: FilterCriteria): List<StorageItem> = withContext(Dispatchers.IO) {
        val rawResults = if (filter.searchQuery.isNotBlank()) {
            val sanitizedQuery = filter.searchQuery.replace("\"", "").replace("'", "") + "*"
            try {
                fileDao.searchFilesFts(sanitizedQuery).map { it.toModel() }
            } catch (e: Exception) {
                fileDao.getAllFilesList().map { it.toModel() }.filter {
                    it.name.contains(filter.searchQuery, ignoreCase = true) || it.path.contains(filter.searchQuery, ignoreCase = true)
                }
            }
        } else {
            fileDao.getAllFilesList().map { it.toModel() }
        }

        var filtered = rawResults
        if (filter.category != null) {
            filtered = filtered.filter { it.category == filter.category }
        }
        if (filter.fileType != null) {
            filtered = filtered.filter { it.type == filter.fileType }
        }
        filtered = filtered.filter { it.size in filter.minSizeBytes..filter.maxSizeBytes }

        when (filter.sortField) {
            SortField.SIZE -> if (filter.sortDirection == SortDirection.DESCENDING) filtered.sortedByDescending { it.size } else filtered.sortedBy { it.size }
            SortField.DATE -> if (filter.sortDirection == SortDirection.DESCENDING) filtered.sortedByDescending { it.lastModified } else filtered.sortedBy { it.lastModified }
            SortField.NAME -> if (filter.sortDirection == SortDirection.DESCENDING) filtered.sortedByDescending { it.name } else filtered.sortedBy { it.name }
        }
    }

    suspend fun getStorageStats(): DeviceStorageStats = withContext(Dispatchers.IO) {
        val raw = StorageUtils.getDeviceStorageStats()
        val safeBytes = fileDao.getSafeCleanableBytes() ?: 0L
        val reviewBytes = fileDao.getReviewCleanableBytes() ?: 0L
        val sensitiveBytes = fileDao.getSensitiveCleanableBytes() ?: 0L
        val count = fileDao.getFilesCount()
        val apks = fileDao.getApkCount()
        val residuals = fileDao.getResidualCount()
        val emptyFolders = fileDao.getEmptyFolderCount()

        raw.copy(
            safeCleanableBytes = safeBytes,
            reviewCleanableBytes = reviewBytes,
            sensitiveCleanableBytes = sensitiveBytes,
            scannedFilesCount = count,
            apksCount = apks,
            residualCount = residuals,
            emptyFoldersCount = emptyFolders
        )
    }

    /**
     * Performs destructive cleanup on explicit user confirmation.
     * Strictly respects exclusions, checks file deletion status, and only counts true freed bytes.
     */
    suspend fun performCleanup(
        items: List<StorageItem>,
        useRecycleBin: Boolean = false
    ): CleanupResult = withContext(Dispatchers.IO) {
        val beforeStats = StorageUtils.getDeviceStorageStats()
        val activeExclusions = exclusionRepository.getActiveExclusions()

        var freedBytes = 0L
        var deletedCount = 0
        var failedCount = 0
        val categoryMap = mutableMapOf<CleanCategory, Long>()

        val deletedIds = mutableListOf<Long>()

        for (item in items) {
            val file = File(item.path)
            // 1. Strict Exclusions check
            if (exclusionRepository.isExcluded(file, activeExclusions)) {
                failedCount++
                continue
            }

            var success = false
            if (useRecycleBin && !item.isDirectory) {
                success = recycleBinRepository.moveToTrash(file, item.mimeType)
            } else {
                success = if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }

            if (success) {
                deletedCount++
                freedBytes += item.size
                deletedIds.add(item.id)
                categoryMap[item.category] = (categoryMap[item.category] ?: 0L) + item.size
            } else {
                // If file doesn't exist anymore, remove from index
                if (!file.exists()) {
                    deletedIds.add(item.id)
                } else {
                    failedCount++
                }
            }
        }

        if (deletedIds.isNotEmpty()) {
            fileDao.deleteByIds(deletedIds)
        }

        val afterStats = StorageUtils.getDeviceStorageStats()

        // Record session history
        if (freedBytes > 0L) {
            val summary = "${StorageUtils.formatBytes(freedBytes)} ($deletedCount items)"
            historyRepository.recordCleanup(freedBytes, deletedCount, summary)
        }

        CleanupResult(
            freedBytes = freedBytes,
            itemsDeleted = deletedCount,
            failedCount = failedCount,
            beforeFreeBytes = beforeStats.freeBytes,
            afterFreeBytes = afterStats.freeBytes,
            deletedCategories = categoryMap
        )
    }
}
