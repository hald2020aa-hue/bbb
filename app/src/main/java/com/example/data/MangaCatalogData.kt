package com.example.data

import com.example.data.local.SiteEntity

data class MangaChapter(
    val id: String,
    val number: Int,
    val title: String,
    val dateText: String,
    val chapterUrl: String = "",
    val pageImageUrls: List<String> = emptyList(),
    val localPagePaths: List<String> = emptyList(),
    val pageCount: Int = 0,
    val downloadedPagesCount: Int = 0,
    val isDownloaded: Boolean = false,
    val downloadStatus: String = "", // "", "QUEUED", "DOWNLOADING", "PAUSED", "COMPLETED", "INCOMPLETE", "FAILED", "CANCELLED"
    val downloadError: String = "",
    val isRead: Boolean = false,
    val lastReadPage: Int = -1,
    val lastReadTimeText: String = "",
    val chapterKey: String = "",
    val chapterNumberValue: Double = number.toDouble(),
    val numberLabel: String = ""
) {
    val displayNumber: String
        get() = when {
            numberLabel.isNotBlank() -> numberLabel
            chapterNumberValue % 1.0 == 0.0 -> chapterNumberValue.toInt().toString()
            else -> chapterNumberValue.toString().trimEnd('0').trimEnd('.')
        }
}

data class MangaLatestUpdateItem(
    val updateKey: String,
    val sourceId: Long,
    val sourceName: String,
    val sourceUrl: String,
    val siteIconKey: String = "globe",
    val workId: String,
    val workTitle: String,
    val mangaUrl: String,
    val coverImageUrl: String? = null,
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
) {
    val displayChapterNumber: String
        get() = when {
            chapterNumberLabel.isNotBlank() -> chapterNumberLabel
            chapterNumberValue % 1.0 == 0.0 -> chapterNumberValue.toInt().toString()
            else -> chapterNumberValue.toString().trimEnd('0').trimEnd('.')
        }

    val displayDateOrFallback: String
        get() = if (isDateAvailable && publishDateText.isNotBlank()) publishDateText else "التاريخ غير متاح"

    val id: String
        get() = updateKey

    val sourceIconKey: String
        get() = siteIconKey

    val workUrl: String
        get() = mangaUrl
}

data class SourceUpdateSyncStatus(
    val sourceId: Long,
    val sourceName: String,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = true,
    val updatesCount: Int = 0,
    val errorMessage: String? = null,
    val lastSyncedAt: Long = 0L
)

data class MangaWork(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val mangaUrl: String = "",
    val arabicBannerTitle: String = "",
    val sourceId: Long = 0L,
    val siteName: String,
    val siteUrl: String,
    val siteIconKey: String = "globe",
    val status: String = "OnGoing",
    val type: String = "مانهوا",
    val rating: String = "4.5",
    val releaseYear: String = "2025",
    val author: String = "",
    val artist: String = "",
    val viewsText: String = "",
    val chaptersCount: Int = 0,
    val savedChaptersCount: Int = 0,
    val incompleteChaptersCount: Int = 0,
    val genres: List<String> = emptyList(),
    val description: String = "",
    val synopsis: String = description,
    val coverStyleIndex: Int = 0,
    val coverImageUrl: String? = null,
    val coverUrl: String? = coverImageUrl,
    val localCoverPath: String = "",
    val isFromLocalStorage: Boolean = false,
    val storageSizeBytes: Long = 0L,
    val chapters: List<MangaChapter> = emptyList(),
    val isDetailsLoaded: Boolean = false
) {
    val effectiveCoverUrl: String?
        get() = coverImageUrl?.takeIf { it.isNotBlank() } ?: coverUrl?.takeIf { it.isNotBlank() }
}

object MangaCatalogData {

    val siteCategoryChips: List<String> = listOf(
        "التصنيفات",
        "دراما",
        "أكشن",
        "خيال",
        "فانتازيا",
        "رومانسي",
        "مغامرة",
        "كوميدي",
        "إثارة",
        "إيسيكاي",
        "سحر",
        "فنون قتالية",
        "نظام",
        "شونين",
        "شوجو",
        "مانهوا"
    )

    // Empty as requested: user adds their own manga sites manually
    val defaultAddedSites: List<SiteEntity> = emptyList()

    // Empty as requested: storage only displays works saved/downloaded by the user
    val storageLibraryWorks: List<MangaWork> = emptyList()

    fun getCatalogForSite(site: SiteEntity): List<MangaWork> = emptyList()
}
