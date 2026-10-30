package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val domain: String,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val iconKey: String = "globe",
    val lastVisitedUrl: String = "",
    val adapterKey: String = "auto",
    val adapterType: String = adapterKey,
    val supportsCatalog: Boolean = true,
    val supportsChapterDownload: Boolean = true,
    val connectionStatus: String = "متصل",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val siteName: String,
    val category: String = "الكل",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_categories")
data class FavoriteCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val siteName: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_works")
data class SavedWorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val fileType: String, // "DOWNLOAD", "PAGE", "IMPORT"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val status: String, // "QUEUED", "DOWNLOADING", "COMPLETED", "FAILED", "PAUSED", "CANCELLED", "INCOMPLETE"
    val progress: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val localPath: String = "",
    val errorMessage: String? = null,
    val workId: String = "",
    val chapterKey: String = "",
    val chapterId: String = "",
    val chapterNumber: Int = 1,
    val sourceName: String = "",
    val coverPath: String = "",
    val coverUrl: String = coverPath,
    val downloadedPages: Int = 0,
    val totalPages: Int = 1,
    val etaSeconds: Int = 0,
    val chaptersCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_works")
data class DownloadedWorkEntity(
    @PrimaryKey val workId: String,
    val sourceId: Long = 0L,
    val sourceName: String,
    val sourceUrl: String = "",
    val siteIconKey: String = "globe",
    val title: String,
    val subtitle: String = "",
    val mangaUrl: String = "",
    val remoteCoverUrl: String = "",
    val coverUrl: String = remoteCoverUrl,
    val localCoverPath: String = "",
    val synopsis: String = "",
    val genresCsv: String = "",
    val statusText: String = "OnGoing",
    val status: String = statusText,
    val type: String = "مانهوا",
    val author: String = "",
    val rating: String = "4.5",
    val totalChaptersOnSite: Int = 0,
    val savedChaptersCount: Int = 0,
    val totalStorageBytes: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_chapters")
data class DownloadedChapterEntity(
    @PrimaryKey val chapterKey: String,
    val workId: String,
    val sourceId: Long = 0L,
    val sourceName: String = "",
    val workTitle: String = "",
    val chapterId: String = "",
    val chapterNumber: Int,
    val chapterNumberValue: Double = chapterNumber.toDouble(),
    val chapterNumberLabel: String = "",
    val chapterTitle: String,
    val chapterUrl: String = "",
    val dateText: String = "",
    val pageCount: Int = 0,
    val downloadedPagesCount: Int = 0,
    val localPagesJson: String = "[]",
    val totalBytes: Long = 0L,
    val status: String = "QUEUED", // "QUEUED", "DOWNLOADING", "PAUSED", "COMPLETED", "FAILED", "CANCELLED", "INCOMPLETE"
    val progressPercent: Float = 0f,
    val etaSeconds: Int = 0,
    val errorMessage: String? = null,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    val localPagePathsJson: String get() = localPagesJson
    val totalPages: Int get() = pageCount
    val downloadedPages: Int get() = downloadedPagesCount
}

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val chapterKey: String,
    val workId: String,
    val workTitle: String,
    val sourceId: Long = 0L,
    val chapterId: String,
    val chapterNumber: Int,
    val chapterTitle: String,
    val lastPageIndex: Int = 0,
    val scrollOffset: Int = 0,
    val totalPages: Int = 1,
    val isCompleted: Boolean = false,
    val lastReadAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "latest_updates")
data class LatestUpdateEntity(
    @PrimaryKey val updateKey: String,
    val sourceId: Long,
    val sourceName: String,
    val sourceUrl: String,
    val siteIconKey: String = "globe",
    val workId: String,
    val workTitle: String,
    val mangaUrl: String,
    val coverImageUrl: String = "",
    val chapterId: String,
    val chapterTitle: String,
    val chapterNumber: Int,
    val chapterNumberValue: Double = chapterNumber.toDouble(),
    val chapterNumberLabel: String = "",
    val chapterUrl: String,
    val publishDateText: String = "",
    val publishedAtEpochMillis: Long? = null,
    val isDateAvailable: Boolean = publishDateText.isNotBlank(),
    val originalOrderIndex: Int = 0,
    val syncedAt: Long = System.currentTimeMillis()
)
