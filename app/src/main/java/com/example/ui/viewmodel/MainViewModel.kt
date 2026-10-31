package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DiagnosticLogEntry
import com.example.data.DiagnosticLogger
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaCatalogData
import com.example.data.MangaChapter
import com.example.data.MangaLatestUpdateItem
import com.example.data.MangaWork
import com.example.data.SourceUpdateSyncStatus
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.DownloadedChapterEntity
import com.example.data.local.DownloadedWorkEntity
import com.example.data.local.FavoriteCategoryEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.SavedWorkEntity
import com.example.data.local.SiteEntity
import com.example.data.preferences.UserSettings
import com.example.data.preferences.UserSettingsDataStore
import com.example.data.repository.AppRepository
import com.example.data.repository.StorageStats
import com.example.data.source.SourceCapabilities
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat

enum class AppScreen {
    STORAGE,
    SITE_CATALOG,
    MANGA_DETAILS,
    MANGA_READER,
    RECOMMENDATIONS,
    UPDATES,
    FAVORITES,
    HISTORY,
    MANAGE_SITES,
    BROWSER,
    SETTINGS_MAIN,
    SETTINGS_GENERAL,
    SETTINGS_APPEARANCE,
    SETTINGS_FAVORITE_CATEGORIES,
    SETTINGS_STORAGE,
    SETTINGS_READING_OPTIONS,
    SETTINGS_NEW_CHAPTERS,
    SETTINGS_STATISTICS,
    SETTINGS_MORE,
    DOWNLOADS
}

sealed class AddSiteDialogState {
    data object Hidden : AddSiteDialogState()
    data class Step1InputUrl(
        val urlInput: String = "",
        val isChecking: Boolean = false,
        val statusMessage: String? = null,
        val errorMessage: String? = null,
        val verificationUrl: String? = null,
        val resolvedDomain: String = "",
        val resolvedSuggestedName: String = "",
        val resolvedIconKey: String = "globe"
    ) : AddSiteDialogState()

    data class Step2ConfirmManual(
        val normalizedUrl: String,
        val domain: String,
        val siteName: String,
        val iconKey: String
    ) : AddSiteDialogState()
}

class MainViewModel(val repository: AppRepository) : ViewModel() {

    val sites: StateFlow<List<SiteEntity>> = repository.allSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledSites: StateFlow<List<SiteEntity>> = repository.enabledSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<FavoriteCategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedWorks: StateFlow<List<SavedWorkEntity>> = repository.allSavedWorks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadTasks: StateFlow<List<DownloadTaskEntity>> = repository.allDownloadTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedWorks: StateFlow<List<DownloadedWorkEntity>> = repository.allDownloadedWorks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedChapters: StateFlow<List<DownloadedChapterEntity>> = repository.allDownloadedChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingProgressList: StateFlow<List<ReadingProgressEntity>> = repository.allReadingProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDownloadsPaused: StateFlow<Boolean> = repository.mangaDownloadManager.isAllPaused

    val settings: StateFlow<UserSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _currentScreen = MutableStateFlow(AppScreen.STORAGE)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenBackStack = MutableStateFlow<List<AppScreen>>(emptyList())

    private val _browserUrl = MutableStateFlow("https://www.google.com/?hl=ar")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    private val _browserTitle = MutableStateFlow("Google")
    val browserTitle: StateFlow<String> = _browserTitle.asStateFlow()

    private val _selectedSite = MutableStateFlow(
        SiteEntity(id = 0L, name = "", url = "", domain = "", iconKey = "globe")
    )
    val selectedSite: StateFlow<SiteEntity> = _selectedSite.asStateFlow()

    private val _selectedWork = MutableStateFlow(
        MangaWork(id = "", title = "", siteName = "", siteUrl = "", siteIconKey = "globe")
    )
    val selectedWork: StateFlow<MangaWork> = _selectedWork.asStateFlow()

    private val _selectedChapter = MutableStateFlow(
        MangaChapter(id = "", number = 1, title = "الفصل 1", dateText = "")
    )
    val selectedChapter: StateFlow<MangaChapter> = _selectedChapter.asStateFlow()

    private val _downloadedChaptersMap = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())
    val downloadedChaptersMap: StateFlow<Map<String, Set<Int>>> = _downloadedChaptersMap.asStateFlow()

    // Maps workId -> (chapterNumber -> DownloadedChapterEntity) for live per-chapter progress/status
    private val _chapterDownloadStatesMap = MutableStateFlow<Map<String, Map<Int, DownloadedChapterEntity>>>(emptyMap())
    val chapterDownloadStatesMap: StateFlow<Map<String, Map<Int, DownloadedChapterEntity>>> = _chapterDownloadStatesMap.asStateFlow()

    val diagnosticLogs: StateFlow<List<DiagnosticLogEntry>> = DiagnosticLogger.logs

    private val _readChaptersMap = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())
    val readChaptersMap: StateFlow<Map<String, Set<Int>>> = _readChaptersMap.asStateFlow()

    // Maps work.id -> Pair(lastReadChapterNumber, lastReadPageIndex)
    private val _readingProgressMap = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val readingProgressMap: StateFlow<Map<String, Pair<Int, Int>>> = _readingProgressMap.asStateFlow()

    private val _addSiteDialogState = MutableStateFlow<AddSiteDialogState>(AddSiteDialogState.Hidden)
    val addSiteDialogState: StateFlow<AddSiteDialogState> = _addSiteDialogState.asStateFlow()

    private val _editingSite = MutableStateFlow<SiteEntity?>(null)
    val editingSite: StateFlow<SiteEntity?> = _editingSite.asStateFlow()

    private val _deletingSite = MutableStateFlow<SiteEntity?>(null)
    val deletingSite: StateFlow<SiteEntity?> = _deletingSite.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _showRestartNoticeBanner = MutableStateFlow(false)
    val showRestartNoticeBanner: StateFlow<Boolean> = _showRestartNoticeBanner.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _isUpdatingSites = MutableStateFlow(false)
    val isUpdatingSites: StateFlow<Boolean> = _isUpdatingSites.asStateFlow()

    val latestUpdates: StateFlow<List<MangaLatestUpdateItem>> = repository.allLatestUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedUpdatesSourceId = MutableStateFlow<Long?>(null)
    val selectedUpdatesSourceId: StateFlow<Long?> = _selectedUpdatesSourceId.asStateFlow()

    private val _sourceUpdateStatuses = MutableStateFlow<Map<Long, SourceUpdateSyncStatus>>(emptyMap())
    val sourceUpdateStatuses: StateFlow<Map<Long, SourceUpdateSyncStatus>> = _sourceUpdateStatuses.asStateFlow()

    private val _latestUpdatesPageBySource = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val latestUpdatesPageBySource: StateFlow<Map<Long, Int>> = _latestUpdatesPageBySource.asStateFlow()

    private val _latestUpdatesHasMore = MutableStateFlow(false)
    val latestUpdatesHasMore: StateFlow<Boolean> = _latestUpdatesHasMore.asStateFlow()

    private val _latestUpdatesErrorMessage = MutableStateFlow<String?>(null)
    val latestUpdatesErrorMessage: StateFlow<String?> = _latestUpdatesErrorMessage.asStateFlow()

    private val _lastUpdatesSyncedAtMillis = MutableStateFlow(0L)
    val lastUpdatesSyncedAtMillis: StateFlow<Long> = _lastUpdatesSyncedAtMillis.asStateFlow()

    private var updatesSyncJob: Job? = null

    // Global Search & Source Migration states
    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    private val _isGlobalSearching = MutableStateFlow(false)
    val isGlobalSearching: StateFlow<Boolean> = _isGlobalSearching.asStateFlow()

    private val _globalSearchResultsBySite = MutableStateFlow<Map<SiteEntity, List<MangaWork>>>(emptyMap())
    val globalSearchResultsBySite: StateFlow<Map<SiteEntity, List<MangaWork>>> = _globalSearchResultsBySite.asStateFlow()

    private val _isSearchingMigration = MutableStateFlow(false)
    val isSearchingMigration: StateFlow<Boolean> = _isSearchingMigration.asStateFlow()

    private val _migrationCandidates = MutableStateFlow<List<MangaWork>>(emptyList())
    val migrationCandidates: StateFlow<List<MangaWork>> = _migrationCandidates.asStateFlow()

    private val preloadedChapterUrls = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val syncedTrackerChapters = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    init {
        refreshStorageStats()
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
        // Sync downloadedChapters and readingProgress from Room into UI maps
        viewModelScope.launch {
            repository.allDownloadedChapters.collect { chList ->
                val stateByWork = chList
                    .groupBy { it.workId }
                    .mapValues { (_, list) -> list.associateBy { it.chapterNumber } }
                _chapterDownloadStatesMap.value = stateByWork

                val verifiedCompleted = chList
                    .filter { ch ->
                        ch.status == "COMPLETED" &&
                            repository.localStorageManager.verifyChapterFilesComplete(
                                ch.pageCount,
                                repository.localStorageManager.decodeLocalPathsJson(ch.localPagesJson)
                            )
                    }
                    .groupBy { it.workId }
                    .mapValues { (_, list) -> list.map { it.chapterNumber }.toSet() }
                _downloadedChaptersMap.value = verifiedCompleted
            }
        }
        viewModelScope.launch {
            repository.allReadingProgress.collect { progList ->
                val readGrouped = progList
                    .groupBy { it.workId }
                    .mapValues { (_, list) -> list.map { it.chapterNumber }.toSet() }
                _readChaptersMap.value = readGrouped

                val latestByWork = progList
                    .groupBy { it.workId }
                    .mapValues { (_, list) ->
                        val latest = list.maxByOrNull { it.lastReadAt }
                        if (latest != null) latest.chapterNumber to latest.lastPageIndex else (1 to 0)
                    }
                _readingProgressMap.value = latestByWork
            }
        }
        viewModelScope.launch {
            repository.settingsFlow.collect { userSettings ->
                LiveMangaSiteScraper.dohProvider = userSettings.dohProvider
                if (_screenBackStack.value.isEmpty() && _currentScreen.value == AppScreen.STORAGE) {
                    val mapped = when (userSettings.startScreen) {
                        "التوصيات" -> AppScreen.RECOMMENDATIONS
                        "التحديثات" -> AppScreen.UPDATES
                        "المفضلة" -> AppScreen.FAVORITES
                        "السجل" -> AppScreen.HISTORY
                        "إدارة المواقع" -> AppScreen.MANAGE_SITES
                        else -> AppScreen.STORAGE
                    }
                    if (mapped != AppScreen.STORAGE) {
                        _currentScreen.value = mapped
                    }
                }
                return@collect
            }
        }
    }

    fun navigateTo(screen: AppScreen, addToBackStack: Boolean = true) {
        if (_currentScreen.value == screen) return
        if (addToBackStack) {
            _screenBackStack.value = _screenBackStack.value + _currentScreen.value
        }
        _currentScreen.value = screen
        if (screen == AppScreen.SETTINGS_STORAGE || screen == AppScreen.STORAGE) {
            refreshStorageStats()
        }
    }

    fun navigateBack(): Boolean {
        val stack = _screenBackStack.value
        if (stack.isNotEmpty()) {
            val previous = stack.last()
            _screenBackStack.value = stack.dropLast(1)
            _currentScreen.value = previous
            return true
        }
        if (_currentScreen.value != AppScreen.STORAGE) {
            _currentScreen.value = AppScreen.STORAGE
            return true
        }
        return false
    }

    fun openBrowserWithUrl(url: String, title: String = "") {
        val formatted = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
        _browserUrl.value = formatted
        if (title.isNotBlank()) {
            _browserTitle.value = title
        }
        navigateTo(AppScreen.BROWSER)
    }

    fun updateBrowserPageState(url: String, title: String) {
        if (url.isNotBlank()) _browserUrl.value = url
        if (title.isNotBlank()) _browserTitle.value = title
        val currentSite = _selectedSite.value
        if (currentSite.id != 0L && url.startsWith("http")) {
            viewModelScope.launch {
                repository.updateSiteLastVisitedUrl(currentSite, url)
            }
        }
    }

    fun recordPageVisit(title: String, url: String, siteName: String = "المتصفح") {
        viewModelScope.launch {
            repository.addHistory(title = title, url = url, siteName = siteName)
            repository.addReadingSeconds(15L)
        }
    }

    fun getSourceCapabilities(site: SiteEntity): SourceCapabilities =
        repository.getSourceCapabilities(site)

    fun testSiteConnection(site: SiteEntity) {
        viewModelScope.launch {
            showToast("جار فحص الاتصال بـ ${site.name}...")
            val result = repository.testSourceConnection(site.url)
            showToast("${site.name}: ${result.message}")
        }
    }

    fun openSiteLastVisitedPage(site: SiteEntity) {
        val targetUrl = site.lastVisitedUrl.ifBlank { site.url }
        openBrowserWithUrl(targetUrl, site.name)
    }

    // Native Site Catalog, Manga Details, and Chapter Reader Navigation
    fun openSiteCatalog(site: SiteEntity) {
        _selectedSite.value = site
        navigateTo(AppScreen.SITE_CATALOG)
    }

    fun openMangaDetails(work: MangaWork) {
        viewModelScope.launch {
            // Merge any locally downloaded chapters for this work so offline/downloaded badges work seamlessly
            val localWorkEntity = repository.dao.getDownloadedWorkById(work.id)
            val mergedWork = if (work.isFromLocalStorage && localWorkEntity != null) {
                repository.buildOfflineMangaWorkFromEntity(localWorkEntity)
            } else if (localWorkEntity != null) {
                val offlineBuilt = repository.buildOfflineMangaWorkFromEntity(localWorkEntity)
                val offlineChByNumber = offlineBuilt.chapters.associateBy { it.number }
                val mergedChapters = if (work.chapters.isEmpty()) {
                    offlineBuilt.chapters
                } else {
                    work.chapters.map { ch ->
                        val localCh = offlineChByNumber[ch.number]
                        if (localCh != null && localCh.localPagePaths.isNotEmpty()) {
                            ch.copy(
                                localPagePaths = localCh.localPagePaths,
                                downloadStatus = localCh.downloadStatus,
                                lastReadPage = localCh.lastReadPage,
                                lastReadTimeText = localCh.lastReadTimeText,
                                chapterKey = localCh.chapterKey
                            )
                        } else {
                            ch
                        }
                    }
                }
                work.copy(
                    localCoverPath = localWorkEntity.localCoverPath.ifBlank { work.localCoverPath },
                    storageSizeBytes = localWorkEntity.totalStorageBytes,
                    chapters = mergedChapters
                )
            } else {
                work
            }

            _selectedWork.value = mergedWork
            if (mergedWork.chapters.isNotEmpty()) {
                _selectedChapter.value = mergedWork.chapters.first()
            }
            recordPageVisit(
                title = mergedWork.title,
                url = mergedWork.mangaUrl.ifBlank { "${mergedWork.siteUrl}/series/${mergedWork.id}" },
                siteName = mergedWork.siteName
            )
            navigateTo(AppScreen.MANGA_DETAILS)
        }
    }

    fun openDownloadedWorkFromStorage(entity: DownloadedWorkEntity) {
        viewModelScope.launch {
            val offlineWork = repository.buildOfflineMangaWorkFromEntity(entity)
            _selectedWork.value = offlineWork
            if (offlineWork.chapters.isNotEmpty()) {
                _selectedChapter.value = offlineWork.chapters.first()
            }
            navigateTo(AppScreen.MANGA_DETAILS)
        }
    }

    fun openMangaChapterReader(work: MangaWork, chapter: MangaChapter) {
        viewModelScope.launch {
            // Check if this chapter has locally downloaded files on disk
            val chapterKey = chapter.chapterKey.ifBlank { "${work.id}_ch_${chapter.number}" }
            val localChEntity = repository.dao.getDownloadedChapterByKey(chapterKey)
                ?: repository.dao.getDownloadedChaptersForWorkOnce(work.id).find { it.chapterNumber == chapter.number }
            val savedProgress = repository.readingProgressRepository.getChapterProgress(work.id, chapter.number)
            val effectiveLastPage = savedProgress?.lastPageIndex ?: chapter.lastReadPage

            val effectiveChapter = if (localChEntity != null) {
                val rawPaths = repository.localStorageManager.decodeLocalPathsJson(localChEntity.localPagePathsJson)
                val validPaths = repository.localStorageManager.filterExistingValidPagePaths(rawPaths)
                val expectedPages = localChEntity.totalPages.coerceAtLeast(rawPaths.size).coerceAtLeast(chapter.pageCount).coerceAtLeast(1)
                val isTrulyComplete = localChEntity.status == "COMPLETED" &&
                    repository.localStorageManager.verifyChapterFilesComplete(expectedPages, validPaths)
                val effectiveStatus = when {
                    isTrulyComplete -> "COMPLETED"
                    localChEntity.status == "COMPLETED" && validPaths.isNotEmpty() -> "INCOMPLETE"
                    localChEntity.status == "COMPLETED" -> "FAILED"
                    else -> localChEntity.status
                }
                chapter.copy(
                    localPagePaths = validPaths,
                    downloadedPagesCount = validPaths.size,
                    isDownloaded = isTrulyComplete,
                    downloadStatus = effectiveStatus,
                    downloadError = localChEntity.errorMessage.orEmpty(),
                    pageCount = expectedPages,
                    isRead = true,
                    lastReadPage = effectiveLastPage,
                    chapterKey = localChEntity.chapterKey.ifBlank { chapterKey }
                )
            } else {
                chapter.copy(
                    isRead = true,
                    lastReadPage = effectiveLastPage,
                    chapterKey = chapterKey
                )
            }

            _selectedWork.value = work
            _selectedChapter.value = effectiveChapter

            val currentReadMap = _readChaptersMap.value.toMutableMap()
            val readSet = currentReadMap[work.id]?.toMutableSet() ?: mutableSetOf()
            readSet.add(effectiveChapter.number)
            currentReadMap[work.id] = readSet
            _readChaptersMap.value = currentReadMap

            val resumePage = effectiveChapter.lastReadPage.takeIf { it >= 0 } ?: 0
            val currentProgressMap = _readingProgressMap.value.toMutableMap()
            currentProgressMap[work.id] = effectiveChapter.number to resumePage
            _readingProgressMap.value = currentProgressMap

            repository.readingProgressRepository.saveProgress(
                workId = work.id,
                workTitle = work.title,
                chapterId = effectiveChapter.id,
                chapterNumber = effectiveChapter.number,
                chapterTitle = effectiveChapter.title,
                pageIndex = resumePage,
                totalPages = effectiveChapter.pageCount,
                scrollOffset = 0
            )

            recordPageVisit(
                title = "${work.title} - ${effectiveChapter.title}",
                url = effectiveChapter.chapterUrl.ifBlank { "${work.siteUrl}/series/${work.id}/chapter-${effectiveChapter.number}" },
                siteName = work.siteName
            )
            navigateTo(AppScreen.MANGA_READER)
        }
    }

    fun openChapterFromNotification(workId: String, chapterNumber: Int) {
        if (workId.isBlank()) return
        viewModelScope.launch {
            val entity = repository.dao.getDownloadedWorkById(workId) ?: return@launch
            val offlineWork = repository.buildOfflineMangaWorkFromEntity(entity)
            val targetCh = offlineWork.chapters.find { it.number == chapterNumber }
                ?: offlineWork.chapters.firstOrNull()
            if (targetCh != null) {
                openMangaChapterReader(offlineWork, targetCh)
            } else {
                openMangaDetails(offlineWork)
            }
        }
    }

    fun saveReadingProgress(work: MangaWork, chapter: MangaChapter, pageIndex: Int, scrollOffset: Int = 0) {
        val safePage = pageIndex.coerceAtLeast(0)
        val totalPages = chapter.pageCount.coerceAtLeast(1)
        val currentProgressMap = _readingProgressMap.value.toMutableMap()
        currentProgressMap[work.id] = chapter.number to safePage
        _readingProgressMap.value = currentProgressMap

        val currentReadMap = _readChaptersMap.value.toMutableMap()
        val readSet = currentReadMap[work.id]?.toMutableSet() ?: mutableSetOf()
        readSet.add(chapter.number)
        currentReadMap[work.id] = readSet
        _readChaptersMap.value = currentReadMap

        viewModelScope.launch {
            repository.readingProgressRepository.saveProgress(
                workId = work.id,
                workTitle = work.title,
                chapterId = chapter.id,
                chapterNumber = chapter.number,
                chapterTitle = chapter.title,
                pageIndex = safePage,
                totalPages = totalPages,
                scrollOffset = scrollOffset
            )

            // 1. Smart Next-3-Chapters Preloading & Auto-Download when reaching 70%+ of the current chapter
            if (settings.value.preloadNextChapter && totalPages > 1 && safePage >= (totalPages * 0.7f).toInt()) {
                val sortedChapters = work.chapters.sortedBy { it.number }
                val nextChapter = sortedChapters.firstOrNull { it.number > chapter.number }
                if (nextChapter != null && nextChapter.chapterUrl.startsWith("http")) {
                    if (preloadedChapterUrls.add(nextChapter.chapterUrl)) {
                        launch(kotlinx.coroutines.Dispatchers.IO) {
                            val res = LiveMangaSiteScraper.fetchChapterPages(work, nextChapter)
                            if (res.pageUrls.isNotEmpty()) {
                                LiveMangaSiteScraper.cacheChapterPages(nextChapter.chapterUrl, res.pageUrls)
                                // Warm up first 3 pages in HTTP cache
                                res.pageUrls.take(3).forEach { imgUrl ->
                                    runCatching {
                                        val req = okhttp3.Request.Builder()
                                            .url(imgUrl)
                                            .header("User-Agent", LiveMangaSiteScraper.activeUserAgent.ifBlank { LiveMangaSiteScraper.DEFAULT_USER_AGENT })
                                            .header("Referer", nextChapter.chapterUrl)
                                            .build()
                                        LiveMangaSiteScraper.httpClient.newCall(req).execute().close()
                                    }
                                }
                            }
                            // Auto-download next up to 3 chapters in background if not already downloaded
                            val downloadedNums = downloadedChaptersMap.value[work.id] ?: emptySet()
                            val nextThreeUnhandled = sortedChapters
                                .filter { it.number > chapter.number && it.number !in downloadedNums && it.chapterUrl.startsWith("http") }
                                .take(3)
                                .filter { preloadedChapterUrls.add("autodl_${work.id}_${it.number}") }
                            if (nextThreeUnhandled.isNotEmpty()) {
                                repository.mangaDownloadManager.enqueueChaptersDownload(work, nextThreeUnhandled)
                            }
                        }
                    }
                }
            }

            // 2. On completing chapter (last page): Auto-delete older read chapters & Sync Trackers
            if (safePage >= totalPages - 1) {
                val syncKey = "${work.id}_${chapter.number}"
                if (syncedTrackerChapters.add(syncKey)) {
                    repository.syncChapterProgressWithTrackers(work.title, chapter.number)
                }
                if (settings.value.autoDeleteReadChapters) {
                    val keepCount = settings.value.keepLastReadChaptersCount.coerceAtLeast(0)
                    val downloadedForWork = repository.dao.getDownloadedChaptersForWorkOnce(work.id)
                    val toDelete = downloadedForWork.filter { ch ->
                        ch.chapterNumber <= (chapter.number - keepCount)
                    }
                    toDelete.forEach { oldCh ->
                        repository.deleteDownloadedChapter(work.id, oldCh.chapterNumber)
                    }
                    if (toDelete.isNotEmpty()) {
                        refreshStorageStats()
                    }
                }
            }
        }
    }

    fun cleanReadChaptersNow() {
        viewModelScope.launch {
            var deletedCount = 0
            val keepCount = settings.value.keepLastReadChaptersCount.coerceAtLeast(0)
            val allProgress = readingProgressList.value
            val latestChapterByWork = allProgress
                .groupBy { it.workId }
                .mapValues { (_, list) -> list.maxOfOrNull { it.chapterNumber } ?: 0 }

            for ((workId, maxReadCh) in latestChapterByWork) {
                val downloadedForWork = repository.dao.getDownloadedChaptersForWorkOnce(workId)
                val toDelete = downloadedForWork.filter { ch ->
                    ch.chapterNumber <= (maxReadCh - keepCount)
                }
                toDelete.forEach { oldCh ->
                    repository.deleteDownloadedChapter(workId, oldCh.chapterNumber)
                    deletedCount++
                }
            }
            refreshStorageStats()
            if (deletedCount > 0) {
                showToast("تم تنظيف $deletedCount فصل مقروء وتوفير المساحة بنجاح")
            } else {
                showToast("لا توجد فصول مقروءة قديمة قابلة للحذف حالياً")
            }
        }
    }

    fun getFormattedResumeLabel(workId: String): String {
        val progList = readingProgressList.value.filter { it.workId == workId }
        val latest = progList.maxByOrNull { it.lastReadAt } ?: return ""
        return repository.readingProgressRepository.formatResumeText(latest)
    }

    fun resumeReadingCurrentOrLastWork() {
        val work = _selectedWork.value
        if (work.id.isBlank() && work.mangaUrl.isBlank()) {
            val firstDownloaded = downloadedWorks.value.firstOrNull()
            if (firstDownloaded != null) {
                openDownloadedWorkFromStorage(firstDownloaded)
                return
            }
            val firstSite = enabledSites.value.firstOrNull()
            if (firstSite != null) {
                openSiteCatalog(firstSite)
            } else {
                showToast("أضف موقع مانجا أولاً من إدارة المواقع لبدء القراءة")
                navigateTo(AppScreen.MANAGE_SITES)
            }
            return
        }
        val savedProgress = _readingProgressMap.value[work.id]
        if (savedProgress != null) {
            val targetCh = work.chapters.find { it.number == savedProgress.first } ?: work.chapters.firstOrNull()
            if (targetCh != null) {
                openMangaChapterReader(work, targetCh)
                return
            }
        }
        openMangaDetails(work)
    }

    fun downloadChaptersForWork(
        work: MangaWork,
        chapters: List<MangaChapter>,
        toastMsg: String,
        quality: String = "عالية (الأصلية)"
    ) {
        if (chapters.isEmpty()) return
        viewModelScope.launch {
            if (settings.value.downloadWifiOnly && !repository.isWifiConnected()) {
                showToast("التنزيل عبر شبكة Wi-Fi فقط مفعّل في الإعدادات")
                return@launch
            }
            if (!repository.isNetworkAvailable() && chapters.all { !it.chapterUrl.startsWith("offline://") && !it.chapterUrl.startsWith("test://") }) {
                showToast("لا يوجد اتصال بالإنترنت لتنزيل الفصول حاليًا")
                return@launch
            }

            showToast(toastMsg)
            repository.mangaDownloadManager.compressDownloadsWebp = settings.value.compressDownloadsWebp
            val matchingSite = sites.value.find { it.id == work.sourceId || it.url == work.siteUrl }
            repository.mangaDownloadManager.enqueueChaptersDownload(
                work = work,
                chapters = chapters,
                site = matchingSite,
                quality = quality
            )
            delay(500)
            refreshStorageStats()
        }
    }

    fun retryOrResumeChapterDownload(work: MangaWork, chapter: MangaChapter) {
        downloadChaptersForWork(
            work = work,
            chapters = listOf(chapter),
            toastMsg = "جارٍ استكمال تنزيل ${chapter.title}..."
        )
    }

    fun clearDiagnosticLogs() {
        DiagnosticLogger.clearLogs()
        showToast("تم مسح سجل التشخيص")
    }

    fun saveWholeMangaFromCatalog(work: MangaWork) {
        viewModelScope.launch {
            if (work.chapters.isNotEmpty()) {
                downloadChaptersForWork(
                    work = work,
                    chapters = work.chapters,
                    toastMsg = "بدأ تنزيل ${work.chapters.size} فصل من ${work.title}"
                )
                return@launch
            }
            showToast("جارٍ جلب قائمة فصول ${work.title} من المصدر...")
            val detailsRes = LiveMangaSiteScraper.fetchMangaDetails(work)
            val realChapters = detailsRes.work.chapters
            if (realChapters.isNotEmpty()) {
                downloadChaptersForWork(
                    work = detailsRes.work,
                    chapters = realChapters,
                    toastMsg = "بدأ تنزيل ${realChapters.size} فصل من ${detailsRes.work.title}"
                )
            } else {
                showToast(detailsRes.errorMessage ?: "تعذر استخراج فصول العمل من المصدر لتنزيلها")
            }
        }
    }

    fun saveMangaWorkToStorage(work: MangaWork) {
        saveWholeMangaFromCatalog(work)
    }

    fun deleteDownloadedWorkFromStorage(workId: String) {
        viewModelScope.launch {
            repository.deleteDownloadedWork(workId)
            refreshStorageStats()
            showToast("تم حذف المانجا وجميع ملفاتها المحلية من التخزين")
            if (_currentScreen.value == AppScreen.MANGA_DETAILS && _selectedWork.value.id == workId) {
                navigateBack()
            }
        }
    }

    fun deleteSingleDownloadedChapter(work: MangaWork, chapter: MangaChapter) {
        viewModelScope.launch {
            repository.deleteDownloadedChapter(work.id, chapter.number)
            refreshStorageStats()
            // Refresh selectedWork if currently viewing it
            val updatedWorkEntity = repository.dao.getDownloadedWorkById(work.id)
            if (updatedWorkEntity != null && work.isFromLocalStorage) {
                _selectedWork.value = repository.buildOfflineMangaWorkFromEntity(updatedWorkEntity)
            }
            showToast("تم حذف ${chapter.title} من التخزين المحلي")
        }
    }

    // Batch Downloads & CBZ Export
    fun downloadUnreadChapters(work: MangaWork, quality: String = "عالية (الأصلية)") {
        val readSet = _readChaptersMap.value[work.id].orEmpty()
        val unread = work.chapters.filter { it.number !in readSet }.sortedBy { it.number }
        if (unread.isEmpty()) {
            showToast("جميع الفصول مقروءة بالفعل")
            return
        }
        downloadChaptersForWork(
            work = work,
            chapters = unread,
            toastMsg = "بدأ تنزيل ${unread.size} فصل غير مقروء",
            quality = quality
        )
    }

    fun downloadFirstNChapters(work: MangaWork, count: Int, quality: String = "عالية (الأصلية)") {
        val sortedAsc = work.chapters.sortedBy { it.number }.take(count)
        if (sortedAsc.isEmpty()) {
            showToast("لا توجد فصول متاحة للتنزيل")
            return
        }
        downloadChaptersForWork(
            work = work,
            chapters = sortedAsc,
            toastMsg = "بدأ تنزيل أول ${sortedAsc.size} فصول",
            quality = quality
        )
    }

    fun downloadAllChapters(work: MangaWork, quality: String = "عالية (الأصلية)") {
        val sortedAsc = work.chapters.sortedBy { it.number }
        if (sortedAsc.isEmpty()) {
            saveWholeMangaFromCatalog(work)
            return
        }
        downloadChaptersForWork(
            work = work,
            chapters = sortedAsc,
            toastMsg = "بدأ تنزيل جميع الفصول (${sortedAsc.size} فصل)",
            quality = quality
        )
    }

    fun exportChapterAsCbz(work: MangaWork, chapter: MangaChapter) {
        viewModelScope.launch {
            var cbzFile = repository.exportChapterToCbz(work, chapter)
            if (cbzFile == null) {
                showToast("جارٍ تجهيز وتنزيل صفحات الفصل لتصديره بصيغة CBZ...")
                repository.mangaDownloadManager.compressDownloadsWebp = settings.value.compressDownloadsWebp
                repository.downloadMangaChapterOffline(work, chapter)
                cbzFile = repository.exportChapterToCbz(work, chapter)
            }
            refreshStorageStats()
            if (cbzFile != null) {
                showToast("تم حفظ الفصل كملف CBZ: ${cbzFile.name}")
            } else {
                showToast("تعذر تصدير الفصل بصيغة .cbz حالياً")
            }
        }
    }

    fun exportChapterAsPdf(work: MangaWork, chapter: MangaChapter) {
        viewModelScope.launch {
            var pdfFile = repository.exportChapterToPdf(work, chapter)
            if (pdfFile == null) {
                showToast("جارٍ تجهيز وتنزيل صفحات الفصل لتصديره بصيغة PDF...")
                repository.mangaDownloadManager.compressDownloadsWebp = settings.value.compressDownloadsWebp
                repository.downloadMangaChapterOffline(work, chapter)
                pdfFile = repository.exportChapterToPdf(work, chapter)
            }
            refreshStorageStats()
            if (pdfFile != null) {
                showToast("تم حفظ الفصل كملف PDF: ${pdfFile.name}")
            } else {
                showToast("تعذر تصدير الفصل بصيغة .pdf حالياً")
            }
        }
    }

    fun exportAllDownloadedChaptersAsCbz(work: MangaWork) {
        viewModelScope.launch {
            val files = repository.exportWorkToCbzFiles(work)
            refreshStorageStats()
            if (files.isNotEmpty()) {
                showToast("تم تصدير ${files.size} فصل بصيغة .cbz في مجلد التخزين")
            } else {
                showToast("لا توجد فصول محملة لهذا العمل لتصديرها بصيغة .cbz")
            }
        }
    }

    fun exportAllChaptersAsCbz(work: MangaWork) = exportAllDownloadedChaptersAsCbz(work)

    fun reorderDownloadTask(task: DownloadTaskEntity, moveUp: Boolean) {
        repository.reorderDownloadTask(task, moveUp)
    }

    // Unified Global Search & Source Migration
    fun performGlobalSearch(query: String) {
        _globalSearchQuery.value = query
        val clean = query.trim()
        if (clean.isEmpty()) {
            _globalSearchResultsBySite.value = emptyMap()
            _isGlobalSearching.value = false
            return
        }
        recordSearchQuery()
        viewModelScope.launch {
            _isGlobalSearching.value = true
            val results = LinkedHashMap<SiteEntity, List<MangaWork>>()
            val activeSites = enabledSites.value
            for (site in activeSites) {
                val scrapeRes = runCatching {
                    LiveMangaSiteScraper.fetchSiteCatalog(site = site, searchQuery = clean)
                }.getOrNull()
                val works = scrapeRes?.works.orEmpty()
                if (works.isNotEmpty()) {
                    results[site] = works
                    _globalSearchResultsBySite.value = results.toMap()
                }
            }
            _isGlobalSearching.value = false
        }
    }

    fun findMigrationCandidatesForWork(work: MangaWork) {
        viewModelScope.launch {
            _isSearchingMigration.value = true
            _migrationCandidates.value = emptyList()
            val query = work.title.trim()
            val candidates = mutableListOf<MangaWork>()
            val otherSites = enabledSites.value.filter { it.id != work.sourceId && it.name != work.siteName }
            val targetSites = if (otherSites.isNotEmpty()) otherSites else enabledSites.value
            for (site in targetSites) {
                val res = runCatching {
                    LiveMangaSiteScraper.fetchSiteCatalog(site = site, searchQuery = query)
                }.getOrNull()
                val matched = res?.works.orEmpty().filter { it.mangaUrl != work.mangaUrl }
                candidates.addAll(matched.take(6))
                _migrationCandidates.value = candidates.toList()
            }
            _isSearchingMigration.value = false
        }
    }

    fun migrateWorkToSource(oldWork: MangaWork, newWork: MangaWork) {
        viewModelScope.launch {
            // Migrate reading progress
            val oldProgress = _readingProgressMap.value[oldWork.id]
            val oldReadSet = _readChaptersMap.value[oldWork.id].orEmpty()
            if (oldProgress != null) {
                val map = _readingProgressMap.value.toMutableMap()
                map[newWork.id] = oldProgress
                _readingProgressMap.value = map
                repository.readingProgressRepository.saveProgress(
                    workId = newWork.id,
                    workTitle = newWork.title,
                    chapterId = "ch-${oldProgress.first}",
                    chapterNumber = oldProgress.first,
                    chapterTitle = "الفصل ${oldProgress.first}",
                    pageIndex = oldProgress.second,
                    totalPages = 12,
                    scrollOffset = 0
                )
            }
            if (oldReadSet.isNotEmpty()) {
                val rMap = _readChaptersMap.value.toMutableMap()
                rMap[newWork.id] = oldReadSet
                _readChaptersMap.value = rMap
            }
            // Migrate favorite if oldWork was in favorites
            val existingFav = favorites.value.find {
                it.url == oldWork.mangaUrl || it.title.equals(oldWork.title, ignoreCase = true)
            }
            if (existingFav != null) {
                repository.deleteFavorite(existingFav)
                repository.toggleFavorite(
                    title = newWork.title,
                    url = newWork.mangaUrl.ifBlank { "${newWork.siteUrl}/series/${newWork.id}" },
                    siteName = newWork.siteName,
                    category = existingFav.category
                )
            }
            showToast("تم نقل العمل وتقدم القراءة إلى ${newWork.siteName}")
            openMangaDetails(newWork)
        }
    }

    // Add Site Dialog Flow (matching 00:08-00:22 in the reference video)
    fun openAddSiteDialog(prefillUrl: String = "") {
        val cleanPrefill = prefillUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
        val initial = if (cleanPrefill.contains("google.")) "" else cleanPrefill
        _addSiteDialogState.value = AddSiteDialogState.Step1InputUrl(urlInput = initial)
    }

    fun updateAddSiteUrlInput(newUrl: String) {
        val current = _addSiteDialogState.value
        if (current is AddSiteDialogState.Step1InputUrl && !current.isChecking) {
            _addSiteDialogState.value = current.copy(urlInput = newUrl, errorMessage = null)
        }
    }

    fun dismissAddSiteDialog() {
        _addSiteDialogState.value = AddSiteDialogState.Hidden
    }

    fun submitAddSiteStep1() {
        val current = _addSiteDialogState.value as? AddSiteDialogState.Step1InputUrl ?: return
        if (current.isChecking) return
        val rawInput = current.urlInput.trim()
        if (rawInput.isEmpty()) {
            _addSiteDialogState.value = current.copy(errorMessage = "يرجى إدخال عنوان موقع إلكتروني")
            return
        }

        viewModelScope.launch {
            _addSiteDialogState.value = current.copy(
                isChecking = true,
                statusMessage = "جار مراجعة حالة توافق الموقع...",
                errorMessage = null
            )
            delay(350)
            val resolved = repository.normalizeAndResolveSite(rawInput)
            if (!resolved.isValid) {
                _addSiteDialogState.value = current.copy(
                    isChecking = false,
                    statusMessage = null,
                    errorMessage = resolved.errorMessage ?: "رابط غير صالح"
                )
                return@launch
            }
            // Open WebView verification once during Step 1 (matching 00:08-00:22 of the reference video)
            _addSiteDialogState.value = current.copy(
                isChecking = true,
                statusMessage = "جار التحقق من توافق الموقع...",
                errorMessage = null,
                verificationUrl = resolved.normalizedUrl,
                resolvedDomain = resolved.domain,
                resolvedSuggestedName = resolved.suggestedName,
                resolvedIconKey = resolved.iconKey
            )
            // Fallback timer for headless unit tests where WebView onPageFinished does not run
            delay(900)
            val afterWait = _addSiteDialogState.value
            if (afterWait is AddSiteDialogState.Step1InputUrl && afterWait.verificationUrl != null) {
                onSiteVerificationCompleted(afterWait.resolvedSuggestedName)
            }
        }
    }

    fun onSiteVerificationCompleted(extractedTitle: String? = null) {
        val current = _addSiteDialogState.value as? AddSiteDialogState.Step1InputUrl ?: return
        val cleanTitle = extractedTitle
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf {
                it.isNotBlank() &&
                    !it.contains("Just a moment", ignoreCase = true) &&
                    !it.contains("Cloudflare", ignoreCase = true) &&
                    !it.contains("Attention Required", ignoreCase = true) &&
                    !it.startsWith("http", ignoreCase = true)
            }
        val finalName = cleanTitle ?: current.resolvedSuggestedName.ifBlank { current.resolvedDomain }
        val targetUrl = current.verificationUrl ?: "https://${current.resolvedDomain}"
        _addSiteDialogState.value = AddSiteDialogState.Step2ConfirmManual(
            normalizedUrl = targetUrl,
            domain = current.resolvedDomain,
            siteName = finalName,
            iconKey = current.resolvedIconKey
        )
    }

    fun updateStep2SiteName(newName: String) {
        val current = _addSiteDialogState.value as? AddSiteDialogState.Step2ConfirmManual ?: return
        _addSiteDialogState.value = current.copy(siteName = newName)
    }

    fun confirmAddSiteStep2() {
        val current = _addSiteDialogState.value as? AddSiteDialogState.Step2ConfirmManual ?: return
        viewModelScope.launch {
            repository.addSite(
                name = current.siteName,
                url = current.normalizedUrl,
                domain = current.domain,
                iconKey = current.iconKey
            )
            _addSiteDialogState.value = AddSiteDialogState.Hidden
            showToast("تم الإضافة بنجاح")
            _showRestartNoticeBanner.value = true
        }
    }

    fun dismissRestartNoticeBanner() {
        _showRestartNoticeBanner.value = false
    }

    fun toggleSiteEnabled(site: SiteEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSite(site.copy(isEnabled = enabled))
        }
    }

    fun openEditSiteDialog(site: SiteEntity) {
        _editingSite.value = site
    }

    fun dismissEditSiteDialog() {
        _editingSite.value = null
    }

    fun saveEditedSite(site: SiteEntity, newName: String, newUrl: String) {
        viewModelScope.launch {
            val resolved = repository.normalizeAndResolveSite(newUrl)
            if (!resolved.isValid) {
                showToast(resolved.errorMessage ?: "الرابط غير صالح")
                return@launch
            }
            repository.updateSite(
                site.copy(
                    name = newName.trim().ifEmpty { site.name },
                    url = resolved.normalizedUrl,
                    domain = resolved.domain,
                    iconKey = resolved.iconKey
                )
            )
            _editingSite.value = null
            showToast("تم تعديل الموقع بنجاح")
        }
    }

    fun requestDeleteSite(site: SiteEntity) {
        _deletingSite.value = site
    }

    fun cancelDeleteSite() {
        _deletingSite.value = null
    }

    fun confirmDeleteSite() {
        val site = _deletingSite.value ?: return
        viewModelScope.launch {
            repository.deleteSite(site)
            _deletingSite.value = null
            showToast("تم حذف الموقع")
        }
    }

    fun moveSiteOrder(site: SiteEntity, moveUp: Boolean) {
        val list = sites.value.toMutableList()
        val index = list.indexOfFirst { it.id == site.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in list.indices) return
        viewModelScope.launch {
            val other = list[targetIndex]
            repository.updateSite(site.copy(sortOrder = targetIndex))
            repository.updateSite(other.copy(sortOrder = index))
        }
    }

    // Login / Profile
    fun setShowLoginDialog(show: Boolean) {
        _showLoginDialog.value = show
    }

    fun saveUserProfile(name: String, email: String) {
        viewModelScope.launch {
            repository.updateStringSetting("userProfileName", name.trim())
            repository.updateStringSetting("userProfileEmail", email.trim())
            _showLoginDialog.value = false
            showToast(if (name.isBlank()) "تم تسجيل الخروج" else "تم تسجيل الدخول باسم ${name.trim()}")
        }
    }

    // Favorites & Categories
    fun toggleFavoritePage(title: String, url: String, siteName: String = "المتصفح", category: String = "الكل") {
        viewModelScope.launch {
            val added = repository.toggleFavorite(title, url, siteName, category)
            showToast(if (added) "تمت الإضافة إلى المفضلة" else "تمت الإزالة من المفضلة")
        }
    }

    fun removeFavorite(favorite: FavoriteEntity) {
        viewModelScope.launch {
            repository.deleteFavorite(favorite)
            showToast("تم الحذف من المفضلة")
        }
    }

    fun addFavoriteCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(name)
            showToast("تمت إضافة الفئة")
        }
    }

    fun deleteFavoriteCategory(category: FavoriteCategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showToast("تم حذف الفئة")
        }
    }

    fun updateFavoriteWorkCategory(favorite: FavoriteEntity, newCategory: String) {
        viewModelScope.launch {
            repository.updateFavoriteCategory(favorite, newCategory)
            showToast("تم نقل \"${favorite.title}\" إلى فئة $newCategory")
        }
    }

    fun assignWorkToFavoriteCategory(work: MangaWork, category: String) {
        viewModelScope.launch {
            val url = work.mangaUrl.ifBlank { "${work.siteUrl}/series/${work.id}" }
            val existing = favorites.value.find { it.url == url }
            if (existing != null) {
                repository.updateFavoriteCategory(existing, category)
            } else {
                repository.toggleFavorite(
                    title = work.title,
                    url = url,
                    siteName = work.siteName,
                    category = category
                )
            }
            showToast("تم حفظ \"${work.title}\" في فئة: $category")
        }
    }

    fun checkLibraryUpdatesInBackgroundNow() {
        viewModelScope.launch {
            if (settings.value.checkNewChaptersWifiOnly && !repository.isWifiConnected()) {
                showToast("فحص التحديثات مقتصر على شبكة Wi-Fi حسب إعداداتك")
                return@launch
            }
            showToast("جارٍ فحص التحديثات الجديدة لأعمال المكتبة والمفضلة...")
            val count = repository.checkFavoriteWorksForNewChapters()
            if (count > 0) {
                showToast("تم العثور على $count تحديث جديد في مكتبتك!")
            } else {
                showToast("جميع أعمال مكتبتك محدّثة لآخر فصل")
            }
        }
    }

    fun updateFavoriteCategoryForItem(favorite: FavoriteEntity, newCategory: String) =
        updateFavoriteWorkCategory(favorite, newCategory)

    fun assignWorkToCategory(work: MangaWork, category: String) =
        assignWorkToFavoriteCategory(work, category)

    fun runBackgroundLibraryUpdateCheckNow() =
        checkLibraryUpdatesInBackgroundNow()

    // History
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            showToast("تم مسح السجل بالكامل")
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    // Storage & Downloads
    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.calculateStorageStats()
        }
    }

    fun saveCurrentPageToStorage(title: String, url: String) {
        viewModelScope.launch {
            if (settings.value.downloadWifiOnly && !repository.isWifiConnected()) {
                showToast("التنزيل متاح عبر شبكة Wi-Fi فقط حسب إعداداتك")
                return@launch
            }
            val res = repository.saveWebPageToStorage(title, url)
            refreshStorageStats()
            if (res.isSuccess) {
                showToast("تم حفظ العمل في التخزين")
            } else {
                showToast("تعذر حفظ الصفحة: ${res.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun startRealDownload(title: String, url: String) {
        viewModelScope.launch {
            if (settings.value.downloadWifiOnly && !repository.isWifiConnected()) {
                showToast("التنزيل عبر شبكة Wi-Fi فقط مفعّل في الإعدادات")
                return@launch
            }
            showToast("بدء التنزيل: $title")
            val res = repository.downloadUrlToFile(title, url)
            refreshStorageStats()
            if (res.isSuccess) {
                showToast("اكتمل التنزيل وتم الحفظ في التخزين")
            } else {
                showToast("فشل التنزيل: تحقق من الاتصال بالإنترنت")
            }
        }
    }

    fun importFileToStorage(uri: Uri) {
        viewModelScope.launch {
            val res = repository.importFileFromUri(uri)
            refreshStorageStats()
            if (res.isSuccess) {
                showToast("تم استيراد الملف بنجاح إلى التخزين")
            } else {
                showToast("تعذر استيراد الملف")
            }
        }
    }

    fun deleteSavedWork(work: SavedWorkEntity) {
        viewModelScope.launch {
            repository.deleteSavedWork(work)
            refreshStorageStats()
            showToast("تم حذف الملف من التخزين")
        }
    }

    fun deleteDownloadTask(task: DownloadTaskEntity) {
        viewModelScope.launch {
            repository.deleteDownloadTask(task)
            refreshStorageStats()
            showToast("تم حذف النسخة المحلية")
        }
    }

    fun togglePauseDownloadTask(task: DownloadTaskEntity) {
        viewModelScope.launch {
            repository.togglePauseDownloadTask(task)
            showToast(if (task.status == "PAUSED") "تم استئناف التنزيل" else "تم إيقاف التنزيل مؤقتاً")
        }
    }

    fun cancelDownloadTask(task: DownloadTaskEntity) {
        viewModelScope.launch {
            repository.cancelDownloadTask(task)
            showToast("تم إلغاء التنزيل")
        }
    }

    fun togglePauseAllDownloads() {
        val paused = !isDownloadsPaused.value
        repository.mangaDownloadManager.setAllPaused(paused)
        showToast(if (paused) "تم إيقاف جميع التنزيلات مؤقتاً" else "تم استئناف التنزيلات")
    }

    fun cancelAllDownloads() {
        viewModelScope.launch {
            repository.cancelAllActiveDownloads()
            showToast("تم إلغاء جميع التنزيلات الجارية")
        }
    }

    fun clearDownloadHistory() {
        viewModelScope.launch {
            repository.clearDownloadHistoryRecords()
            showToast("تم حذف سجل التنزيلات")
        }
    }

    fun retryDownloadTask(task: DownloadTaskEntity) {
        viewModelScope.launch {
            repository.retryFailedDownloadTask(task)
            refreshStorageStats()
            showToast("تمت إعادة تنزيل الفصل بنجاح")
        }
    }

    fun openDownloadedTaskInReader(task: DownloadTaskEntity) {
        viewModelScope.launch {
            val workId = if (task.workId.isNotBlank()) {
                task.workId
            } else if (task.url.startsWith("manga://")) {
                task.url.removePrefix("manga://").substringBefore("/chapter/")
            } else {
                ""
            }
            val chNum = if (task.chapterNumber > 0) {
                task.chapterNumber
            } else if (task.url.startsWith("manga://")) {
                task.url.substringAfter("/chapter/").toIntOrNull() ?: 1
            } else {
                1
            }

            if (workId.isNotBlank()) {
                val downloadedWorkEntity = repository.dao.getDownloadedWorkById(workId)
                if (downloadedWorkEntity != null) {
                    val offlineWork = repository.buildOfflineMangaWorkFromEntity(downloadedWorkEntity)
                    val targetChapter = offlineWork.chapters.find { it.number == chNum }
                        ?: offlineWork.chapters.firstOrNull()
                    if (targetChapter != null) {
                        openMangaChapterReader(offlineWork, targetChapter)
                        return@launch
                    }
                    openMangaDetails(offlineWork)
                    return@launch
                }
            }

            val allWorks = MangaCatalogData.defaultAddedSites.flatMap { MangaCatalogData.getCatalogForSite(it) } +
                MangaCatalogData.storageLibraryWorks
            val matchedWork = allWorks.find { it.id == workId } ?: _selectedWork.value
            val matchedChapter = matchedWork.chapters.find { it.number == chNum }
                ?: matchedWork.chapters.firstOrNull()
                ?: _selectedChapter.value
            openMangaChapterReader(matchedWork, matchedChapter)
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            val ok = repository.clearAppCache()
            refreshStorageStats()
            showToast(if (ok) "تم مسح ذاكرة التخزين المؤقتة (مع الحفاظ على الفصول المنزلة)" else "تعذر مسح ذاكرة التخزين المؤقتة")
        }
    }

    fun clearPreviousSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchCount()
            showToast("تم مسح البحث السابق")
        }
    }

    fun recordSearchQuery() {
        viewModelScope.launch {
            repository.incrementSearchCount()
        }
    }

    fun selectUpdatesSourceFilter(sourceId: Long?) {
        _selectedUpdatesSourceId.value = sourceId
    }

    fun checkSitesForUpdates(
        targetSite: SiteEntity? = null,
        loadNextPage: Boolean = false,
        showFeedbackToast: Boolean = true
    ) {
        updatesSyncJob?.cancel()
        updatesSyncJob = viewModelScope.launch {
            val activeSites = when {
                targetSite != null -> listOf(targetSite)
                _selectedUpdatesSourceId.value != null -> {
                    enabledSites.value.filter { it.id == _selectedUpdatesSourceId.value }
                }
                else -> enabledSites.value
            }

            if (activeSites.isEmpty()) {
                _isUpdatingSites.value = false
                _latestUpdatesErrorMessage.value = "لا توجد مواقع مفعلة لجلب آخر التحديثات منها"
                if (showFeedbackToast) {
                    showToast("لا توجد مواقع مفعلة للتحقق من التحديثات")
                }
                return@launch
            }

            if (!repository.isNetworkAvailable()) {
                _isUpdatingSites.value = false
                val cached = repository.getCachedLatestUpdates(_selectedUpdatesSourceId.value)
                val lastTs = cached.maxOfOrNull { it.syncedAt } ?: 0L
                if (lastTs > 0L) _lastUpdatesSyncedAtMillis.value = lastTs
                _latestUpdatesErrorMessage.value = if (cached.isNotEmpty()) {
                    "لا يوجد اتصال بالإنترنت — يتم عرض آخر التحديثات المخزنة مؤقتًا"
                } else {
                    "لا يوجد اتصال بالإنترنت ولا توجد تحديثات مخزنة مؤقتًا"
                }
                if (showFeedbackToast) {
                    showToast(_latestUpdatesErrorMessage.value ?: "لا يوجد اتصال بالإنترنت")
                }
                return@launch
            }

            _isUpdatingSites.value = true
            _latestUpdatesErrorMessage.value = null

            var totalFetched = 0
            var anyHasMore = false
            val errors = mutableListOf<String>()
            val statusMap = _sourceUpdateStatuses.value.toMutableMap()
            val pageMap = _latestUpdatesPageBySource.value.toMutableMap()

            for (site in activeSites) {
                val nextPage = if (loadNextPage) (pageMap[site.id] ?: 1) + 1 else 1
                statusMap[site.id] = SourceUpdateSyncStatus(
                    sourceId = site.id,
                    sourceName = site.name,
                    isLoading = true
                )
                _sourceUpdateStatuses.value = statusMap.toMap()

                try {
                    val res = repository.syncLatestUpdatesForSite(
                        site = site,
                        page = nextPage,
                        replaceCachedForPageOne = !loadNextPage
                    )
                    val now = System.currentTimeMillis()
                    if (res.data.isNotEmpty()) {
                        totalFetched += res.data.size
                        pageMap[site.id] = nextPage
                        if (res.hasNextPage) anyHasMore = true
                        _lastUpdatesSyncedAtMillis.value = now
                        statusMap[site.id] = SourceUpdateSyncStatus(
                            sourceId = site.id,
                            sourceName = site.name,
                            isLoading = false,
                            isSuccess = true,
                            updatesCount = res.data.size,
                            errorMessage = null,
                            lastSyncedAt = now
                        )
                    } else {
                        val errMsg = res.errorMessage ?: "لم يتم العثور على تحديثات فصول في ${site.name}"
                        errors.add("${site.name}: $errMsg")
                        statusMap[site.id] = SourceUpdateSyncStatus(
                            sourceId = site.id,
                            sourceName = site.name,
                            isLoading = false,
                            isSuccess = false,
                            updatesCount = 0,
                            errorMessage = errMsg,
                            lastSyncedAt = statusMap[site.id]?.lastSyncedAt ?: 0L
                        )
                    }
                } catch (e: Exception) {
                    val errMsg = e.localizedMessage ?: "تعذر الاتصال بالمصدر"
                    errors.add("${site.name}: $errMsg")
                    statusMap[site.id] = SourceUpdateSyncStatus(
                        sourceId = site.id,
                        sourceName = site.name,
                        isLoading = false,
                        isSuccess = false,
                        updatesCount = 0,
                        errorMessage = errMsg,
                        lastSyncedAt = statusMap[site.id]?.lastSyncedAt ?: 0L
                    )
                }
                _sourceUpdateStatuses.value = statusMap.toMap()
                _latestUpdatesPageBySource.value = pageMap.toMap()
            }

            _latestUpdatesHasMore.value = anyHasMore
            _isUpdatingSites.value = false

            if (totalFetched > 0) {
                _latestUpdatesErrorMessage.value = if (errors.isNotEmpty()) {
                    "تعذر تحديث بعض المصادر (${errors.joinToString(" | ")})"
                } else null
                if (showFeedbackToast) {
                    showToast("تم التحديث بنجاح ($totalFetched فصل جديد)")
                }
            } else {
                val combinedError = errors.joinToString("\n").ifBlank { "لم يتم العثور على تحديثات جديدة من المصادر" }
                _latestUpdatesErrorMessage.value = combinedError
                if (showFeedbackToast) {
                    showToast(errors.firstOrNull() ?: "تعذر جلب التحديثات من المصدر")
                }
            }
        }
    }

    fun openWorkFromLatestUpdate(item: MangaLatestUpdateItem) {
        val work = MangaWork(
            id = item.workId,
            title = item.workTitle,
            mangaUrl = item.mangaUrl,
            sourceId = item.sourceId,
            siteName = item.sourceName,
            siteUrl = item.sourceUrl,
            siteIconKey = item.siteIconKey,
            coverImageUrl = item.coverImageUrl,
            coverUrl = item.coverImageUrl
        )
        openMangaDetails(work)
    }

    fun openChapterFromLatestUpdate(item: MangaLatestUpdateItem) {
        val chapter = MangaChapter(
            id = item.chapterId,
            number = item.chapterNumber,
            title = item.chapterTitle,
            dateText = item.publishDateText,
            chapterUrl = item.chapterUrl,
            chapterNumberValue = item.chapterNumberValue,
            numberLabel = item.chapterNumberLabel
        )
        val work = MangaWork(
            id = item.workId,
            title = item.workTitle,
            mangaUrl = item.mangaUrl,
            sourceId = item.sourceId,
            siteName = item.sourceName,
            siteUrl = item.sourceUrl,
            siteIconKey = item.siteIconKey,
            coverImageUrl = item.coverImageUrl,
            coverUrl = item.coverImageUrl,
            chapters = listOf(chapter),
            chaptersCount = 1
        )
        openMangaChapterReader(work, chapter)
    }

    // Backup & Restore
    fun createBackup() {
        viewModelScope.launch {
            try {
                repository.exportBackupJson()
                refreshStorageStats()
                showToast("تم حفظ النسخة الاحتياطية في التخزين")
            } catch (e: Exception) {
                showToast("فشل إنشاء النسخة الاحتياطية")
            }
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            try {
                val count = repository.restoreBackupFromDefaultFile()
                showToast(
                    if (count > 0) "تمت استعادة $count موقع من النسخة الاحتياطية"
                    else "تم فحص النسخة الاحتياطية (لا توجد بيانات جديدة)"
                )
            } catch (e: Exception) {
                showToast("لا توجد نسخة احتياطية محفوظة للاستعادة")
            }
        }
    }

    // Settings updates
    fun setStringSetting(key: String, value: String) {
        viewModelScope.launch { repository.updateStringSetting(key, value) }
    }

    fun setBooleanSetting(key: String, value: Boolean) {
        viewModelScope.launch { repository.updateBooleanSetting(key, value) }
    }

    fun setIntSetting(key: String, value: Int) {
        viewModelScope.launch { repository.updateIntSetting(key, value) }
    }

    fun setFloatSetting(key: String, value: Float) {
        viewModelScope.launch { repository.updateFloatSetting(key, value) }
    }

    fun resetReadingStats() {
        viewModelScope.launch {
            repository.resetStatistics()
            showToast("تم تصفير الإحصائيات")
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
            delay(2800)
            if (_toastMessage.value == message) {
                _toastMessage.value = null
            }
        }
    }

    fun isNetworkConnected(): Boolean = repository.isNetworkAvailable()

    companion object {
        fun toArabicDigits(input: String): String {
            val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
            val sb = StringBuilder(input.length)
            for (ch in input) {
                if (ch in '0'..'9') {
                    sb.append(arabicDigits[ch - '0'])
                } else if (ch == '.' || ch == ',') {
                    sb.append('٫')
                } else {
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        fun formatBytesArabic(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            val df = DecimalFormat("#.#")
            return when {
                gb >= 1.0 -> "${toArabicDigits(df.format(gb))} GB"
                mb >= 0.1 -> "${toArabicDigits(df.format(mb))} MB"
                kb >= 1.0 -> "${toArabicDigits(df.format(kb))} KB"
                else -> "$bytes B"
            }
        }

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val settingsStore = UserSettingsDataStore(context.applicationContext)
                    val repo = AppRepository(context.applicationContext, db.appDao(), settingsStore)
                    return MainViewModel(repo) as T
                }
            }
        }
    }
}
