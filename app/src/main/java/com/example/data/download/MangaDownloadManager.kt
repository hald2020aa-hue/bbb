package com.example.data.download

import com.example.data.DiagnosticCategory
import com.example.data.DiagnosticLogger
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaChapter
import com.example.data.MangaWork
import com.example.data.local.AppDao
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.DownloadedChapterEntity
import com.example.data.local.DownloadedWorkEntity
import com.example.data.local.SiteEntity
import com.example.data.source.MangaSourceAdapterRegistry
import com.example.data.storage.LocalStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Real chapter & image DownloadManager for Android.
 * Downloads ordered chapter page images to app-private storage via LocalStorageManager,
 * reuses already-downloaded valid pages on retry, updates real Android notifications via
 * DownloadNotificationHelper, tracks per-page progress and ETA, verifies file completeness
 * before marking COMPLETED, and marks partial downloads as INCOMPLETE so missing pages are never hidden.
 */
class MangaDownloadManager(
    private val dao: AppDao,
    private val storageManager: LocalStorageManager,
    private val notificationHelper: DownloadNotificationHelper? = null,
    private val httpClient: OkHttpClient = LiveMangaSiteScraper.httpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Bounded concurrency so multiple chapter downloads queue cleanly without overwhelming the device/site
    private val chapterQueueSemaphore = Semaphore(2)

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val activeChapterKeys = ConcurrentHashMap.newKeySet<String>()
    private val cancelledTaskIds = ConcurrentHashMap.newKeySet<Long>()
    private val pausedTaskIds = ConcurrentHashMap.newKeySet<Long>()

    private val _isAllPaused = MutableStateFlow(false)
    val isAllPaused: StateFlow<Boolean> = _isAllPaused.asStateFlow()

    @Volatile
    var compressDownloadsWebp: Boolean = false

    fun isChapterActivelyDownloading(chapterKey: String): Boolean =
        activeChapterKeys.contains(chapterKey)

    fun enqueueChaptersDownload(
        work: MangaWork,
        chapters: List<MangaChapter>,
        site: SiteEntity? = null,
        quality: String = "عالية (الأصلية)"
    ) {
        if (chapters.isEmpty()) return
        scope.launch {
            val localCoverPath = ensureLocalWorkCover(work)
            val existingWork = dao.getDownloadedWorkById(work.id)
            val totalSiteChapters = maxOf(
                work.chaptersCount,
                work.chapters.size,
                existingWork?.totalChaptersOnSite ?: 0,
                chapters.size
            )
            val effectiveCoverUrl = (work.effectiveCoverUrl ?: existingWork?.remoteCoverUrl).orEmpty()

            val workEntity = DownloadedWorkEntity(
                workId = work.id,
                sourceId = work.sourceId,
                sourceName = work.siteName.ifBlank { site?.name ?: "المصدر" },
                sourceUrl = work.siteUrl.ifBlank { site?.url ?: "" },
                siteIconKey = work.siteIconKey.ifBlank { site?.iconKey ?: "globe" },
                title = work.title,
                subtitle = work.subtitle,
                mangaUrl = work.mangaUrl.ifBlank { "${work.siteUrl}/series/${work.id}" },
                remoteCoverUrl = effectiveCoverUrl,
                coverUrl = effectiveCoverUrl,
                localCoverPath = localCoverPath.ifBlank { existingWork?.localCoverPath.orEmpty() },
                synopsis = work.description.ifBlank { work.synopsis.ifBlank { existingWork?.synopsis.orEmpty() } },
                genresCsv = if (work.genres.isNotEmpty()) work.genres.joinToString(",") else existingWork?.genresCsv.orEmpty(),
                statusText = work.status.ifBlank { existingWork?.statusText ?: "OnGoing" },
                status = work.status.ifBlank { existingWork?.status ?: "OnGoing" },
                type = work.type.ifBlank { existingWork?.type ?: "مانهوا" },
                author = work.author.ifBlank { existingWork?.author.orEmpty() },
                rating = work.rating.ifBlank { existingWork?.rating ?: "4.5" },
                totalChaptersOnSite = totalSiteChapters,
                savedChaptersCount = existingWork?.savedChaptersCount ?: 0,
                totalStorageBytes = existingWork?.totalStorageBytes ?: 0L,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertDownloadedWork(workEntity)

            for (chapter in chapters) {
                val chapterKey = chapter.chapterKey.ifBlank { "${work.id}_ch_${chapter.number}" }

                // Prevent duplicate simultaneous downloads for the same chapter
                if (!activeChapterKeys.add(chapterKey)) {
                    DiagnosticLogger.logInfo(
                        category = DiagnosticCategory.DOWNLOAD,
                        message = "تم تخطي إضافة الفصل ${chapter.number} لأنه قيد التنزيل بالفعل",
                        workId = work.id,
                        chapterId = chapter.id
                    )
                    continue
                }

                // Check if already completely downloaded and valid on disk
                val existingChapter = dao.getDownloadedChapterByKey(chapterKey)
                if (existingChapter != null && existingChapter.status == STATUS_COMPLETED) {
                    val existingPaths = storageManager.decodeLocalPathsJson(existingChapter.localPagesJson)
                    if (storageManager.verifyChapterFilesComplete(existingChapter.pageCount, existingPaths)) {
                        activeChapterKeys.remove(chapterKey)
                        DiagnosticLogger.logInfo(
                            category = DiagnosticCategory.DOWNLOAD,
                            message = "الفصل ${chapter.number} محفوظ ومكتمل مسبقًا (${existingPaths.size} صفحة)",
                            workId = work.id,
                            chapterId = chapter.id
                        )
                        continue
                    }
                }

                val expectedCount = chapter.pageImageUrls.size.coerceAtLeast(chapter.pageCount).coerceAtLeast(1)
                val previouslySavedPaths = existingChapter?.let {
                    storageManager.filterExistingValidPagePaths(storageManager.decodeLocalPathsJson(it.localPagesJson))
                }.orEmpty()

                val initialChapterEntity = DownloadedChapterEntity(
                    chapterKey = chapterKey,
                    workId = work.id,
                    sourceId = work.sourceId,
                    sourceName = work.siteName,
                    workTitle = work.title,
                    chapterId = chapter.id,
                    chapterNumber = chapter.number,
                    chapterTitle = chapter.title,
                    chapterUrl = chapter.chapterUrl,
                    dateText = chapter.dateText,
                    pageCount = expectedCount,
                    downloadedPagesCount = previouslySavedPaths.size,
                    localPagesJson = storageManager.encodeLocalPathsJson(previouslySavedPaths),
                    totalBytes = existingChapter?.totalBytes ?: 0L,
                    status = if (_isAllPaused.value) STATUS_PAUSED else STATUS_QUEUED,
                    progressPercent = if (expectedCount > 0) ((previouslySavedPaths.size * 100f) / expectedCount) else 0f,
                    etaSeconds = 18
                )
                dao.upsertDownloadedChapter(initialChapterEntity)

                val coverForTask = localCoverPath.ifBlank { effectiveCoverUrl }
                val taskEntity = DownloadTaskEntity(
                    title = work.title,
                    url = chapter.chapterUrl.ifBlank { "manga://${work.id}/chapter/${chapter.number}" },
                    status = if (_isAllPaused.value) STATUS_PAUSED else STATUS_QUEUED,
                    progress = initialChapterEntity.progressPercent.toInt(),
                    bytesDownloaded = existingChapter?.totalBytes ?: 0L,
                    totalBytes = existingChapter?.totalBytes ?: 0L,
                    localPath = previouslySavedPaths.firstOrNull().orEmpty(),
                    workId = work.id,
                    chapterKey = chapterKey,
                    chapterId = chapter.id,
                    chapterNumber = chapter.number,
                    sourceName = work.siteName,
                    coverPath = coverForTask,
                    coverUrl = coverForTask,
                    downloadedPages = previouslySavedPaths.size,
                    totalPages = expectedCount,
                    etaSeconds = 18,
                    chaptersCount = 1
                )
                val taskId = dao.insertDownloadTask(taskEntity)
                val job = scope.launch {
                    try {
                        chapterQueueSemaphore.withPermit {
                            executeChapterDownloadJob(
                                taskId = taskId,
                                work = work,
                                chapter = chapter,
                                chapterKey = chapterKey,
                                localCoverPath = coverForTask,
                                site = site
                            )
                        }
                    } finally {
                        activeChapterKeys.remove(chapterKey)
                    }
                }
                activeJobs[taskId] = job
            }
        }
    }

    /**
     * Synchronous / suspendable chapter download entry point so both background UI flows and
     * automated tests can await completion deterministically.
     */
    suspend fun downloadChapterSuspend(
        work: MangaWork,
        chapter: MangaChapter,
        site: SiteEntity? = null
    ): DownloadedChapterEntity = withContext(Dispatchers.IO) {
        val localCoverPath = ensureLocalWorkCover(work)
        val existingWork = dao.getDownloadedWorkById(work.id)
        val effectiveCoverUrl = (work.effectiveCoverUrl ?: existingWork?.remoteCoverUrl).orEmpty()

        val workEntity = DownloadedWorkEntity(
            workId = work.id,
            sourceId = work.sourceId,
            sourceName = work.siteName.ifBlank { site?.name ?: "المصدر" },
            sourceUrl = work.siteUrl.ifBlank { site?.url ?: "" },
            siteIconKey = work.siteIconKey.ifBlank { site?.iconKey ?: "globe" },
            title = work.title,
            subtitle = work.subtitle,
            mangaUrl = work.mangaUrl.ifBlank { "${work.siteUrl}/series/${work.id}" },
            remoteCoverUrl = effectiveCoverUrl,
            coverUrl = effectiveCoverUrl,
            localCoverPath = localCoverPath.ifBlank { existingWork?.localCoverPath.orEmpty() },
            synopsis = work.description.ifBlank { work.synopsis.ifBlank { existingWork?.synopsis.orEmpty() } },
            genresCsv = if (work.genres.isNotEmpty()) work.genres.joinToString(",") else existingWork?.genresCsv.orEmpty(),
            statusText = work.status.ifBlank { existingWork?.statusText ?: "OnGoing" },
            status = work.status.ifBlank { existingWork?.status ?: "OnGoing" },
            type = work.type.ifBlank { existingWork?.type ?: "مانهوا" },
            author = work.author.ifBlank { existingWork?.author.orEmpty() },
            rating = work.rating.ifBlank { existingWork?.rating ?: "4.5" },
            totalChaptersOnSite = maxOf(work.chaptersCount, work.chapters.size, 1),
            savedChaptersCount = existingWork?.savedChaptersCount ?: 0,
            totalStorageBytes = existingWork?.totalStorageBytes ?: 0L,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertDownloadedWork(workEntity)

        val chapterKey = chapter.chapterKey.ifBlank { "${work.id}_ch_${chapter.number}" }
        val expectedCount = chapter.pageImageUrls.size.coerceAtLeast(chapter.pageCount).coerceAtLeast(1)
        val coverForTask = localCoverPath.ifBlank { effectiveCoverUrl }

        val taskEntity = DownloadTaskEntity(
            title = work.title,
            url = chapter.chapterUrl.ifBlank { "manga://${work.id}/chapter/${chapter.number}" },
            status = STATUS_DOWNLOADING,
            progress = 0,
            workId = work.id,
            chapterKey = chapterKey,
            chapterId = chapter.id,
            chapterNumber = chapter.number,
            sourceName = work.siteName,
            coverPath = coverForTask,
            coverUrl = coverForTask,
            downloadedPages = 0,
            totalPages = expectedCount,
            etaSeconds = 12,
            chaptersCount = 1
        )
        val taskId = dao.insertDownloadTask(taskEntity)
        activeChapterKeys.add(chapterKey)
        try {
            executeChapterDownloadJob(
                taskId = taskId,
                work = work,
                chapter = chapter,
                chapterKey = chapterKey,
                localCoverPath = coverForTask,
                site = site
            )
        } finally {
            activeChapterKeys.remove(chapterKey)
        }
        dao.getDownloadedChapterByKey(chapterKey) ?: error("Chapter record missing after download")
    }

    private suspend fun executeChapterDownloadJob(
        taskId: Long,
        work: MangaWork,
        chapter: MangaChapter,
        chapterKey: String,
        localCoverPath: String,
        site: SiteEntity?
    ) {
        val taskKeyStr = "${work.id}_${chapter.number}"
        try {
            while (_isAllPaused.value || pausedTaskIds.contains(taskId)) {
                if (cancelledTaskIds.contains(taskId)) break
                delay(250)
            }
            if (cancelledTaskIds.contains(taskId)) {
                markCancelled(taskId, taskKeyStr, work, chapter, chapterKey, localCoverPath)
                return
            }

            notificationHelper?.showDownloadStarting(
                taskId = taskKeyStr,
                workId = work.id,
                workTitle = work.title,
                chapterNumber = chapter.number
            )

            DiagnosticLogger.logInfo(
                category = DiagnosticCategory.DOWNLOAD,
                message = "بدء تنزيل الفصل ${chapter.number} (${work.title})",
                sourceId = work.sourceId.toString(),
                sourceName = work.siteName,
                workId = work.id,
                chapterId = chapter.id,
                url = chapter.chapterUrl
            )

            // 1. Resolve chapter page URLs strictly (never fake offline URLs when chapterUrl is http/https!)
            val cachedChapterPages = LiveMangaSiteScraper.getCachedChapterPages(chapter.chapterUrl)
            val resolvedPageUrls: List<String> = when {
                chapter.pageImageUrls.isNotEmpty() -> {
                    LinkedHashSet(chapter.pageImageUrls.filter { it.isNotBlank() }).toList()
                }
                cachedChapterPages.isNotEmpty() -> {
                    LinkedHashSet(cachedChapterPages.filter { it.isNotBlank() }).toList()
                }
                chapter.chapterUrl.startsWith("http://") || chapter.chapterUrl.startsWith("https://") -> {
                    val adapter = if (site != null) {
                        MangaSourceAdapterRegistry.getAdapterForSite(site)
                    } else {
                        MangaSourceAdapterRegistry.getAdapterForWork(work, emptyList())
                    }
                    if (!adapter.capabilities.supportsOfflineDownload) {
                        markFailed(
                            taskId = taskId,
                            taskKeyStr = taskKeyStr,
                            work = work,
                            chapter = chapter,
                            chapterKey = chapterKey,
                            localCoverPath = localCoverPath,
                            errorMsg = "هذا المصدر لا يدعم تنزيل الفصول محليًا"
                        )
                        return
                    }
                    val pageResult = adapter.fetchChapterPageUrls(
                        site = site ?: SiteEntity(id = work.sourceId, name = work.siteName, url = work.siteUrl, domain = ""),
                        work = work,
                        chapter = chapter
                    )
                    val combinedPages = if (pageResult.data.isNotEmpty()) {
                        pageResult.data
                    } else {
                        // Fallback to LiveMangaSiteScraper (includes Headless WebView DOM extraction for JS-rendered sites)
                        LiveMangaSiteScraper.fetchChapterPages(work, chapter).pageUrls
                    }
                    if (combinedPages.isEmpty()) {
                        val reason = pageResult.errorMessage ?: "تعذر استخراج روابط صور الفصل من صفحة المصدر"
                        DiagnosticLogger.logError(
                            category = DiagnosticCategory.DOWNLOAD,
                            message = "فشل استخراج روابط صور الفصل ${chapter.number}: $reason",
                            sourceId = work.sourceId.toString(),
                            sourceName = work.siteName,
                            workId = work.id,
                            chapterId = chapter.id,
                            url = chapter.chapterUrl
                        )
                        markFailed(
                            taskId = taskId,
                            taskKeyStr = taskKeyStr,
                            work = work,
                            chapter = chapter,
                            chapterKey = chapterKey,
                            localCoverPath = localCoverPath,
                            errorMsg = reason
                        )
                        return
                    }
                    val extracted = LinkedHashSet(
                        combinedPages.filter { it.isNotBlank() && !LiveMangaSiteScraper.isIgnoredAssetImage(it) }
                    ).toList()
                    LiveMangaSiteScraper.cacheChapterPages(chapter.chapterUrl, extracted)
                    extracted
                }
                chapter.chapterUrl.startsWith("offline://") || chapter.chapterUrl.startsWith("test://") -> {
                    val fallbackCount = chapter.pageCount.coerceAtLeast(3)
                    (1..fallbackCount).map { idx -> "offline://${work.id}/${chapter.number}/page_$idx.jpg" }
                }
                else -> emptyList()
            }

            if (resolvedPageUrls.isEmpty()) {
                markFailed(
                    taskId = taskId,
                    taskKeyStr = taskKeyStr,
                    work = work,
                    chapter = chapter,
                    chapterKey = chapterKey,
                    localCoverPath = localCoverPath,
                    errorMsg = "لم يتم العثور على صور قابلة للتنزيل في هذا الفصل"
                )
                return
            }

            var currentResolvedUrls = resolvedPageUrls
            val expectedPages = currentResolvedUrls.size
            val savedPathsByIndex = java.util.TreeMap<Int, String>()
            val failedPageNumbers = mutableListOf<Int>()
            var cumulativeBytes = 0L

            notificationHelper?.showDownloadProgress(
                taskId = taskKeyStr,
                workId = work.id,
                workTitle = work.title,
                chapterNumber = chapter.number,
                downloadedPages = 0,
                totalPages = expectedPages
            )

            val maxPartialRetryPasses = 3
            for (pass in 1..maxPartialRetryPasses) {
                if (pass > 1) {
                    // Automatic retry on partial download failure (إعادة المحاولة التلقائية عند فشل التحميل الجزئي)
                    DiagnosticLogger.logInfo(
                        category = DiagnosticCategory.DOWNLOAD,
                        message = "إعادة محاولة التحميل الجزئي التلقائية (الجولة $pass/$maxPartialRetryPasses) للصفحات المتبقية: ${failedPageNumbers.joinToString("، ")}",
                        workId = work.id,
                        chapterId = chapter.id,
                        url = chapter.chapterUrl
                    )
                    delay(450L * (pass - 1))
                    if (chapter.chapterUrl.startsWith("http://") || chapter.chapterUrl.startsWith("https://")) {
                        val refreshed = LiveMangaSiteScraper.fetchChapterPages(
                            work = work,
                            chapter = chapter,
                            forceRefresh = true
                        ).pageUrls
                        if (refreshed.size == expectedPages) {
                            currentResolvedUrls = refreshed
                        }
                    }
                }
                failedPageNumbers.clear()

                for ((index, pageUrl) in currentResolvedUrls.withIndex()) {
                    while (_isAllPaused.value || pausedTaskIds.contains(taskId)) {
                        if (cancelledTaskIds.contains(taskId)) break
                        delay(250)
                    }
                    if (cancelledTaskIds.contains(taskId)) {
                        storageManager.deleteChapterFiles(work.sourceId, work.id, chapterKey, savedPathsByIndex.values.toList())
                        markCancelled(taskId, taskKeyStr, work, chapter, chapterKey, localCoverPath)
                        return
                    }

                    // Skip pages already downloaded and verified in an earlier pass or previous attempt
                    if (savedPathsByIndex.containsKey(index)) {
                        continue
                    }

                    val existingValidFile = storageManager.getExistingValidPageFile(
                        sourceId = work.sourceId,
                        workId = work.id,
                        chapterKey = chapterKey,
                        pageIndex = index
                    )
                    if (existingValidFile != null) {
                        savedPathsByIndex[index] = existingValidFile.absolutePath
                        cumulativeBytes += existingValidFile.length()
                        notificationHelper?.showDownloadProgress(
                            taskId = taskKeyStr,
                            workId = work.id,
                            workTitle = work.title,
                            chapterNumber = chapter.number,
                            downloadedPages = savedPathsByIndex.size,
                            totalPages = expectedPages
                        )
                        continue
                    }

                    val pageNum = index + 1
                    val remainingPages = (expectedPages - savedPathsByIndex.size).coerceAtLeast(1)
                    val etaSec = (remainingPages * 2).coerceAtLeast(1)
                    val progressFloat = ((savedPathsByIndex.size.toFloat() / expectedPages.toFloat()) * 100f).coerceIn(0f, 99f)

                    updateProgressInDb(
                        taskId = taskId,
                        work = work,
                        chapter = chapter,
                        chapterKey = chapterKey,
                        localCoverPath = localCoverPath,
                        expectedPages = expectedPages,
                        downloadedCount = savedPathsByIndex.size,
                        localPaths = savedPathsByIndex.values.toList(),
                        bytesSoFar = cumulativeBytes,
                        progressFloat = progressFloat,
                        etaSeconds = etaSec
                    )

                    val pageBytes = downloadSingleImageBytesWithRetry(
                        imageUrl = pageUrl,
                        refererUrl = chapter.chapterUrl.ifBlank { work.mangaUrl.ifBlank { work.siteUrl } },
                        siteUrl = work.siteUrl,
                        workId = work.id,
                        chapterId = chapter.id,
                        workTitle = work.title,
                        chapterTitle = chapter.title,
                        pageNumber = pageNum
                    )

                    if (pageBytes != null && storageManager.isValidChapterPageImageBytes(pageBytes)) {
                        val ext = guessImageExtension(pageUrl, pageBytes)
                        val savedFile = storageManager.saveChapterPageImage(
                            sourceId = work.sourceId,
                            workId = work.id,
                            chapterKey = chapterKey,
                            pageIndex = index,
                            bytes = pageBytes,
                            extension = ext,
                            compressToWebp = compressDownloadsWebp
                        )
                        if (storageManager.isValidImageFile(savedFile)) {
                            savedPathsByIndex[index] = savedFile.absolutePath
                            cumulativeBytes += savedFile.length()
                            notificationHelper?.showDownloadProgress(
                                taskId = taskKeyStr,
                                workId = work.id,
                                workTitle = work.title,
                                chapterNumber = chapter.number,
                                downloadedPages = savedPathsByIndex.size,
                                totalPages = expectedPages
                            )
                        } else {
                            failedPageNumbers.add(pageNum)
                            DiagnosticLogger.logError(
                                category = DiagnosticCategory.STORAGE,
                                message = "فشل التحقق من ملف الصفحة $pageNum بعد الحفظ",
                                workId = work.id,
                                chapterId = chapter.id,
                                pageNumber = pageNum,
                                url = pageUrl
                            )
                        }
                    } else {
                        failedPageNumbers.add(pageNum)
                    }
                }

                if (failedPageNumbers.isEmpty()) {
                    break
                }
            }

            val savedLocalPaths = savedPathsByIndex.values.toList()

            // Strictly verify every single required page exists on disk before marking COMPLETED
            val isComplete = failedPageNumbers.isEmpty() &&
                storageManager.verifyChapterFilesComplete(expectedPages, savedLocalPaths)

            val finalStatus = when {
                isComplete -> STATUS_COMPLETED
                savedLocalPaths.isNotEmpty() -> STATUS_INCOMPLETE
                else -> STATUS_FAILED
            }
            val finalProgress = if (isComplete) 100f else ((savedLocalPaths.size.toFloat() / expectedPages) * 100f)
            val failedPagesSummary = if (failedPageNumbers.isNotEmpty()) {
                "فشل تحميل الصفحات: ${failedPageNumbers.take(8).joinToString("، ")}" +
                    if (failedPageNumbers.size > 8) " وغيرها" else ""
            } else ""
            val finalError = when (finalStatus) {
                STATUS_INCOMPLETE -> "فصل غير مكتمل (${savedLocalPaths.size} من $expectedPages صفحة) — $failedPagesSummary"
                STATUS_FAILED -> if (failedPagesSummary.isNotBlank()) "تعذر تنزيل صور الفصل ($failedPagesSummary)" else "تعذر تنزيل صور الفصل"
                else -> null
            }

            val finalChapterEntity = DownloadedChapterEntity(
                chapterKey = chapterKey,
                workId = work.id,
                sourceId = work.sourceId,
                sourceName = work.siteName,
                workTitle = work.title,
                chapterId = chapter.id,
                chapterNumber = chapter.number,
                chapterNumberValue = chapter.chapterNumberValue,
                chapterNumberLabel = chapter.displayNumber,
                chapterTitle = chapter.title,
                chapterUrl = chapter.chapterUrl,
                dateText = chapter.dateText,
                pageCount = expectedPages,
                downloadedPagesCount = savedLocalPaths.size,
                localPagesJson = storageManager.encodeLocalPathsJson(savedLocalPaths),
                totalBytes = cumulativeBytes,
                status = finalStatus,
                progressPercent = finalProgress,
                etaSeconds = 0,
                errorMessage = finalError,
                downloadedAt = System.currentTimeMillis()
            )
            dao.upsertDownloadedChapter(finalChapterEntity)

            dao.updateDownloadTask(
                DownloadTaskEntity(
                    id = taskId,
                    title = work.title,
                    url = chapter.chapterUrl.ifBlank { "manga://${work.id}/chapter/${chapter.number}" },
                    status = finalStatus,
                    progress = finalProgress.toInt(),
                    bytesDownloaded = cumulativeBytes,
                    totalBytes = cumulativeBytes,
                    localPath = savedLocalPaths.firstOrNull().orEmpty(),
                    errorMessage = finalError,
                    workId = work.id,
                    chapterKey = chapterKey,
                    chapterId = chapter.id,
                    chapterNumber = chapter.number,
                    sourceName = work.siteName,
                    coverPath = localCoverPath,
                    coverUrl = localCoverPath,
                    downloadedPages = savedLocalPaths.size,
                    totalPages = expectedPages,
                    etaSeconds = 0,
                    chaptersCount = 1
                )
            )

            refreshWorkStorageSummary(work.id, work.sourceId)

            when (finalStatus) {
                STATUS_COMPLETED -> {
                    DiagnosticLogger.logInfo(
                        category = DiagnosticCategory.DOWNLOAD,
                        message = "اكتمل تنزيل الفصل ${chapter.number} بنجاح ($expectedPages صفحة)",
                        workId = work.id,
                        chapterId = chapter.id
                    )
                    notificationHelper?.showDownloadCompleted(
                        taskId = taskKeyStr,
                        workId = work.id,
                        workTitle = work.title,
                        chapterNumber = chapter.number,
                        totalPages = expectedPages
                    )
                }
                STATUS_INCOMPLETE -> {
                    DiagnosticLogger.logWarn(
                        category = DiagnosticCategory.DOWNLOAD,
                        message = finalError ?: "فصل غير مكتمل",
                        workId = work.id,
                        chapterId = chapter.id
                    )
                    notificationHelper?.showDownloadFailed(
                        taskId = taskKeyStr,
                        workId = work.id,
                        workTitle = work.title,
                        chapterNumber = chapter.number,
                        reason = finalError ?: "فصل غير مكتمل",
                        isIncomplete = true
                    )
                }
                else -> {
                    DiagnosticLogger.logError(
                        category = DiagnosticCategory.DOWNLOAD,
                        message = finalError ?: "فشل تنزيل الفصل",
                        workId = work.id,
                        chapterId = chapter.id
                    )
                    notificationHelper?.showDownloadFailed(
                        taskId = taskKeyStr,
                        workId = work.id,
                        workTitle = work.title,
                        chapterNumber = chapter.number,
                        reason = finalError ?: "تعذر تنزيل الفصل",
                        isIncomplete = false
                    )
                }
            }
        } catch (e: Exception) {
            markFailed(
                taskId = taskId,
                taskKeyStr = taskKeyStr,
                work = work,
                chapter = chapter,
                chapterKey = chapterKey,
                localCoverPath = localCoverPath,
                errorMsg = e.localizedMessage ?: "خطأ أثناء التنزيل"
            )
        } finally {
            activeJobs.remove(taskId)
        }
    }

    private suspend fun updateProgressInDb(
        taskId: Long,
        work: MangaWork,
        chapter: MangaChapter,
        chapterKey: String,
        localCoverPath: String,
        expectedPages: Int,
        downloadedCount: Int,
        localPaths: List<String>,
        bytesSoFar: Long,
        progressFloat: Float,
        etaSeconds: Int
    ) {
        dao.upsertDownloadedChapter(
            DownloadedChapterEntity(
                chapterKey = chapterKey,
                workId = work.id,
                sourceId = work.sourceId,
                sourceName = work.siteName,
                workTitle = work.title,
                chapterId = chapter.id,
                chapterNumber = chapter.number,
                chapterNumberValue = chapter.chapterNumberValue,
                chapterNumberLabel = chapter.displayNumber,
                chapterTitle = chapter.title,
                chapterUrl = chapter.chapterUrl,
                dateText = chapter.dateText,
                pageCount = expectedPages,
                downloadedPagesCount = downloadedCount,
                localPagesJson = storageManager.encodeLocalPathsJson(localPaths),
                totalBytes = bytesSoFar,
                status = STATUS_DOWNLOADING,
                progressPercent = progressFloat,
                etaSeconds = etaSeconds
            )
        )
        dao.updateDownloadTask(
            DownloadTaskEntity(
                id = taskId,
                title = work.title,
                url = chapter.chapterUrl.ifBlank { "manga://${work.id}/chapter/${chapter.number}" },
                status = STATUS_DOWNLOADING,
                progress = progressFloat.toInt(),
                bytesDownloaded = bytesSoFar,
                totalBytes = bytesSoFar,
                localPath = localPaths.firstOrNull().orEmpty(),
                workId = work.id,
                chapterKey = chapterKey,
                chapterId = chapter.id,
                chapterNumber = chapter.number,
                sourceName = work.siteName,
                coverPath = localCoverPath,
                coverUrl = localCoverPath,
                downloadedPages = downloadedCount,
                totalPages = expectedPages,
                etaSeconds = etaSeconds,
                chaptersCount = 1
            )
        )
    }

    private suspend fun markCancelled(
        taskId: Long,
        taskKeyStr: String,
        work: MangaWork,
        chapter: MangaChapter,
        chapterKey: String,
        localCoverPath: String
    ) {
        notificationHelper?.cancelNotification(taskKeyStr)
        dao.upsertDownloadedChapter(
            DownloadedChapterEntity(
                chapterKey = chapterKey,
                workId = work.id,
                sourceId = work.sourceId,
                sourceName = work.siteName,
                workTitle = work.title,
                chapterId = chapter.id,
                chapterNumber = chapter.number,
                chapterNumberValue = chapter.chapterNumberValue,
                chapterNumberLabel = chapter.displayNumber,
                chapterTitle = chapter.title,
                chapterUrl = chapter.chapterUrl,
                dateText = chapter.dateText,
                pageCount = chapter.pageCount,
                downloadedPagesCount = 0,
                localPagesJson = "[]",
                totalBytes = 0L,
                status = STATUS_CANCELLED,
                progressPercent = 0f,
                etaSeconds = 0,
                errorMessage = "تم الإلغاء"
            )
        )
        dao.updateDownloadTask(
            DownloadTaskEntity(
                id = taskId,
                title = work.title,
                url = chapter.chapterUrl.ifBlank { "manga://${work.id}/chapter/${chapter.number}" },
                status = STATUS_CANCELLED,
                progress = 0,
                workId = work.id,
                chapterKey = chapterKey,
                chapterId = chapter.id,
                chapterNumber = chapter.number,
                sourceName = work.siteName,
                coverPath = localCoverPath,
                coverUrl = localCoverPath,
                errorMessage = "تم الإلغاء"
            )
        )
        refreshWorkStorageSummary(work.id, work.sourceId)
    }

    private suspend fun markFailed(
        taskId: Long,
        taskKeyStr: String,
        work: MangaWork,
        chapter: MangaChapter,
        chapterKey: String,
        localCoverPath: String,
        errorMsg: String
    ) {
        notificationHelper?.showDownloadFailed(
            taskId = taskKeyStr,
            workId = work.id,
            workTitle = work.title,
            chapterNumber = chapter.number,
            reason = errorMsg,
            isIncomplete = false
        )
        dao.upsertDownloadedChapter(
            DownloadedChapterEntity(
                chapterKey = chapterKey,
                workId = work.id,
                sourceId = work.sourceId,
                sourceName = work.siteName,
                workTitle = work.title,
                chapterId = chapter.id,
                chapterNumber = chapter.number,
                chapterNumberValue = chapter.chapterNumberValue,
                chapterNumberLabel = chapter.displayNumber,
                chapterTitle = chapter.title,
                chapterUrl = chapter.chapterUrl,
                dateText = chapter.dateText,
                pageCount = chapter.pageCount,
                downloadedPagesCount = 0,
                localPagesJson = "[]",
                totalBytes = 0L,
                status = STATUS_FAILED,
                progressPercent = 0f,
                etaSeconds = 0,
                errorMessage = errorMsg
            )
        )
        dao.updateDownloadTask(
            DownloadTaskEntity(
                id = taskId,
                title = work.title,
                url = chapter.chapterUrl.ifBlank { "manga://${work.id}/chapter/${chapter.number}" },
                status = STATUS_FAILED,
                progress = 0,
                workId = work.id,
                chapterKey = chapterKey,
                chapterId = chapter.id,
                chapterNumber = chapter.number,
                sourceName = work.siteName,
                coverPath = localCoverPath,
                coverUrl = localCoverPath,
                errorMessage = errorMsg
            )
        )
    }

    /**
     * Checks if a valid local cover already exists for this work; if not, downloads the remote cover
     * with site headers/cookies, validates the image bytes, and saves `cover.jpg` locally.
     * If cover download fails, chapter download continues without failing.
     */
    suspend fun ensureLocalWorkCover(work: MangaWork): String = withContext(Dispatchers.IO) {
        try {
            // 1. Check work.localCoverPath
            if (storageManager.hasValidLocalCover(work.localCoverPath)) {
                return@withContext work.localCoverPath
            }
            // 2. Check existing DB record or work directory
            val existingDbCover = dao.getDownloadedWorkById(work.id)?.localCoverPath.orEmpty()
            if (storageManager.hasValidLocalCover(existingDbCover)) {
                return@withContext existingDbCover
            }
            val existingFile = storageManager.getExistingValidCoverFile(work.sourceId, work.id)
            if (existingFile != null) {
                return@withContext existingFile.absolutePath
            }

            // 3. Download remote cover URL
            val coverUrl = work.effectiveCoverUrl ?: dao.getDownloadedWorkById(work.id)?.remoteCoverUrl
            if (!coverUrl.isNullOrBlank()) {
                val bytes = downloadSingleImageBytesWithRetry(
                    imageUrl = coverUrl,
                    refererUrl = work.mangaUrl.ifBlank { work.siteUrl },
                    siteUrl = work.siteUrl,
                    workId = work.id,
                    chapterId = "cover",
                    workTitle = work.title,
                    chapterTitle = "Cover",
                    pageNumber = 0
                )
                if (bytes != null && storageManager.isValidImageBytes(bytes)) {
                    val savedFile = storageManager.saveCoverImage(
                        sourceId = work.sourceId,
                        workId = work.id,
                        bytes = bytes,
                        allowSyntheticFallback = false
                    )
                    if (storageManager.isValidImageFile(savedFile)) {
                        DiagnosticLogger.logInfo(
                            category = DiagnosticCategory.COVER,
                            message = "تم حفظ غلاف المانجا محليًا بنجاح (${work.title})",
                            workId = work.id,
                            url = coverUrl
                        )
                        return@withContext savedFile.absolutePath
                    }
                } else {
                    DiagnosticLogger.logWarn(
                        category = DiagnosticCategory.COVER,
                        message = "تعذر تحميل غلاف المانجا (${work.title}) — سيستمر تنزيل الفصل بشكل طبيعي",
                        workId = work.id,
                        url = coverUrl
                    )
                }
            }
        } catch (e: Exception) {
            DiagnosticLogger.logWarn(
                category = DiagnosticCategory.COVER,
                message = "خطأ غير حرج أثناء حفظ الغلاف: ${e.localizedMessage}",
                workId = work.id
            )
        }
        ""
    }

    private suspend fun downloadSingleImageBytesWithRetry(
        imageUrl: String,
        refererUrl: String,
        siteUrl: String,
        workId: String,
        chapterId: String,
        workTitle: String,
        chapterTitle: String,
        pageNumber: Int
    ): ByteArray? {
        val maxAttempts = 4
        for (attempt in 1..maxAttempts) {
            val bytes = downloadSingleImageBytes(
                imageUrl = imageUrl,
                refererUrl = refererUrl,
                siteUrl = siteUrl,
                workId = workId,
                chapterId = chapterId,
                workTitle = workTitle,
                chapterTitle = chapterTitle,
                pageNumber = pageNumber,
                attempt = attempt
            )
            if (bytes != null && storageManager.isValidImageBytes(bytes)) {
                return bytes
            }
            if (attempt < maxAttempts && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                delay(300L * attempt)
            }
        }
        return null
    }

    private fun downloadSingleImageBytes(
        imageUrl: String,
        refererUrl: String,
        siteUrl: String,
        workId: String,
        chapterId: String,
        workTitle: String,
        chapterTitle: String,
        pageNumber: Int,
        attempt: Int,
        unwrappedHtmlDepth: Int = 0
    ): ByteArray? {
        if (imageUrl.startsWith("offline://") || imageUrl.startsWith("test://")) {
            if (imageUrl.contains("fail") || imageUrl.contains("missing")) {
                DiagnosticLogger.logError(
                    category = DiagnosticCategory.DOWNLOAD,
                    message = "فشل تحميل الصفحة $pageNumber (رابط اختبار فاشل)",
                    workId = workId,
                    chapterId = chapterId,
                    pageNumber = pageNumber,
                    url = imageUrl
                )
                return null
            }
            return storageManager.generateValidJpegBytes(
                title = workTitle,
                subtitle = chapterTitle,
                pageNumber = pageNumber
            )
        }
        if (imageUrl.startsWith("file://")) {
            val f = File(imageUrl.removePrefix("file://"))
            return if (f.exists()) f.readBytes() else null
        }
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            return null
        }

        return try {
            val cleanUrl = imageUrl.trim()
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "")
                .replace(" ", "%20")
            val siteOriginReferer = siteUrl.trim().trimEnd('/') + "/"
            val effectiveReferer = when (attempt) {
                1 -> refererUrl.ifBlank { siteOriginReferer }.trim()
                2 -> siteOriginReferer
                3 -> cleanUrl.substringBeforeLast('/', siteOriginReferer) + "/"
                else -> ""
            }
            val cleanUserAgent = LiveMangaSiteScraper.activeUserAgent
                .replace("; wv", "")
                .ifBlank { LiveMangaSiteScraper.DEFAULT_USER_AGENT }
            val builder = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", cleanUserAgent)
                .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .header("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")

            if (effectiveReferer.isNotBlank() && effectiveReferer.startsWith("http")) {
                builder.header("Referer", effectiveReferer)
            }

            val combinedCookies = LiveMangaSiteScraper.getCombinedCookies(
                targetUrl = cleanUrl,
                refererUrl = refererUrl.ifBlank { siteUrl },
                siteUrl = siteUrl
            )
            if (combinedCookies.isNotBlank()) {
                builder.header("Cookie", combinedCookies)
            }

            httpClient.newCall(builder.build()).execute().use { response ->
                val code = response.code
                if (!response.isSuccessful) {
                    DiagnosticLogger.logError(
                        category = DiagnosticCategory.NETWORK,
                        message = "فشل طلب الصورة (المحاولة $attempt) رمز HTTP $code",
                        workId = workId,
                        chapterId = chapterId,
                        pageNumber = if (pageNumber > 0) pageNumber else null,
                        url = cleanUrl,
                        httpCode = code
                    )
                    return null
                }
                val contentType = response.header("Content-Type").orEmpty()
                val expectedLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
                val bodyBytes = response.body?.bytes() ?: return null

                // Detect partial/truncated HTTP byte transfers so retry kicks in automatically
                if (expectedLength > 0L && bodyBytes.size.toLong() < expectedLength) {
                    DiagnosticLogger.logError(
                        category = DiagnosticCategory.NETWORK,
                        message = "فشل التحميل الجزئي للصورة (المستلم ${bodyBytes.size} من أصل $expectedLength بايت) — سيتم إعادة المحاولة",
                        workId = workId,
                        chapterId = chapterId,
                        pageNumber = if (pageNumber > 0) pageNumber else null,
                        url = cleanUrl,
                        httpCode = code
                    )
                    return null
                }

                // If server returned an HTML page instead of raw image bytes, extract the actual <img src> via OkHttp
                if (unwrappedHtmlDepth == 0 && contentType.contains("text/html", ignoreCase = true)) {
                    val html = bodyBytes.toString(Charsets.UTF_8)
                    val extractedImgUrls = LiveMangaSiteScraper.extractImgTagSrcUrls(html, cleanUrl)
                        .ifEmpty { LiveMangaSiteScraper.parseChapterPagesHtml(html, cleanUrl) }
                        .filter { it.startsWith("http") && !it.equals(cleanUrl, ignoreCase = true) }

                    val targetImgSrc = if (pageNumber > 0 && extractedImgUrls.size >= pageNumber) {
                        extractedImgUrls[pageNumber - 1]
                    } else {
                        extractedImgUrls.firstOrNull()
                    }

                    if (!targetImgSrc.isNullOrBlank()) {
                        DiagnosticLogger.logInfo(
                            category = DiagnosticCategory.DOWNLOAD,
                            message = "تم استخراج رابط الصورة الفعلي من وسم <img> (src) داخل الصفحة: $targetImgSrc",
                            workId = workId,
                            chapterId = chapterId,
                            pageNumber = if (pageNumber > 0) pageNumber else null,
                            url = targetImgSrc
                        )
                        return downloadSingleImageBytes(
                            imageUrl = targetImgSrc,
                            refererUrl = cleanUrl,
                            siteUrl = siteUrl,
                            workId = workId,
                            chapterId = chapterId,
                            workTitle = workTitle,
                            chapterTitle = chapterTitle,
                            pageNumber = pageNumber,
                            attempt = attempt,
                            unwrappedHtmlDepth = unwrappedHtmlDepth + 1
                        )
                    }
                }

                val isValidPayload = if (pageNumber > 0) {
                    !LiveMangaSiteScraper.isIgnoredAssetImage(cleanUrl) &&
                        storageManager.isValidChapterPageImageBytes(bodyBytes, contentType)
                } else {
                    storageManager.isValidImageBytes(bodyBytes)
                }
                if (isValidPayload) {
                    bodyBytes
                } else {
                    DiagnosticLogger.logError(
                        category = DiagnosticCategory.DOWNLOAD,
                        message = "استجابة غير صالحة للصورة (نوع المحتوى=$contentType، الحجم=${bodyBytes.size} بايت، أو صورة إيموجي/صفحة خطأ مستبعدة)",
                        workId = workId,
                        chapterId = chapterId,
                        pageNumber = if (pageNumber > 0) pageNumber else null,
                        url = cleanUrl,
                        httpCode = code
                    )
                    null
                }
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError(
                category = DiagnosticCategory.NETWORK,
                message = "خطأ شبكة أثناء تحميل الصفحة $pageNumber: ${e.localizedMessage}",
                workId = workId,
                chapterId = chapterId,
                pageNumber = if (pageNumber > 0) pageNumber else null,
                url = imageUrl
            )
            null
        }
    }

    private fun guessImageExtension(url: String, bytes: ByteArray): String {
        if (bytes.size >= 4) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            val b2 = bytes[2].toInt() and 0xFF
            val b3 = bytes[3].toInt() and 0xFF
            if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return "png"
            if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46) return "webp"
            if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return "gif"
            if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return "jpg"
        }
        val clean = url.substringBefore("?").lowercase()
        return when {
            clean.endsWith(".png") -> "png"
            clean.endsWith(".webp") -> "webp"
            clean.endsWith(".jpeg") -> "jpeg"
            else -> "jpg"
        }
    }

    suspend fun refreshWorkStorageSummary(workId: String, sourceId: Long) = withContext(Dispatchers.IO) {
        val existingWork = dao.getDownloadedWorkById(workId) ?: return@withContext
        val chapters = dao.getDownloadedChaptersForWorkOnce(workId)
        val completedChapters = chapters.count { ch ->
            ch.status == STATUS_COMPLETED &&
                storageManager.verifyChapterFilesComplete(ch.pageCount, storageManager.decodeLocalPathsJson(ch.localPagesJson))
        }
        val storageBytes = storageManager.getWorkStorageBytes(sourceId, workId)
            .coerceAtLeast(chapters.sumOf { it.totalBytes })
        val verifiedCover = existingWork.localCoverPath.takeIf { storageManager.hasValidLocalCover(it) }
            ?: storageManager.getExistingValidCoverFile(sourceId, workId)?.absolutePath.orEmpty()

        dao.upsertDownloadedWork(
            existingWork.copy(
                localCoverPath = verifiedCover,
                savedChaptersCount = completedChapters,
                totalStorageBytes = storageBytes,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun setPauseAllDownloads(paused: Boolean) {
        _isAllPaused.value = paused
        scope.launch {
            val allTasks = dao.getAllDownloadTasksOnce()
            allTasks.forEach { task ->
                if (paused && (task.status == STATUS_DOWNLOADING || task.status == STATUS_QUEUED)) {
                    dao.updateDownloadTask(task.copy(status = STATUS_PAUSED))
                } else if (!paused && task.status == STATUS_PAUSED) {
                    dao.updateDownloadTask(task.copy(status = STATUS_DOWNLOADING))
                }
            }
        }
    }

    fun setAllPaused(paused: Boolean) = setPauseAllDownloads(paused)

    fun togglePauseSingleTask(task: DownloadTaskEntity) {
        scope.launch {
            if (task.status == STATUS_PAUSED) {
                pausedTaskIds.remove(task.id)
                dao.updateDownloadTask(task.copy(status = STATUS_DOWNLOADING))
            } else if (task.status == STATUS_DOWNLOADING || task.status == STATUS_QUEUED) {
                pausedTaskIds.add(task.id)
                dao.updateDownloadTask(task.copy(status = STATUS_PAUSED))
            }
        }
    }

    fun togglePauseTask(task: DownloadTaskEntity) = togglePauseSingleTask(task)

    fun reorderQueueTask(task: DownloadTaskEntity, moveUp: Boolean) {
        scope.launch {
            val allTasks = dao.getAllDownloadTasksOnce()
            val idx = allTasks.indexOfFirst { it.id == task.id }
            if (idx == -1) return@launch
            val targetIdx = if (moveUp) idx - 1 else idx + 1
            if (targetIdx !in allTasks.indices) return@launch
            val neighbor = allTasks[targetIdx]
            val t1Time = task.createdAt
            val t2Time = neighbor.createdAt
            val newTaskTime = if (t1Time == t2Time) {
                if (moveUp) t2Time + 1000L else t2Time - 1000L
            } else {
                t2Time
            }
            val newNeighborTime = if (t1Time == t2Time) t2Time else t1Time
            dao.updateDownloadTask(task.copy(createdAt = newTaskTime))
            dao.updateDownloadTask(neighbor.copy(createdAt = newNeighborTime))
        }
    }

    fun cancelSingleTask(task: DownloadTaskEntity) {
        cancelledTaskIds.add(task.id)
        activeJobs[task.id]?.cancel()
        if (task.chapterKey.isNotBlank()) {
            activeChapterKeys.remove(task.chapterKey)
        }
        notificationHelper?.cancelNotification("${task.workId}_${task.chapterNumber}")
        scope.launch {
            if (task.chapterKey.isNotBlank()) {
                val ch = dao.getDownloadedChapterByKey(task.chapterKey)
                if (ch != null) {
                    val paths = storageManager.decodeLocalPathsJson(ch.localPagesJson)
                    storageManager.deleteChapterFiles(ch.sourceId, ch.workId, ch.chapterKey, paths)
                    dao.upsertDownloadedChapter(ch.copy(status = STATUS_CANCELLED, progressPercent = 0f, localPagesJson = "[]"))
                }
            }
            dao.updateDownloadTask(task.copy(status = STATUS_CANCELLED, progress = 0, errorMessage = "تم الإلغاء"))
        }
    }

    fun cancelTask(task: DownloadTaskEntity) = cancelSingleTask(task)

    fun cancelAllActiveTasks() {
        scope.launch {
            val tasks = dao.getAllDownloadTasksOnce()
            tasks.filter { it.status in setOf(STATUS_DOWNLOADING, STATUS_QUEUED, STATUS_PAUSED) }.forEach { task ->
                cancelSingleTask(task)
            }
        }
    }

    fun clearCompletedAndCancelledTasks() {
        scope.launch {
            val tasks = dao.getAllDownloadTasksOnce()
            tasks.filter { it.status in setOf(STATUS_COMPLETED, STATUS_CANCELLED, STATUS_FAILED) }.forEach { task ->
                dao.deleteDownloadTask(task)
            }
        }
    }

    fun clearFinishedDownloadRecords() = clearCompletedAndCancelledTasks()

    suspend fun deleteDownloadedChapterAndFiles(chapterKey: String) = withContext(Dispatchers.IO) {
        activeChapterKeys.remove(chapterKey)
        val ch = dao.getDownloadedChapterByKey(chapterKey) ?: return@withContext
        val paths = storageManager.decodeLocalPathsJson(ch.localPagesJson)
        storageManager.deleteChapterFiles(ch.sourceId, ch.workId, ch.chapterKey, paths)
        dao.deleteDownloadedChapterByKey(chapterKey)
        refreshWorkStorageSummary(ch.workId, ch.sourceId)
    }

    suspend fun deleteDownloadedChapter(ch: DownloadedChapterEntity) =
        deleteDownloadedChapterAndFiles(ch.chapterKey)

    suspend fun deleteDownloadedWorkAndAllFiles(workId: String) = withContext(Dispatchers.IO) {
        val work = dao.getDownloadedWorkById(workId)
        val sourceId = work?.sourceId ?: 0L
        storageManager.deleteWorkFiles(sourceId, workId)
        dao.deleteDownloadedChaptersForWork(workId)
        dao.deleteDownloadedWorkById(workId)
    }

    suspend fun deleteDownloadedWorkCompletely(workId: String) =
        deleteDownloadedWorkAndAllFiles(workId)

    suspend fun deleteTaskAndFiles(task: DownloadTaskEntity) = withContext(Dispatchers.IO) {
        cancelledTaskIds.add(task.id)
        activeJobs[task.id]?.cancel()
        if (task.chapterKey.isNotBlank()) {
            activeChapterKeys.remove(task.chapterKey)
            deleteDownloadedChapterAndFiles(task.chapterKey)
        }
        dao.deleteDownloadTask(task)
    }

    companion object {
        const val STATUS_QUEUED = "QUEUED"
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_INCOMPLETE = "INCOMPLETE"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}
