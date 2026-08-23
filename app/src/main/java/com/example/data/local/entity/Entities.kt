package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.CleanCategory
import com.example.model.Exclusion
import com.example.model.ExclusionType
import com.example.model.FileType
import com.example.model.RecycledItem
import com.example.model.StorageItem

@Entity(
    tableName = "files",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["size"]),
        Index(value = ["last_modified"]),
        Index(value = ["category"]),
        Index(value = ["file_type"]),
        Index(value = ["hash"]),
        Index(value = ["partial_hash"]),
        Index(value = ["perceptual_hash"])
    ]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "size")
    val size: Long,
    @ColumnInfo(name = "last_modified")
    val lastModified: Long,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "extension")
    val extension: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "file_type")
    val fileType: String,
    @ColumnInfo(name = "hash")
    val hash: String? = null,
    @ColumnInfo(name = "partial_hash")
    val partialHash: String? = null,
    @ColumnInfo(name = "perceptual_hash")
    val perceptualHash: Long? = null,
    @ColumnInfo(name = "is_protected")
    val isProtected: Boolean = false,
    @ColumnInfo(name = "is_directory")
    val isDirectory: Boolean = false,
    @ColumnInfo(name = "item_count")
    val itemCount: Int = 0
) {
    fun toModel(): StorageItem = StorageItem(
        id = id,
        uri = uri,
        path = path,
        name = name,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        extension = extension,
        category = try { CleanCategory.valueOf(category) } catch (e: Exception) { CleanCategory.REVIEW },
        type = try { FileType.valueOf(fileType) } catch (e: Exception) { FileType.OTHER },
        hash = hash,
        partialHash = partialHash,
        perceptualHash = perceptualHash,
        isProtected = isProtected,
        isDirectory = isDirectory,
        itemCount = itemCount
    )
}

@Entity(tableName = "files_fts")
@Fts4(contentEntity = FileEntity::class)
data class FileFtsEntity(
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "path")
    val path: String
)

@Entity(
    tableName = "exclusions",
    indices = [Index(value = ["pattern"], unique = true)]
)
data class ExclusionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val type: String,
    val enabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val description: String = ""
) {
    fun toModel(): Exclusion = Exclusion(
        id = id,
        pattern = pattern,
        type = try { ExclusionType.valueOf(type) } catch (e: Exception) { ExclusionType.FOLDER_NAME },
        enabled = enabled,
        addedAt = addedAt,
        description = description
    )
}

@Entity(
    tableName = "recycle_bin",
    indices = [Index(value = ["original_path"]), Index(value = ["trash_path"])]
)
data class RecycleBinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "original_path")
    val originalPath: String,
    @ColumnInfo(name = "trash_path")
    val trashPath: String,
    val name: String,
    val size: Long,
    @ColumnInfo(name = "recycled_at")
    val recycledAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "mime_type")
    val mimeType: String = ""
) {
    fun toModel(): RecycledItem = RecycledItem(
        id = id,
        originalPath = originalPath,
        trashPath = trashPath,
        name = name,
        size = size,
        recycledAt = recycledAt,
        mimeType = mimeType
    )
}

@Entity(tableName = "cleanup_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val bytesFreed: Long,
    val itemCount: Int,
    val categorySummary: String
)
