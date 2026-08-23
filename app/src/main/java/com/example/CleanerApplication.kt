package com.example

import android.app.Application
import com.example.data.local.CleanerDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.AppRepository
import com.example.data.repository.ExclusionRepository
import com.example.data.repository.HistoryRepository
import com.example.data.repository.RecycleBinRepository
import com.example.data.repository.StorageRepository

class CleanerApplication : Application() {

    lateinit var database: CleanerDatabase
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var storageRepository: StorageRepository
        private set

    lateinit var appRepository: AppRepository
        private set

    lateinit var recycleBinRepository: RecycleBinRepository
        private set

    lateinit var exclusionRepository: ExclusionRepository
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = CleanerDatabase.getDatabase(this)
        settingsDataStore = SettingsDataStore(this)
        exclusionRepository = ExclusionRepository(database.exclusionDao())
        recycleBinRepository = RecycleBinRepository(this, database.recycleBinDao())
        historyRepository = HistoryRepository(database.historyDao())
        appRepository = AppRepository(this)
        storageRepository = StorageRepository(
            context = this,
            fileDao = database.fileDao(),
            exclusionRepository = exclusionRepository,
            recycleBinRepository = recycleBinRepository,
            historyRepository = historyRepository,
            settingsDataStore = settingsDataStore
        )
    }

    companion object {
        lateinit var instance: CleanerApplication
            private set
    }
}
