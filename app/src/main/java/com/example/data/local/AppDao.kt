package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Sites / Sources
    @Query("SELECT * FROM sites ORDER BY sortOrder ASC, addedAt ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE isEnabled = 1 ORDER BY sortOrder ASC, addedAt ASC")
    fun getEnabledSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites ORDER BY sortOrder ASC, addedAt ASC")
    suspend fun getAllSitesOnce(): List<SiteEntity>

    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    suspend fun getSiteById(id: Long): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity): Long

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Delete
    suspend fun deleteSite(site: SiteEntity)

    @Query("DELETE FROM sites WHERE id = :id")
    suspend fun deleteSiteById(id: Long)

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAllFavoritesOnce(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE url = :url")
    suspend fun deleteFavoriteByUrl(url: String)

    // Favorite Categories
    @Query("SELECT * FROM favorite_categories ORDER BY sortOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<FavoriteCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: FavoriteCategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: FavoriteCategoryEntity)

    // History
    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    suspend fun getAllHistoryOnce(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    // Saved Works (Legacy / Imported Files)
    @Query("SELECT * FROM saved_works ORDER BY createdAt DESC")
    fun getAllSavedWorks(): Flow<List<SavedWorkEntity>>

    @Query("SELECT * FROM saved_works ORDER BY createdAt DESC")
    suspend fun getAllSavedWorksOnce(): List<SavedWorkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedWork(work: SavedWorkEntity): Long

    @Delete
    suspend fun deleteSavedWork(work: SavedWorkEntity)

    @Query("DELETE FROM saved_works")
    suspend fun clearSavedWorks()

    // Download Tasks
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllDownloadTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    suspend fun getAllDownloadTasksOnce(): List<DownloadTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadTask(task: DownloadTaskEntity): Long

    @Update
    suspend fun updateDownloadTask(task: DownloadTaskEntity)

    @Delete
    suspend fun deleteDownloadTask(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks")
    suspend fun clearDownloadTasks()

    // Downloaded Works (Offline Manga Library in التخزين)
    @Query("SELECT * FROM downloaded_works ORDER BY updatedAt DESC")
    fun getAllDownloadedWorks(): Flow<List<DownloadedWorkEntity>>

    @Query("SELECT * FROM downloaded_works ORDER BY updatedAt DESC")
    suspend fun getAllDownloadedWorksOnce(): List<DownloadedWorkEntity>

    @Query("SELECT * FROM downloaded_works WHERE workId = :workId LIMIT 1")
    suspend fun getDownloadedWorkById(workId: String): DownloadedWorkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadedWork(work: DownloadedWorkEntity)

    @Query("DELETE FROM downloaded_works WHERE workId = :workId")
    suspend fun deleteDownloadedWorkById(workId: String)

    // Downloaded Chapters (Offline Chapter Files & Status)
    @Query("SELECT * FROM downloaded_chapters ORDER BY downloadedAt DESC")
    fun getAllDownloadedChapters(): Flow<List<DownloadedChapterEntity>>

    @Query("SELECT * FROM downloaded_chapters WHERE workId = :workId ORDER BY chapterNumberValue ASC, chapterNumber ASC")
    fun getDownloadedChaptersForWorkFlow(workId: String): Flow<List<DownloadedChapterEntity>>

    @Query("SELECT * FROM downloaded_chapters WHERE workId = :workId ORDER BY chapterNumberValue ASC, chapterNumber ASC")
    suspend fun getDownloadedChaptersForWorkOnce(workId: String): List<DownloadedChapterEntity>

    @Query("SELECT * FROM downloaded_chapters WHERE chapterKey = :chapterKey LIMIT 1")
    suspend fun getDownloadedChapterByKey(chapterKey: String): DownloadedChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadedChapter(chapter: DownloadedChapterEntity)

    @Query("DELETE FROM downloaded_chapters WHERE chapterKey = :chapterKey")
    suspend fun deleteDownloadedChapterByKey(chapterKey: String)

    @Query("DELETE FROM downloaded_chapters WHERE workId = :workId")
    suspend fun deleteDownloadedChaptersForWork(workId: String)

    @Query("DELETE FROM downloaded_chapters WHERE status IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    suspend fun clearCompletedAndCancelledChaptersRecord()

    // Reading Progress
    @Query("SELECT * FROM reading_progress ORDER BY lastReadAt DESC")
    fun getAllReadingProgress(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE workId = :workId ORDER BY lastReadAt DESC")
    fun getReadingProgressForWorkFlow(workId: String): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE workId = :workId ORDER BY lastReadAt DESC")
    suspend fun getReadingProgressForWorkOnce(workId: String): List<ReadingProgressEntity>

    @Query("SELECT * FROM reading_progress WHERE chapterKey = :chapterKey LIMIT 1")
    suspend fun getReadingProgressByKey(chapterKey: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress ORDER BY lastReadAt DESC LIMIT 1")
    suspend fun getLatestReadingProgressOnce(): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: ReadingProgressEntity)

    // Latest Updates Cache
    @Query("SELECT * FROM latest_updates ORDER BY CASE WHEN publishedAtEpochMillis IS NOT NULL AND publishedAtEpochMillis > 0 THEN 0 ELSE 1 END ASC, publishedAtEpochMillis DESC, originalOrderIndex ASC, syncedAt DESC")
    fun getAllLatestUpdatesFlow(): Flow<List<LatestUpdateEntity>>

    @Query("SELECT * FROM latest_updates WHERE sourceId = :sourceId ORDER BY CASE WHEN publishedAtEpochMillis IS NOT NULL AND publishedAtEpochMillis > 0 THEN 0 ELSE 1 END ASC, publishedAtEpochMillis DESC, originalOrderIndex ASC, syncedAt DESC")
    fun getLatestUpdatesForSourceFlow(sourceId: Long): Flow<List<LatestUpdateEntity>>

    @Query("SELECT * FROM latest_updates WHERE sourceId = :sourceId ORDER BY CASE WHEN publishedAtEpochMillis IS NOT NULL AND publishedAtEpochMillis > 0 THEN 0 ELSE 1 END ASC, publishedAtEpochMillis DESC, originalOrderIndex ASC, syncedAt DESC")
    suspend fun getLatestUpdatesForSourceOnce(sourceId: Long): List<LatestUpdateEntity>

    @Query("SELECT * FROM latest_updates ORDER BY CASE WHEN publishedAtEpochMillis IS NOT NULL AND publishedAtEpochMillis > 0 THEN 0 ELSE 1 END ASC, publishedAtEpochMillis DESC, originalOrderIndex ASC, syncedAt DESC")
    suspend fun getAllLatestUpdatesOnce(): List<LatestUpdateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLatestUpdates(items: List<LatestUpdateEntity>)

    @Query("DELETE FROM latest_updates WHERE sourceId = :sourceId")
    suspend fun clearLatestUpdatesForSource(sourceId: Long)

    @Query("DELETE FROM latest_updates")
    suspend fun clearAllLatestUpdates()
}
