package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SiteEntity::class,
        FavoriteEntity::class,
        FavoriteCategoryEntity::class,
        HistoryEntity::class,
        SavedWorkEntity::class,
        DownloadTaskEntity::class,
        DownloadedWorkEntity::class,
        DownloadedChapterEntity::class,
        ReadingProgressEntity::class,
        LatestUpdateEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "manga_sites_reader.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
