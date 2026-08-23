package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ExclusionDao
import com.example.data.local.dao.FileDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.dao.RecycleBinDao
import com.example.data.local.entity.ExclusionEntity
import com.example.data.local.entity.FileEntity
import com.example.data.local.entity.FileFtsEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.RecycleBinEntity

@Database(
    entities = [
        FileEntity::class,
        FileFtsEntity::class,
        ExclusionEntity::class,
        RecycleBinEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CleanerDatabase : RoomDatabase() {

    abstract fun fileDao(): FileDao
    abstract fun exclusionDao(): ExclusionDao
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CleanerDatabase? = null

        fun getDatabase(context: Context): CleanerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CleanerDatabase::class.java,
                    "storage_cleaner.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
