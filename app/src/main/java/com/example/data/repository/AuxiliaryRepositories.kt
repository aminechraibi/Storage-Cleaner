package com.example.data.repository

import com.example.data.local.dao.ExclusionDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.dao.RecycleBinDao
import com.example.data.local.entity.ExclusionEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.RecycleBinEntity
import com.example.model.CleanHistory
import com.example.model.Exclusion
import com.example.model.ExclusionType
import com.example.model.RecycledItem
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Locale

class ExclusionRepository(private val exclusionDao: ExclusionDao) {

    val exclusionsFlow: Flow<List<Exclusion>> = exclusionDao.getAllExclusionsFlow().map { list ->
        list.map { it.toModel() }
    }

    suspend fun getActiveExclusions(): List<Exclusion> {
        return exclusionDao.getActiveExclusions().map { it.toModel() }
    }

    suspend fun addExclusion(pattern: String, type: ExclusionType, description: String = ""): Long {
        val entity = ExclusionEntity(
            pattern = pattern.trim(),
            type = type.name,
            enabled = true,
            description = description
        )
        return exclusionDao.insert(entity)
    }

    suspend fun removeExclusion(id: Long) {
        exclusionDao.deleteById(id)
    }

    suspend fun setExclusionEnabled(id: Long, enabled: Boolean) {
        exclusionDao.setEnabled(id, enabled)
    }

    fun isExcluded(file: File, activeExclusions: List<Exclusion>): Boolean {
        if (activeExclusions.isEmpty()) return false
        val path = file.absolutePath.lowercase(Locale.ROOT)
        val name = file.name.lowercase(Locale.ROOT)
        val ext = file.extension.lowercase(Locale.ROOT)

        for (exclusion in activeExclusions) {
            val pattern = exclusion.pattern.lowercase(Locale.ROOT)
            when (exclusion.type) {
                ExclusionType.PATH -> {
                    if (path.startsWith(pattern) || path == pattern) return true
                }
                ExclusionType.FOLDER_NAME -> {
                    if (path.contains("/$pattern/") || name == pattern) return true
                }
                ExclusionType.EXTENSION -> {
                    val cleanExt = pattern.removePrefix(".")
                    if (ext == cleanExt) return true
                }
                ExclusionType.KEYWORD -> {
                    if (name.contains(pattern) || path.contains(pattern)) return true
                }
            }
        }
        return false
    }
}

class RecycleBinRepository(
    private val context: Context,
    private val recycleBinDao: RecycleBinDao
) {
    private val trashDir: File by lazy {
        File(context.filesDir, "recycle_bin").apply {
            if (!exists()) mkdirs()
        }
    }

    val recycledItemsFlow: Flow<List<RecycledItem>> = recycleBinDao.getAllRecycledFlow().map { list ->
        list.map { it.toModel() }
    }

    val totalRecycledSizeFlow: Flow<Long> = recycleBinDao.getTotalRecycledSizeFlow().map { it ?: 0L }

    suspend fun moveToTrash(file: File, mimeType: String = ""): Boolean {
        if (!file.exists()) return false
        return try {
            val uniqueName = "${System.currentTimeMillis()}_${file.name}"
            val dest = File(trashDir, uniqueName)
            val moved = file.renameTo(dest) || (copyAndDelete(file, dest))
            if (moved) {
                val entity = RecycleBinEntity(
                    originalPath = file.absolutePath,
                    trashPath = dest.absolutePath,
                    name = file.name,
                    size = dest.length(),
                    recycledAt = System.currentTimeMillis(),
                    mimeType = mimeType
                )
                recycleBinDao.insert(entity)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreItem(recycledItem: RecycledItem): Boolean {
        return try {
            val trashFile = File(recycledItem.trashPath)
            if (!trashFile.exists()) {
                recycleBinDao.deleteById(recycledItem.id)
                return false
            }
            val originalFile = File(recycledItem.originalPath)
            originalFile.parentFile?.mkdirs()
            val restored = trashFile.renameTo(originalFile) || copyAndDelete(trashFile, originalFile)
            if (restored) {
                recycleBinDao.deleteById(recycledItem.id)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deletePermanently(recycledItem: RecycledItem): Boolean {
        return try {
            val trashFile = File(recycledItem.trashPath)
            if (trashFile.exists()) {
                trashFile.delete()
            }
            recycleBinDao.deleteById(recycledItem.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun emptyRecycleBin(): Long {
        var freed = 0L
        val items = recycleBinDao.getAllRecycledList()
        for (item in items) {
            val trashFile = File(item.trashPath)
            if (trashFile.exists()) {
                val len = trashFile.length()
                if (trashFile.delete()) {
                    freed += len
                }
            }
        }
        recycleBinDao.clearAll()
        return freed
    }

    private fun copyAndDelete(source: File, dest: File): Boolean {
        return try {
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            source.delete()
        } catch (e: Exception) {
            false
        }
    }
}

class HistoryRepository(private val historyDao: HistoryDao) {

    val historyFlow: Flow<List<CleanHistory>> = historyDao.getAllHistoryFlow().map { list ->
        list.map {
            CleanHistory(
                id = it.id,
                timestamp = it.timestamp,
                bytesFreed = it.bytesFreed,
                itemCount = it.itemCount,
                categorySummary = it.categorySummary
            )
        }
    }

    val totalFreedBytesFlow: Flow<Long> = historyDao.getTotalFreedBytesFlow().map { it ?: 0L }

    suspend fun recordCleanup(bytesFreed: Long, itemCount: Int, categorySummary: String): Long {
        val entity = HistoryEntity(
            timestamp = System.currentTimeMillis(),
            bytesFreed = bytesFreed,
            itemCount = itemCount,
            categorySummary = categorySummary
        )
        return historyDao.insert(entity)
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }
}
