package com.example.util

import com.example.model.CleanCategory
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
        val path = file.absolutePath.lowercase(Locale.ROOT)
        val name = file.name.lowercase(Locale.ROOT)
        val ext = file.extension.lowercase(Locale.ROOT)
        val size = file.length()
        val modified = file.lastModified()

        // 1. Check directory
        if (file.isDirectory) {
            val children = file.list()
            return if (children == null || children.isEmpty()) {
                CleanCategory.SAFE_JUNK to FileType.EMPTY_FOLDER
            } else {
                CleanCategory.REVIEW to FileType.OTHER
            }
        }

        // 2. Safe Junk patterns (Temporary files, residual logs, caches, thumbnails)
        if (SAFE_EXTENSIONS.contains(ext) ||
            path.contains("/.thumbnails") ||
            path.contains("/cache/") ||
            path.contains("/cache") ||
            path.contains("/.cache/") ||
            name.startsWith("cache_") ||
            name.endsWith(".tmp") ||
            name.endsWith(".log") ||
            name.startsWith("thumb_")
        ) {
            return CleanCategory.SAFE_JUNK to FileType.CACHE
        }

        // 3. Residual files from uninstalled apps or temporary folders
        if (path.contains("/android/data/") && (path.contains("/cache") || path.contains("/temp"))) {
            return CleanCategory.SAFE_JUNK to FileType.RESIDUAL
        }

        // 4. APK Files (Downloaded installer packages)
        if (ext == "apk") {
            return CleanCategory.SAFE_JUNK to FileType.APK
        }

        // 5. Screenshots & Screen Recordings
        if (path.contains("/screenshots") || name.startsWith("screenshot") || path.contains("/screenrecorder") || name.startsWith("screen_recording")) {
            return CleanCategory.REVIEW to FileType.SCREENSHOT
        }

        // 6. Downloads folder items
        if (path.contains("/download/") || path.contains("/downloads/")) {
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
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return if (modified < oldThresholdTimestamp) {
                CleanCategory.REVIEW to FileType.OLD
            } else {
                CleanCategory.SENSITIVE to FileType.MEDIA_IMAGE
            }
        }

        if (VIDEO_EXTENSIONS.contains(ext)) {
            return if (size >= largeThresholdBytes) {
                CleanCategory.REVIEW to FileType.LARGE
            } else {
                CleanCategory.SENSITIVE to FileType.MEDIA_VIDEO
            }
        }

        if (AUDIO_EXTENSIONS.contains(ext)) {
            return CleanCategory.SENSITIVE to FileType.MEDIA_AUDIO
        }

        if (DOCUMENT_EXTENSIONS.contains(ext)) {
            return CleanCategory.SENSITIVE to FileType.DOCUMENT
        }

        if (ARCHIVE_EXTENSIONS.contains(ext)) {
            return CleanCategory.REVIEW to FileType.ARCHIVE
        }

        // 9. Old unaccessed files
        if (modified < oldThresholdTimestamp) {
            return CleanCategory.REVIEW to FileType.OLD
        }

        return CleanCategory.REVIEW to FileType.OTHER
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
