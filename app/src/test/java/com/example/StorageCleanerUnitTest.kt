package com.example

import com.example.model.CleanCategory
import com.example.model.Exclusion
import com.example.model.ExclusionType
import com.example.model.FileType
import com.example.model.StorageItem
import com.example.util.FileClassifier
import com.example.util.HashUtils
import com.example.util.StorageUtils
import com.example.util.TopKMinHeap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageCleanerUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFileClassifierSafeJunk() {
        val (apkCategory, apkType) = FileClassifier.classify(
            path = "/storage/emulated/0/Download/test.apk",
            name = "test.apk",
            extension = "apk",
            size = 15_000_000L
        )
        assertEquals(CleanCategory.SAFE_JUNK, apkCategory)
        assertEquals(FileType.APK, apkType)

        val (logCategory, logType) = FileClassifier.classify(
            path = "/storage/emulated/0/Android/data/app/cache/app.log",
            name = "app.log",
            extension = "log",
            size = 500L
        )
        assertEquals(CleanCategory.SAFE_JUNK, logCategory)
        assertEquals(FileType.CACHE, logType)
    }

    @Test
    fun testFileClassifierSensitive() {
        val (photoCategory, photoType) = FileClassifier.classify(
            path = "/storage/emulated/0/DCIM/Camera/IMG_20260823.jpg",
            name = "IMG_20260823.jpg",
            extension = "jpg",
            size = 3_500_000L,
            lastModified = System.currentTimeMillis()
        )
        assertEquals(CleanCategory.SENSITIVE, photoCategory)
        assertEquals(FileType.MEDIA_IMAGE, photoType)

        val (docCategory, docType) = FileClassifier.classify(
            path = "/storage/emulated/0/Documents/tax_return.pdf",
            name = "tax_return.pdf",
            extension = "pdf",
            size = 1_200_000L
        )
        assertEquals(CleanCategory.SENSITIVE, docCategory)
        assertEquals(FileType.DOCUMENT, docType)
    }

    @Test
    fun testFileClassifierReview() {
        val (archiveCategory, archiveType) = FileClassifier.classify(
            path = "/storage/emulated/0/Other/report.zip",
            name = "report.zip",
            extension = "zip",
            size = 4_000_000L
        )
        assertEquals(CleanCategory.REVIEW, archiveCategory)
        assertEquals(FileType.ARCHIVE, archiveType)
    }

    @Test
    fun testExclusionMatching() {
        val exclusions = listOf(
            Exclusion(id = 1, pattern = "DCIM", type = ExclusionType.FOLDER_NAME, enabled = true),
            Exclusion(id = 2, pattern = ".nomedia", type = ExclusionType.EXTENSION, enabled = true),
            Exclusion(id = 3, pattern = "backup", type = ExclusionType.KEYWORD, enabled = true)
        )

        assertTrue(FileClassifier.isExcluded(File("/storage/emulated/0/DCIM/Camera/photo.jpg"), exclusions))
        assertTrue(FileClassifier.isExcluded(File("/storage/emulated/0/WhatsApp/.nomedia"), exclusions))
        assertTrue(FileClassifier.isExcluded(File("/storage/emulated/0/Download/my_backup_2026.zip"), exclusions))
        assertFalse(FileClassifier.isExcluded(File("/storage/emulated/0/Download/invoice.pdf"), exclusions))
    }

    @Test
    fun testHashingUtils() {
        val sampleFile = tempFolder.newFile("sample.txt").apply {
            writeText("Storage Cleaner test content for deterministic hashing.")
        }

        val partialHash = HashUtils.calculatePartialHash(sampleFile, 1024)
        val fullHash = HashUtils.calculateSha256(sampleFile)

        assertNotNull(partialHash)
        assertNotNull(fullHash)
        assertEquals(64, fullHash?.length) // SHA-256 is 64 hex characters
    }

    @Test
    fun testHammingDistance() {
        val hashA = 0b10101010L
        val hashB = 0b10101011L // 1 bit difference
        val hashC = 0b01010101L // all 8 bits different

        assertEquals(1, HashUtils.hammingDistance(hashA, hashB))
        assertEquals(8, HashUtils.hammingDistance(hashA, hashC))
    }

    @Test
    fun testTopKMinHeap() {
        val heap = TopKMinHeap<StorageItem>(3, compareBy { it.size })
        heap.add(StorageItem(id = 1, path = "/a", name = "a", size = 100L, lastModified = 0L))
        heap.add(StorageItem(id = 2, path = "/b", name = "b", size = 500L, lastModified = 0L))
        heap.add(StorageItem(id = 3, path = "/c", name = "c", size = 300L, lastModified = 0L))
        heap.add(StorageItem(id = 4, path = "/d", name = "d", size = 900L, lastModified = 0L))
        heap.add(StorageItem(id = 5, path = "/e", name = "e", size = 50L, lastModified = 0L))

        val top3 = heap.toList()
        assertEquals(3, top3.size)
        assertEquals(900L, top3[0].size)
        assertEquals(500L, top3[1].size)
        assertEquals(300L, top3[2].size)
    }

    @Test
    fun testStorageFormatting() {
        assertEquals("0 B", StorageUtils.formatBytes(0L))
        assertEquals("1 KB", StorageUtils.formatBytes(1024L))
        assertEquals("1.5 MB", StorageUtils.formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2 GB", StorageUtils.formatBytes(2L * 1024 * 1024 * 1024))
    }
}
