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

    @Test
    fun testAppSortingBySizeDescAndAsc() {
        val app1 = com.example.model.AppStorageInfo(packageName = "com.a", appName = "Alpha", totalSize = 1000L)
        val app2 = com.example.model.AppStorageInfo(packageName = "com.b", appName = "Beta", totalSize = 5000L)
        val app3 = com.example.model.AppStorageInfo(packageName = "com.c", appName = "Gamma", totalSize = 2500L)
        val list = listOf(app1, app2, app3)

        val desc = list.sortedByDescending { it.totalSize }
        assertEquals("com.b", desc[0].packageName)
        assertEquals("com.c", desc[1].packageName)
        assertEquals("com.a", desc[2].packageName)

        val asc = list.sortedBy { it.totalSize }
        assertEquals("com.a", asc[0].packageName)
        assertEquals("com.c", asc[1].packageName)
        assertEquals("com.b", asc[2].packageName)
    }

    @Test
    fun testAppSortingByInstallTimeDescAndAsc() {
        val app1 = com.example.model.AppStorageInfo(packageName = "com.a", appName = "OldApp", installTime = 100000L)
        val app2 = com.example.model.AppStorageInfo(packageName = "com.b", appName = "NewestApp", installTime = 500000L)
        val app3 = com.example.model.AppStorageInfo(packageName = "com.c", appName = "MidApp", installTime = 300000L)
        val list = listOf(app1, app2, app3)

        val newestFirst = list.sortedByDescending { it.installTime }
        assertEquals("com.b", newestFirst[0].packageName)
        assertEquals("com.c", newestFirst[1].packageName)
        assertEquals("com.a", newestFirst[2].packageName)

        val oldestFirst = list.sortedBy { it.installTime }
        assertEquals("com.a", oldestFirst[0].packageName)
        assertEquals("com.c", oldestFirst[1].packageName)
        assertEquals("com.b", oldestFirst[2].packageName)
    }

    @Test
    fun testAppSortingByNameDescAndAsc() {
        val app1 = com.example.model.AppStorageInfo(packageName = "com.z", appName = "Zebra")
        val app2 = com.example.model.AppStorageInfo(packageName = "com.a", appName = "Ant")
        val app3 = com.example.model.AppStorageInfo(packageName = "com.m", appName = "Monkey")
        val list = listOf(app1, app2, app3)

        val asc = list.sortedBy { it.appName.lowercase() }
        assertEquals("Ant", asc[0].appName)
        assertEquals("Monkey", asc[1].appName)
        assertEquals("Zebra", asc[2].appName)

        val desc = list.sortedByDescending { it.appName.lowercase() }
        assertEquals("Zebra", desc[0].appName)
        assertEquals("Monkey", desc[1].appName)
        assertEquals("Ant", desc[2].appName)
    }

    @Test
    fun testAppSearchFiltering() {
        val apps = listOf(
            com.example.model.AppStorageInfo(packageName = "com.whatsapp", appName = "WhatsApp"),
            com.example.model.AppStorageInfo(packageName = "org.telegram.messenger", appName = "Telegram"),
            com.example.model.AppStorageInfo(packageName = "com.google.android.youtube", appName = "YouTube")
        )

        val query1 = "tele"
        val filtered1 = apps.filter { it.appName.lowercase().contains(query1) || it.packageName.lowercase().contains(query1) }
        assertEquals(1, filtered1.size)
        assertEquals("Telegram", filtered1[0].appName)

        val query2 = "google"
        val filtered2 = apps.filter { it.appName.lowercase().contains(query2) || it.packageName.lowercase().contains(query2) }
        assertEquals(1, filtered2.size)
        assertEquals("YouTube", filtered2[0].appName)

        val queryEmpty = ""
        val filteredEmpty = apps.filter { queryEmpty.isBlank() || it.appName.lowercase().contains(queryEmpty) }
        assertEquals(3, filteredEmpty.size)
    }

    @Test
    fun testSafeJunkExcludesZeroByteFilesAndDirectories() {
        val items = listOf(
            StorageItem(id = 1, path = "/a/cache.tmp", name = "cache.tmp", size = 15000L, lastModified = 0L, isDirectory = false),
            StorageItem(id = 2, path = "/a/empty.log", name = "empty.log", size = 0L, lastModified = 0L, isDirectory = false),
            StorageItem(id = 3, path = "/a/empty_dir", name = "empty_dir", size = 0L, lastModified = 0L, isDirectory = true),
            StorageItem(id = 4, path = "/b/crash.dmp", name = "crash.dmp", size = 45000L, lastModified = 0L, isDirectory = false)
        )

        val filteredSafeJunk = items.filter { !it.isDirectory && it.size > 0L }.sortedByDescending { it.size }
        assertEquals(2, filteredSafeJunk.size)
        assertEquals(45000L, filteredSafeJunk[0].size)
        assertEquals(15000L, filteredSafeJunk[1].size)
    }

    @Test
    fun testSelectedAppsCacheAndDataAggregation() {
        val app1 = com.example.model.AppStorageInfo(
            packageName = "com.app.one",
            appName = "App One",
            codeSize = 50_000_000L,
            dataSize = 20_000_000L,
            cacheSize = 10_000_000L,
            totalSize = 80_000_000L
        )
        val app2 = com.example.model.AppStorageInfo(
            packageName = "com.app.two",
            appName = "App Two",
            codeSize = 30_000_000L,
            dataSize = 15_000_000L,
            cacheSize = 5_000_000L,
            totalSize = 50_000_000L
        )
        val selectedApps = listOf(app1, app2)

        val totalCache = selectedApps.sumOf { it.cacheSize }
        val totalData = selectedApps.sumOf { it.dataSize }
        val totalSpace = selectedApps.sumOf { it.totalSize }

        assertEquals(15_000_000L, totalCache)
        assertEquals(35_000_000L, totalData)
        assertEquals(130_000_000L, totalSpace)
    }

    @Test
    fun testDuplicateGroupsSortingByWastedBytes() {
        val group1 = com.example.model.DuplicateGroup(
            hash = "hash1",
            singleSize = 2_000_000L,
            wastedBytes = 4_000_000L,
            items = emptyList()
        )
        val group2 = com.example.model.DuplicateGroup(
            hash = "hash2",
            singleSize = 10_000_000L,
            wastedBytes = 20_000_000L,
            items = emptyList()
        )
        val group3 = com.example.model.DuplicateGroup(
            hash = "hash3",
            singleSize = 500_000L,
            wastedBytes = 1_000_000L,
            items = emptyList()
        )

        val groups = listOf(group1, group2, group3)
        val sortedDesc = groups.sortedByDescending { it.wastedBytes }
        assertEquals("hash2", sortedDesc[0].hash)
        assertEquals("hash1", sortedDesc[1].hash)
        assertEquals("hash3", sortedDesc[2].hash)

        val sortedAsc = groups.sortedBy { it.wastedBytes }
        assertEquals("hash3", sortedAsc[0].hash)
        assertEquals("hash1", sortedAsc[1].hash)
        assertEquals("hash2", sortedAsc[2].hash)
    }

    @Test
    fun testAppSelectionToggles() {
        val selected = mutableSetOf<String>()

        fun toggle(pkg: String) {
            if (selected.contains(pkg)) selected.remove(pkg) else selected.add(pkg)
        }

        toggle("com.test.a")
        assertTrue(selected.contains("com.test.a"))

        toggle("com.test.b")
        assertEquals(2, selected.size)

        toggle("com.test.a")
        assertFalse(selected.contains("com.test.a"))
        assertEquals(1, selected.size)
        assertTrue(selected.contains("com.test.b"))
    }
}

