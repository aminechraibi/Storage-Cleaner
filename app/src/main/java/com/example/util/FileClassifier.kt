package com.example.util

import com.example.model.CleanCategory
import com.example.model.Exclusion
import com.example.model.ExclusionType
import com.example.model.FileType
import java.io.File
import java.util.Locale

object FileClassifier {

    private val SAFE_EXTENSIONS = setOf(
        "tmp", "temp", "log", "bak", "cache", "thumb", "thumbnails",
        "crdownload", "part", "dmp", "old", "swp"
    )

    private val ARCHIVE_EXTENSIONS = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso"
    )

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg"
    )

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "mov", "avi", "wmv", "flv", "webm", "3gp", "m4v"
    )

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "m4a", "flac", "aac", "ogg", "wma", "opus"
    )

    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub", "csv"
    )

    fun classify(
        file: File,
        largeThresholdBytes: Long = 25 * 1024 * 1024L,
        oldThresholdTimestamp: Long = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)
    ): Pair<CleanCategory, FileType> {
        val path = file.absolutePath
        val name = file.name
        val ext = file.extension
        val size = if (file.exists()) file.length() else 0L
        val modified = if (file.exists()) file.lastModified() else System.currentTimeMillis()
        val isDirectory = file.isDirectory
        val isEmptyDirectory = isDirectory && (file.list()?.isEmpty() == true)

        return classify(
            path = path,
            name = name,
            extension = ext,
            size = size,
            lastModified = modified,
            isDirectory = isDirectory,
            isEmptyDirectory = isEmptyDirectory,
            largeThresholdBytes = largeThresholdBytes,
            oldThresholdTimestamp = oldThresholdTimestamp
        )
    }

    fun classify(
        path: String,
        name: String,
        extension: String,
        size: Long,
        lastModified: Long = System.currentTimeMillis(),
        isDirectory: Boolean = false,
        isEmptyDirectory: Boolean = false,
        largeThresholdBytes: Long = 25 * 1024 * 1024L,
        oldThresholdTimestamp: Long = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)
    ): Pair<CleanCategory, FileType> {
        val lowerPath = path.lowercase(Locale.ROOT)
        val lowerName = name.lowercase(Locale.ROOT)
        val lowerExt = extension.lowercase(Locale.ROOT)

        // 1. Check directory
        if (isDirectory) {
            return if (isEmptyDirectory) {
                CleanCategory.SAFE_JUNK to FileType.EMPTY_FOLDER
            } else {
                CleanCategory.REVIEW to FileType.OTHER
            }
        }

        // 2. Safe Junk patterns (Temporary files, residual logs, caches, thumbnails)
        if (SAFE_EXTENSIONS.contains(lowerExt) ||
            lowerPath.contains("/.thumbnails") ||
            lowerPath.contains("/cache/") ||
            lowerPath.contains("/cache") ||
            lowerPath.contains("/.cache/") ||
            lowerName.startsWith("cache_") ||
            lowerName.endsWith(".tmp") ||
            lowerName.endsWith(".log") ||
            lowerName.startsWith("thumb_")
        ) {
            return CleanCategory.SAFE_JUNK to FileType.CACHE
        }

        // 3. Residual files from uninstalled apps or temporary folders
        if (lowerPath.contains("/android/data/") && (lowerPath.contains("/cache") || lowerPath.contains("/temp"))) {
            return CleanCategory.SAFE_JUNK to FileType.RESIDUAL
        }

        // 4. APK Files (Downloaded installer packages)
        if (lowerExt == "apk") {
            return CleanCategory.SAFE_JUNK to FileType.APK
        }

        // 5. Screenshots & Screen Recordings
        if (lowerPath.contains("/screenshots") || lowerName.startsWith("screenshot") || lowerPath.contains("/screenrecorder") || lowerName.startsWith("screen_recording")) {
            return CleanCategory.REVIEW to FileType.SCREENSHOT
        }

        // 6. Downloads folder items
        if (lowerPath.contains("/download/") || lowerPath.contains("/downloads/")) {
            return if (size >= largeThresholdBytes) {
                CleanCategory.REVIEW to FileType.LARGE
            } else {
                CleanCategory.REVIEW to FileType.DOWNLOAD
            }
        }

        // 7. Large Files
        if (size >= largeThresholdBytes) {
            return CleanCategory.REVIEW to FileType.LARGE
        }

        // 8. Media Types
        if (IMAGE_EXTENSIONS.contains(lowerExt)) {
            return if (lastModified < oldThresholdTimestamp) {
                CleanCategory.REVIEW to FileType.OLD
            } else {
                CleanCategory.SENSITIVE to FileType.MEDIA_IMAGE
            }
        }

        if (VIDEO_EXTENSIONS.contains(lowerExt)) {
            return if (size >= largeThresholdBytes) {
                CleanCategory.REVIEW to FileType.LARGE
            } else {
                CleanCategory.SENSITIVE to FileType.MEDIA_VIDEO
            }
        }

        if (AUDIO_EXTENSIONS.contains(lowerExt)) {
            return CleanCategory.SENSITIVE to FileType.MEDIA_AUDIO
        }

        if (DOCUMENT_EXTENSIONS.contains(lowerExt)) {
            return CleanCategory.SENSITIVE to FileType.DOCUMENT
        }

        if (ARCHIVE_EXTENSIONS.contains(lowerExt)) {
            return CleanCategory.REVIEW to FileType.ARCHIVE
        }

        // 9. Old unaccessed files
        if (lastModified < oldThresholdTimestamp) {
            return CleanCategory.REVIEW to FileType.OLD
        }

        return CleanCategory.REVIEW to FileType.OTHER
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
                    if (path.contains("/$pattern/") || name == pattern || path.endsWith("/$pattern")) return true
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

    fun getMimeType(extension: String): String {
        return when (extension.lowercase(Locale.ROOT)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
