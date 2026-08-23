package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ExclusionEntity
import com.example.data.local.entity.FileEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.RecycleBinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(files: List<FileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: FileEntity): Long

    @Update
    suspend fun update(file: FileEntity)

    @Query("SELECT * FROM files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): FileEntity?

    @Query("SELECT * FROM files ORDER BY size DESC")
    fun getAllFilesFlow(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY size DESC")
    suspend fun getAllFilesList(): List<FileEntity>

    @Query("SELECT * FROM files WHERE category = :category ORDER BY size DESC")
    fun getFilesByCategoryFlow(category: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE category = :category ORDER BY size DESC")
    suspend fun getFilesByCategory(category: String): List<FileEntity>

    @Query("SELECT * FROM files WHERE file_type = :fileType ORDER BY size DESC")
    fun getFilesByTypeFlow(fileType: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE file_type = :fileType ORDER BY size DESC")
    suspend fun getFilesByType(fileType: String): List<FileEntity>

    @Query("SELECT * FROM files WHERE is_directory = 0 ORDER BY size DESC LIMIT :limit")
    suspend fun getTopKLargeFiles(limit: Int): List<FileEntity>

    @Query("SELECT * FROM files WHERE is_directory = 0 AND last_modified < :thresholdTimestamp ORDER BY last_modified ASC")
    suspend fun getOldFiles(thresholdTimestamp: Long): List<FileEntity>

    @Query("SELECT * FROM files WHERE hash IS NOT NULL AND hash IN (SELECT hash FROM files WHERE is_directory = 0 AND hash IS NOT NULL GROUP BY hash HAVING COUNT(*) > 1) ORDER BY hash ASC, size DESC")
    suspend fun getDuplicates(): List<FileEntity>

    @Query("SELECT * FROM files WHERE file_type = 'MEDIA_IMAGE' AND perceptual_hash IS NOT NULL ORDER BY perceptual_hash ASC")
    suspend fun getSimilarCandidates(): List<FileEntity>

    @Query("SELECT * FROM files WHERE is_directory = 0 AND size IN (SELECT size FROM files WHERE is_directory = 0 AND size > 1024 GROUP BY size HAVING COUNT(*) > 1) ORDER BY size DESC")
    suspend fun getPotentialDuplicateSizeCandidates(): List<FileEntity>

    // Room FTS Full-Text Search
    @Query("""
        SELECT files.* FROM files
        JOIN files_fts ON files.id = files_fts.rowid
        WHERE files_fts MATCH :searchQuery
        ORDER BY files.size DESC
    """)
    suspend fun searchFilesFts(searchQuery: String): List<FileEntity>

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM files WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM files WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM files")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM files")
    suspend fun getFilesCount(): Int

    @Query("SELECT SUM(size) FROM files WHERE category = 'SAFE_JUNK'")
    suspend fun getSafeCleanableBytes(): Long?

    @Query("SELECT SUM(size) FROM files WHERE category = 'REVIEW'")
    suspend fun getReviewCleanableBytes(): Long?

    @Query("SELECT SUM(size) FROM files WHERE category = 'SENSITIVE'")
    suspend fun getSensitiveCleanableBytes(): Long?

    @Query("SELECT COUNT(*) FROM files WHERE file_type = 'APK'")
    suspend fun getApkCount(): Int

    @Query("SELECT COUNT(*) FROM files WHERE file_type = 'RESIDUAL'")
    suspend fun getResidualCount(): Int

    @Query("SELECT COUNT(*) FROM files WHERE file_type = 'EMPTY_FOLDER'")
    suspend fun getEmptyFolderCount(): Int
}

@Dao
interface ExclusionDao {

    @Query("SELECT * FROM exclusions ORDER BY addedAt DESC")
    fun getAllExclusionsFlow(): Flow<List<ExclusionEntity>>

    @Query("SELECT * FROM exclusions WHERE enabled = 1")
    suspend fun getActiveExclusions(): List<ExclusionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exclusion: ExclusionEntity): Long

    @Query("DELETE FROM exclusions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE exclusions SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}

@Dao
interface RecycleBinDao {

    @Query("SELECT * FROM recycle_bin ORDER BY recycled_at DESC")
    fun getAllRecycledFlow(): Flow<List<RecycleBinEntity>>

    @Query("SELECT * FROM recycle_bin ORDER BY recycled_at DESC")
    suspend fun getAllRecycledList(): List<RecycleBinEntity>

    @Query("SELECT * FROM recycle_bin WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecycleBinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecycleBinEntity): Long

    @Query("DELETE FROM recycle_bin WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recycle_bin WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM recycle_bin")
    suspend fun clearAll()

    @Query("SELECT SUM(size) FROM recycle_bin")
    fun getTotalRecycledSizeFlow(): Flow<Long?>
}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM cleanup_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long

    @Query("SELECT SUM(bytesFreed) FROM cleanup_history")
    fun getTotalFreedBytesFlow(): Flow<Long?>

    @Query("DELETE FROM cleanup_history")
    suspend fun clearAll()
}
