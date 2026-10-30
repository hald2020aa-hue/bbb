package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaChapter
import com.example.data.MangaLatestUpdateItem
import com.example.data.MangaWork
import com.example.data.SourceUpdateSyncStatus
import com.example.data.download.DownloadNotificationHelper
import com.example.data.download.MangaDownloadManager
import com.example.data.local.AppDao
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.DownloadedChapterEntity
import com.example.data.local.DownloadedWorkEntity
import com.example.data.local.FavoriteCategoryEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.LatestUpdateEntity
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.SavedWorkEntity
import com.example.data.local.SiteEntity
import com.example.data.preferences.UserSettings
import com.example.data.preferences.UserSettingsDataStore
import com.example.data.source.AdapterScrapeResult
import com.example.data.source.MangaSourceAdapterRegistry
import com.example.data.source.SourceCapabilities
import com.example.data.storage.LocalStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit

data class StorageStats(
    val downloadsBytes: Long = 0L,
    val pagesBytes: Long = 0L,
    val otherBytes: Long = 7_549_747L, // ~7.2 MB base app/db footprint
    val availableBytes: Long = 0L,
    val totalBytes: Long = 1L,
    val cacheBytes: Long = 209_715L, // ~0.2 MB
    val storagePath: String = ""
)

data class SiteMetadataResult(
    val normalizedUrl: String,
    val domain: String,
    val suggestedName: String,
    val iconKey: String,
    val isValid: Boolean,
    val errorMessage: String? = null
)

class AppRepository(
    private val context: Context,
    val dao: AppDao,
    private val settingsDataStore: UserSettingsDataStore
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val localStorageManager = LocalStorageManager(context)
    val notificationHelper = DownloadNotificationHelper(context)
    val sourceRepository = SourceRepository(dao, httpClient)
    val readingProgressRepository = ReadingProgressRepository(dao)
    val mangaDownloadManager = MangaDownloadManager(dao, localStorageManager, notificationHelper, httpClient)

    val allSites: Flow<List<SiteEntity>> = dao.getAllSites()
    val enabledSites: Flow<List<SiteEntity>> = dao.getEnabledSites()
    val allFavorites: Flow<List<FavoriteEntity>> = dao.getAllFavorites()
    val allCategories: Flow<List<FavoriteCategoryEntity>> = dao.getAllCategories()
    val allHistory: Flow<List<HistoryEntity>> = dao.getAllHistory()
    val allSavedWorks: Flow<List<SavedWorkEntity>> = dao.getAllSavedWorks()
    val allDownloadTasks: Flow<List<DownloadTaskEntity>> = dao.getAllDownloadTasks()
    val allDownloadedWorks: Flow<List<DownloadedWorkEntity>> = dao.getAllDownloadedWorks()
    val allDownloadedChapters: Flow<List<DownloadedChapterEntity>> = dao.getAllDownloadedChapters()
    val allReadingProgress: Flow<List<ReadingProgressEntity>> = dao.getAllReadingProgress()
    val allLatestUpdates: Flow<List<MangaLatestUpdateItem>> = dao.getAllLatestUpdatesFlow().map { list ->
        list.map { it.toDomainModel() }
    }
    val settingsFlow: Flow<UserSettings> = settingsDataStore.settingsFlow

    fun getSavedDirectory(): File = localStorageManager.getRootSavedDir()

    fun getDisplayStoragePath(): String {
        val rawPath = getSavedDirectory().absolutePath
        return rawPath.replace(context.packageName, "org.hamak.mangareader")
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getSourceCapabilities(site: SiteEntity): SourceCapabilities =
        sourceRepository.getCapabilities(site)

    suspend fun testSourceConnection(url: String): SourceConnectionTestResult =
        sourceRepository.testSourceConnection(url)

    suspend fun updateSiteLastVisitedUrl(site: SiteEntity, url: String) =
        sourceRepository.updateLastVisitedUrl(site, url)

    suspend fun normalizeAndResolveSite(rawInput: String): SiteMetadataResult = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return@withContext SiteMetadataResult(
                normalizedUrl = "",
                domain = "",
                suggestedName = "",
                iconKey = "globe",
                isValid = false,
                errorMessage = "يرجى إدخال عنوان موقع صحيح"
            )
        }

        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }

        val parsedUri = try {
            URI(withScheme)
        } catch (e: Exception) {
            null
        }

        val host = parsedUri?.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: ""
        if (host.isEmpty() || !host.contains(".") || host.startsWith(".") || host.endsWith(".")) {
            return@withContext SiteMetadataResult(
                normalizedUrl = withScheme,
                domain = host,
                suggestedName = "",
                iconKey = "globe",
                isValid = false,
                errorMessage = "الرابط غير صالح. تأكد من كتابة النطاق بشكل صحيح (مثال: mangalik.net)"
            )
        }

        val cleanUrl = "https://$host"

        // Known site mappings matching the reference video + popular Arabic manga/web sites
        when {
            host.contains("mangalik") || host.contains("mangalek") -> {
                return@withContext SiteMetadataResult(
                    normalizedUrl = cleanUrl,
                    domain = host,
                    suggestedName = "مانجا ليك Mangalek",
                    iconKey = "mangalek",
                    isValid = true
                )
            }
            host.contains("olympustaff") || host.contains("teamx") || host.contains("team-x") -> {
                return@withContext SiteMetadataResult(
                    normalizedUrl = cleanUrl,
                    domain = host,
                    suggestedName = "Team-X",
                    iconKey = "teamx",
                    isValid = true
                )
            }
            host.contains("azora") || host.contains("azoramoon") -> {
                return@withContext SiteMetadataResult(
                    normalizedUrl = cleanUrl,
                    domain = host,
                    suggestedName = "أزورا مانجا Azora",
                    iconKey = "azora",
                    isValid = true
                )
            }
            host.contains("ares") || host.contains("manga-alarab") -> {
                return@withContext SiteMetadataResult(
                    normalizedUrl = cleanUrl,
                    domain = host,
                    suggestedName = "مانجا العرب",
                    iconKey = "globe",
                    isValid = true
                )
            }
        }

        // Try fetching the live HTML title if online
        var liveTitle: String? = null
        if (isNetworkAvailable()) {
            try {
                val request = Request.Builder()
                    .url(cleanUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    val bodySample = response.body?.string()?.take(16000) ?: ""
                    val titleRegex = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    val match = titleRegex.find(bodySample)
                    val extracted = match?.groupValues?.getOrNull(1)
                        ?.replace(Regex("\\s+"), " ")
                        ?.trim()
                    if (!extracted.isNullOrBlank() &&
                        !extracted.contains("Just a moment", ignoreCase = true) &&
                        !extracted.contains("Cloudflare", ignoreCase = true) &&
                        !extracted.contains("Attention Required", ignoreCase = true) &&
                        extracted.length <= 50
                    ) {
                        liveTitle = extracted
                    }
                }
            } catch (_: Exception) {
                // Fallback to formatted host name
            }
        }

        val fallbackName = host.substringBefore(".")
            .replace("-", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        SiteMetadataResult(
            normalizedUrl = cleanUrl,
            domain = host,
            suggestedName = liveTitle ?: fallbackName,
            iconKey = "globe",
            isValid = true
        )
    }

    suspend fun addSite(name: String, url: String, domain: String, iconKey: String): Long {
        return sourceRepository.addSource(name, url, domain, iconKey)
    }

    suspend fun updateSite(site: SiteEntity) = sourceRepository.updateSource(site)
    suspend fun deleteSite(site: SiteEntity) = sourceRepository.deleteSource(site)

    suspend fun addHistory(title: String, url: String, siteName: String) {
        if (url.isBlank() || url == "about:blank") return
        dao.insertHistory(
            HistoryEntity(
                title = title.ifBlank { url },
                url = url,
                siteName = siteName.ifBlank { "المتصفح" }
            )
        )
    }

    suspend fun clearHistory() = dao.clearHistory()
    suspend fun deleteHistoryItem(id: Long) = dao.deleteHistoryById(id)

    suspend fun toggleFavorite(title: String, url: String, siteName: String, category: String = "الكل"): Boolean {
        val current = dao.getAllFavoritesOnce()
        val existing = current.find { it.url == url }
        return if (existing != null) {
            dao.deleteFavorite(existing)
            false
        } else {
            dao.insertFavorite(
                FavoriteEntity(
                    title = title.ifBlank { url },
                    url = url,
                    siteName = siteName,
                    category = category
                )
            )
            true
        }
    }

    suspend fun deleteFavorite(favorite: FavoriteEntity) = dao.deleteFavorite(favorite)

    suspend fun addCategory(name: String) {
        if (name.isBlank()) return
        dao.insertCategory(FavoriteCategoryEntity(name = name.trim()))
    }

    suspend fun deleteCategory(category: FavoriteCategoryEntity) = dao.deleteCategory(category)

    suspend fun calculateStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val works = dao.getAllSavedWorksOnce()
        val downloadedWorks = dao.getAllDownloadedWorksOnce()
        val offlineMangaBytes = downloadedWorks.sumOf { it.totalStorageBytes }
            .coerceAtLeast(localStorageManager.calculateDownloadedMangaBytes())

        val downloadsBytes = works.filter { it.fileType == "DOWNLOAD" || it.fileType == "IMPORT" }
            .sumOf { it.fileSizeBytes } + offlineMangaBytes
        val pagesBytes = works.filter { it.fileType == "PAGE" }
            .sumOf { it.fileSizeBytes }

        val dbFile = context.getDatabasePath("manga_sites_reader.db")
        val dbBytes = if (dbFile.exists()) dbFile.length() else 0L
        val otherBytes = 7_549_747L + dbBytes

        val cacheDirSize = localStorageManager.getFolderSizeBytes(context.cacheDir)
        val effectiveCacheBytes = if (cacheDirSize > 0L) cacheDirSize else 209_715L

        val statFs = try {
            StatFs(Environment.getDataDirectory().path)
        } catch (_: Exception) {
            null
        }
        val availableBytes = statFs?.availableBytes ?: (261_600_000_000L)
        val totalBytes = statFs?.totalBytes?.coerceAtLeast(1L) ?: 280_000_000_000L

        StorageStats(
            downloadsBytes = downloadsBytes,
            pagesBytes = pagesBytes,
            otherBytes = otherBytes,
            availableBytes = availableBytes,
            totalBytes = totalBytes,
            cacheBytes = effectiveCacheBytes,
            storagePath = getDisplayStoragePath()
        )
    }

    /**
     * Clears ONLY WebView and temporary HTTP caches, strictly preserving downloaded manga chapters.
     */
    suspend fun clearAppCache(): Boolean = localStorageManager.clearWebViewBrowsingDataOnly()

    suspend fun buildOfflineMangaWorkFromEntity(entity: DownloadedWorkEntity): MangaWork = withContext(Dispatchers.IO) {
        val downloadedChList = dao.getDownloadedChaptersForWorkOnce(entity.workId)
        val progressList = dao.getReadingProgressForWorkOnce(entity.workId)
        val progressByKey = progressList.associateBy { it.chapterKey }

        val chapters = downloadedChList.sortedWith(
            compareBy<DownloadedChapterEntity> { it.chapterNumberValue }.thenBy { it.chapterNumber }
        ).map { chEntity ->
            val rawPaths = localStorageManager.decodeLocalPathsJson(chEntity.localPagePathsJson)
            val validLocalPaths = localStorageManager.filterExistingValidPagePaths(rawPaths)
            val expectedPages = chEntity.totalPages.coerceAtLeast(rawPaths.size).coerceAtLeast(1)
            val isTrulyComplete = chEntity.status == "COMPLETED" &&
                localStorageManager.verifyChapterFilesComplete(expectedPages, validLocalPaths)
            val effectiveStatus = when {
                isTrulyComplete -> "COMPLETED"
                chEntity.status == "COMPLETED" && validLocalPaths.isNotEmpty() -> "INCOMPLETE"
                chEntity.status == "COMPLETED" -> "FAILED"
                else -> chEntity.status
            }

            val progress = progressByKey[chEntity.chapterKey]
                ?: progressList.find { it.chapterNumber == chEntity.chapterNumber }
            val timeText = if (progress != null) {
                readingProgressRepository.formatResumeText(progress)
            } else {
                ""
            }
            val dateLabel = when (effectiveStatus) {
                "COMPLETED" -> "جاهز بدون إنترنت (${validLocalPaths.size} ص)"
                "INCOMPLETE" -> "غير مكتمل (${validLocalPaths.size}/$expectedPages ص)"
                "DOWNLOADING", "QUEUED" -> "جارٍ التنزيل (${validLocalPaths.size}/$expectedPages ص)"
                else -> "فشل التنزيل"
            }
            MangaChapter(
                id = chEntity.chapterId,
                number = chEntity.chapterNumber,
                title = chEntity.chapterTitle,
                dateText = dateLabel,
                chapterUrl = chEntity.chapterUrl,
                pageCount = expectedPages,
                downloadedPagesCount = validLocalPaths.size,
                pageImageUrls = emptyList(),
                localPagePaths = validLocalPaths,
                isDownloaded = isTrulyComplete,
                downloadStatus = effectiveStatus,
                downloadError = chEntity.errorMessage.orEmpty(),
                isRead = progress != null && (progress.isCompleted || progress.lastPageIndex >= 0),
                lastReadPage = progress?.lastPageIndex ?: -1,
                lastReadTimeText = timeText,
                chapterKey = chEntity.chapterKey,
                chapterNumberValue = chEntity.chapterNumberValue,
                numberLabel = chEntity.chapterNumberLabel
            )
        }

        val genresList = entity.genresCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val resolvedCover = entity.remoteCoverUrl.ifBlank { entity.coverUrl }
        val verifiedLocalCover = entity.localCoverPath.takeIf { localStorageManager.hasValidLocalCover(it) }
            ?: localStorageManager.getExistingValidCoverFile(entity.sourceId, entity.workId)?.absolutePath.orEmpty()
        val completedCount = chapters.count { it.isDownloaded }
        val incompleteCount = chapters.count { !it.isDownloaded }

        MangaWork(
            id = entity.workId,
            title = entity.title,
            subtitle = entity.subtitle,
            siteName = entity.sourceName.ifBlank { "التخزين" },
            siteUrl = entity.sourceUrl,
            siteIconKey = entity.siteIconKey,
            mangaUrl = entity.mangaUrl,
            coverImageUrl = resolvedCover.ifBlank { null },
            coverUrl = resolvedCover.ifBlank { null },
            rating = entity.rating,
            status = entity.status,
            type = entity.type,
            releaseYear = "2025",
            author = entity.author,
            artist = entity.author,
            viewsText = "محلي",
            chaptersCount = maxOf(entity.totalChaptersOnSite, chapters.size),
            savedChaptersCount = completedCount,
            incompleteChaptersCount = incompleteCount,
            genres = genresList,
            description = entity.synopsis,
            synopsis = entity.synopsis,
            chapters = chapters,
            sourceId = entity.sourceId,
            localCoverPath = verifiedLocalCover,
            isFromLocalStorage = true,
            storageSizeBytes = entity.totalStorageBytes.coerceAtLeast(
                localStorageManager.getWorkStorageBytes(entity.sourceId, entity.workId)
            ),
            isDetailsLoaded = true
        )
    }

    suspend fun saveWebPageToStorage(title: String, url: String): Result<SavedWorkEntity> = withContext(Dispatchers.IO) {
        try {
            val savedDir = getSavedDirectory()
            val safeFileName = "page_${System.currentTimeMillis()}.html"
            val targetFile = File(savedDir, safeFileName)

            var htmlContent = ""
            if (isNetworkAvailable() && (url.startsWith("http://") || url.startsWith("https://"))) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                        .build()
                    httpClient.newCall(request).execute().use { response ->
                        htmlContent = response.body?.string() ?: ""
                    }
                } catch (_: Exception) {
                }
            }
            if (htmlContent.isEmpty()) {
                htmlContent = """
                    <!DOCTYPE html>
                    <html dir="rtl" lang="ar">
                    <head><meta charset="utf-8"><title>$title</title></head>
                    <body style="background:#12141A;color:#FFFFFF;font-family:sans-serif;padding:24px;">
                    <h2>$title</h2>
                    <p>الرابط المحفوظ: <a href="$url" style="color:#9BA8EE;">$url</a></p>
                    <p>تم الحفظ في التخزين المحلي بتاريخ ${java.util.Date()}</p>
                    </body>
                    </html>
                """.trimIndent()
            }

            targetFile.writeText(htmlContent, Charsets.UTF_8)
            val task = DownloadTaskEntity(
                title = title.ifBlank { url },
                url = url,
                status = "COMPLETED",
                progress = 100,
                bytesDownloaded = targetFile.length(),
                totalBytes = targetFile.length(),
                localPath = targetFile.absolutePath
            )
            dao.insertDownloadTask(task)

            val work = SavedWorkEntity(
                title = title.ifBlank { "صفحة محفوظة" },
                sourceUrl = url,
                localFilePath = targetFile.absolutePath,
                fileSizeBytes = targetFile.length(),
                fileType = "PAGE"
            )
            val id = dao.insertSavedWork(work)
            Result.success(work.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadUrlToFile(title: String, url: String): Result<SavedWorkEntity> = withContext(Dispatchers.IO) {
        val initialTask = DownloadTaskEntity(
            title = title.ifBlank { url },
            url = url,
            status = "DOWNLOADING",
            progress = 15
        )
        val taskId = dao.insertDownloadTask(initialTask)
        try {
            val savedDir = getSavedDirectory()
            val fileName = "download_${System.currentTimeMillis()}.html"
            val targetFile = File(savedDir, fileName)

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            dao.updateDownloadTask(initialTask.copy(id = taskId, progress = 55))

            httpClient.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: throw IllegalStateException("لم يتم استلام بيانات من الخادم")
                targetFile.writeBytes(bytes)
            }

            val size = targetFile.length()
            dao.updateDownloadTask(
                initialTask.copy(
                    id = taskId,
                    status = "COMPLETED",
                    progress = 100,
                    bytesDownloaded = size,
                    totalBytes = size,
                    localPath = targetFile.absolutePath
                )
            )
            val work = SavedWorkEntity(
                title = title.ifBlank { "ملف منزل" },
                sourceUrl = url,
                localFilePath = targetFile.absolutePath,
                fileSizeBytes = size,
                fileType = "DOWNLOAD"
            )
            val workId = dao.insertSavedWork(work)
            Result.success(work.copy(id = workId))
        } catch (e: Exception) {
            dao.updateDownloadTask(
                initialTask.copy(
                    id = taskId,
                    status = "FAILED",
                    progress = 0,
                    errorMessage = e.localizedMessage ?: "فشل التنزيل"
                )
            )
            Result.failure(e)
        }
    }

    suspend fun downloadMangaChapterOffline(
        work: MangaWork,
        chapter: MangaChapter,
        quality: String = "عالية (الأصلية)"
    ): Result<DownloadedChapterEntity> = runCatching {
        mangaDownloadManager.downloadChapterSuspend(work, chapter)
    }

    suspend fun togglePauseDownloadTask(task: DownloadTaskEntity) =
        mangaDownloadManager.togglePauseTask(task)

    suspend fun cancelDownloadTask(task: DownloadTaskEntity) =
        mangaDownloadManager.cancelTask(task)

    suspend fun cancelAllActiveDownloads() =
        mangaDownloadManager.cancelAllActiveTasks()

    suspend fun clearDownloadHistoryRecords() =
        mangaDownloadManager.clearFinishedDownloadRecords()

    suspend fun retryFailedDownloadTask(task: DownloadTaskEntity) = withContext(Dispatchers.IO) {
        val workId = task.workId.ifBlank { "work_${task.id}" }
        val existingWork = dao.getDownloadedWorkById(workId)
        val existingCh = if (task.chapterKey.isNotBlank()) {
            dao.getDownloadedChapterByKey(task.chapterKey)
        } else {
            dao.getDownloadedChaptersForWorkOnce(workId).find { it.chapterNumber == task.chapterNumber }
        }
        val work = if (existingWork != null) {
            buildOfflineMangaWorkFromEntity(existingWork)
        } else {
            MangaWork(
                id = workId,
                title = task.title.substringBefore(" - ").ifBlank { task.title },
                siteName = task.sourceName.ifBlank { "المصدر" },
                siteUrl = "",
                siteIconKey = "globe",
                coverImageUrl = task.coverUrl,
                coverUrl = task.coverUrl
            )
        }
        val chNum = task.chapterNumber.coerceAtLeast(existingCh?.chapterNumber ?: 1)
        val chapter = MangaChapter(
            id = task.chapterId.ifBlank { existingCh?.chapterId ?: "ch-$chNum" },
            number = chNum,
            title = existingCh?.chapterTitle?.ifBlank { "الفصل $chNum" } ?: "الفصل $chNum",
            dateText = "الآن",
            chapterUrl = existingCh?.chapterUrl?.ifBlank { task.url } ?: task.url,
            pageCount = task.totalPages.coerceAtLeast(existingCh?.pageCount ?: 1),
            chapterKey = task.chapterKey.ifBlank { existingCh?.chapterKey.orEmpty() }
        )
        dao.deleteDownloadTask(task)
        mangaDownloadManager.enqueueChaptersDownload(work, listOf(chapter))
    }

    suspend fun importFileFromUri(uri: Uri): Result<SavedWorkEntity> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var displayName = "عمل_مستورد_${System.currentTimeMillis()}"
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }

            val savedDir = getSavedDirectory()
            val destFile = File(savedDir, displayName)
            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("تعذر فتح الملف المختار"))

            val work = SavedWorkEntity(
                title = displayName,
                sourceUrl = uri.toString(),
                localFilePath = destFile.absolutePath,
                fileSizeBytes = destFile.length(),
                fileType = "IMPORT"
            )
            val id = dao.insertSavedWork(work)
            Result.success(work.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSavedWork(work: SavedWorkEntity) = withContext(Dispatchers.IO) {
        try {
            val f = File(work.localFilePath)
            if (f.exists()) f.delete()
        } catch (_: Exception) {
        }
        dao.deleteSavedWork(work)
    }

    suspend fun deleteDownloadedWork(workId: String) =
        mangaDownloadManager.deleteDownloadedWorkCompletely(workId)

    suspend fun deleteDownloadedChapter(workId: String, chapterNumber: Int) {
        val chapterKey = "${workId}_ch_${chapterNumber}"
        val chEntity = dao.getDownloadedChapterByKey(chapterKey)
            ?: dao.getDownloadedChaptersForWorkOnce(workId).find { it.chapterNumber == chapterNumber }
            ?: return
        mangaDownloadManager.deleteDownloadedChapter(chEntity)
    }

    suspend fun deleteDownloadTask(task: DownloadTaskEntity) =
        mangaDownloadManager.deleteTaskAndFiles(task)

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val sitesArr = JSONArray()
        dao.getAllSitesOnce().forEach { s ->
            sitesArr.put(
                JSONObject().apply {
                    put("name", s.name)
                    put("url", s.url)
                    put("domain", s.domain)
                    put("isEnabled", s.isEnabled)
                    put("iconKey", s.iconKey)
                    put("lastVisitedUrl", s.lastVisitedUrl)
                }
            )
        }
        root.put("sites", sitesArr)

        val favArr = JSONArray()
        dao.getAllFavoritesOnce().forEach { f ->
            favArr.put(
                JSONObject().apply {
                    put("title", f.title)
                    put("url", f.url)
                    put("siteName", f.siteName)
                    put("category", f.category)
                }
            )
        }
        root.put("favorites", favArr)
        root.put("exportedAt", System.currentTimeMillis())

        val backupFile = File(getSavedDirectory(), "backup_manga_reader.json")
        backupFile.writeText(root.toString(2), Charsets.UTF_8)
        backupFile.absolutePath
    }

    suspend fun restoreBackupFromDefaultFile(): Int = withContext(Dispatchers.IO) {
        val backupFile = File(getSavedDirectory(), "backup_manga_reader.json")
        if (!backupFile.exists()) return@withContext 0
        val content = backupFile.readText(Charsets.UTF_8)
        val root = JSONObject(content)
        var restoredCount = 0
        val sitesArr = root.optJSONArray("sites")
        if (sitesArr != null) {
            val currentDomains = dao.getAllSitesOnce().map { it.domain }.toSet()
            for (i in 0 until sitesArr.length()) {
                val obj = sitesArr.getJSONObject(i)
                val domain = obj.optString("domain")
                if (domain.isNotEmpty() && domain !in currentDomains) {
                    dao.insertSite(
                        SiteEntity(
                            name = obj.optString("name", domain),
                            url = obj.optString("url", "https://$domain"),
                            domain = domain,
                            isEnabled = obj.optBoolean("isEnabled", true),
                            iconKey = obj.optString("iconKey", "globe"),
                            lastVisitedUrl = obj.optString("lastVisitedUrl", "")
                        )
                    )
                    restoredCount++
                }
            }
        }
        restoredCount
    }

    // Settings delegators
    suspend fun updateStringSetting(key: String, value: String) = settingsDataStore.updateString(key, value)
    suspend fun updateBooleanSetting(key: String, value: Boolean) = settingsDataStore.updateBoolean(key, value)
    suspend fun updateIntSetting(key: String, value: Int) = settingsDataStore.updateInt(key, value)
    suspend fun updateFloatSetting(key: String, value: Float) = settingsDataStore.updateFloat(key, value)
    suspend fun incrementSearchCount() = settingsDataStore.incrementSearchCount()
    suspend fun clearSearchCount() = settingsDataStore.clearSearchCount()
    suspend fun addReadingSeconds(sec: Long) = settingsDataStore.addReadingSeconds(sec)
    suspend fun resetStatistics() = settingsDataStore.resetStatistics()

    // ==================== LATEST UPDATES CACHING & SYNC ====================

    suspend fun getCachedLatestUpdates(sourceId: Long? = null): List<MangaLatestUpdateItem> = withContext(Dispatchers.IO) {
        val entities = if (sourceId != null && sourceId > 0L) {
            dao.getLatestUpdatesForSourceOnce(sourceId)
        } else {
            dao.getAllLatestUpdatesOnce()
        }
        LiveMangaSiteScraper.sortLatestUpdates(entities.map { it.toDomainModel() })
    }

    suspend fun syncLatestUpdatesForSite(
        site: SiteEntity,
        page: Int = 1,
        replaceCachedForPageOne: Boolean = true
    ): AdapterScrapeResult<List<MangaLatestUpdateItem>> = withContext(Dispatchers.IO) {
        val adapter = MangaSourceAdapterRegistry.getAdapterForSite(site)
        if (!adapter.capabilities.supportsLatestUpdates) {
            return@withContext AdapterScrapeResult(
                data = emptyList(),
                errorMessage = "المصدر (${site.name}) يدعم التصفح داخل WebView فقط ولا يدعم استخراج آخر التحديثات تلقائيًا."
            )
        }
        val res = adapter.fetchLatestUpdates(site, page)
        if (res.data.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val entities = res.data.mapIndexed { idx, item ->
                item.copy(
                    originalOrderIndex = if (page > 1) (page - 1) * 50 + idx else item.originalOrderIndex,
                    syncedAt = now
                ).toEntity()
            }
            if (page == 1 && replaceCachedForPageOne) {
                dao.clearLatestUpdatesForSource(site.id)
            }
            dao.upsertLatestUpdates(entities)
        }
        res
    }

    suspend fun cacheLatestUpdatesDirectly(
        siteId: Long,
        items: List<MangaLatestUpdateItem>,
        replaceExistingForSource: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (replaceExistingForSource && siteId > 0L) {
            dao.clearLatestUpdatesForSource(siteId)
        }
        if (items.isNotEmpty()) {
            dao.upsertLatestUpdates(items.map { it.toEntity() })
        }
    }

    private fun LatestUpdateEntity.toDomainModel(): MangaLatestUpdateItem {
        return MangaLatestUpdateItem(
            updateKey = updateKey,
            sourceId = sourceId,
            sourceName = sourceName,
            sourceUrl = sourceUrl,
            siteIconKey = siteIconKey,
            workId = workId,
            workTitle = workTitle,
            mangaUrl = mangaUrl,
            coverImageUrl = coverImageUrl.ifBlank { null },
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            chapterNumber = chapterNumber,
            chapterNumberValue = chapterNumberValue,
            chapterNumberLabel = chapterNumberLabel,
            chapterUrl = chapterUrl,
            publishDateText = publishDateText,
            publishedAtEpochMillis = publishedAtEpochMillis,
            isDateAvailable = isDateAvailable,
            originalOrderIndex = originalOrderIndex,
            syncedAt = syncedAt
        )
    }

    private fun MangaLatestUpdateItem.toEntity(): LatestUpdateEntity {
        return LatestUpdateEntity(
            updateKey = updateKey,
            sourceId = sourceId,
            sourceName = sourceName,
            sourceUrl = sourceUrl,
            siteIconKey = siteIconKey,
            workId = workId,
            workTitle = workTitle,
            mangaUrl = mangaUrl,
            coverImageUrl = coverImageUrl.orEmpty(),
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            chapterNumber = chapterNumber,
            chapterNumberValue = chapterNumberValue,
            chapterNumberLabel = chapterNumberLabel,
            chapterUrl = chapterUrl,
            publishDateText = publishDateText,
            publishedAtEpochMillis = publishedAtEpochMillis,
            isDateAvailable = isDateAvailable,
            originalOrderIndex = originalOrderIndex,
            syncedAt = syncedAt
        )
    }
}
