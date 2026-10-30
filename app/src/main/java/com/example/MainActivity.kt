package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MangaCatalogData
import com.example.data.MangaChapter
import com.example.data.MangaWork
import com.example.data.download.DownloadNotificationHelper
import com.example.ui.components.AddSiteFlowDialog
import com.example.ui.components.AppNavigationDrawerContent
import com.example.ui.components.BottomToastAndNoticeOverlay
import com.example.ui.components.DeleteSiteConfirmDialog
import com.example.ui.components.EditSiteDialog
import com.example.ui.components.LoginProfileDialog
import com.example.ui.screens.AppearanceSettingsScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FavoriteCategoriesSettingsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.GeneralSettingsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ManageSitesScreen
import com.example.ui.screens.MangaChapterReaderScreen
import com.example.ui.screens.MangaDetailsScreen
import com.example.ui.screens.MoreSettingsScreen
import com.example.ui.screens.NewChaptersSettingsScreen
import com.example.ui.screens.ReadingOptionsSettingsScreen
import com.example.ui.screens.RecommendationsScreen
import com.example.ui.screens.SettingsMainScreen
import com.example.ui.screens.SiteCatalogScreen
import com.example.ui.screens.SourceWebView
import com.example.ui.screens.StatisticsSettingsScreen
import com.example.ui.screens.StorageLibraryScreen
import com.example.ui.screens.StorageSettingsScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var pendingNotificationIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNotificationIntent = intent
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(context)
            )
            val userSettings by mainViewModel.settings.collectAsStateWithLifecycle()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(pendingNotificationIntent) {
                val currentIntent = pendingNotificationIntent ?: return@LaunchedEffect
                val openWorkId = currentIntent.getStringExtra(DownloadNotificationHelper.EXTRA_OPEN_WORK_ID)
                val openChapterNum = currentIntent.getIntExtra(DownloadNotificationHelper.EXTRA_OPEN_CHAPTER_NUMBER, -1)
                if (!openWorkId.isNullOrBlank() && openChapterNum > 0) {
                    mainViewModel.openChapterFromNotification(openWorkId, openChapterNum)
                    currentIntent.removeExtra(DownloadNotificationHelper.EXTRA_OPEN_WORK_ID)
                    currentIntent.removeExtra(DownloadNotificationHelper.EXTRA_OPEN_CHAPTER_NUMBER)
                }
                pendingNotificationIntent = null
            }

            MyApplicationTheme(
                appThemeKey = userSettings.appTheme,
                pureDarkMode = userSettings.pureDarkMode
            ) {
                MangaSitesReaderApp(viewModel = mainViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationIntent = intent
    }
}

@Composable
fun MangaSitesReaderApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val enabledSites by viewModel.enabledSites.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val savedWorks by viewModel.savedWorks.collectAsStateWithLifecycle()
    val downloadedWorks by viewModel.downloadedWorks.collectAsStateWithLifecycle()
    val downloadTasks by viewModel.downloadTasks.collectAsStateWithLifecycle()
    val isDownloadsPaused by viewModel.isDownloadsPaused.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val addSiteDialogState by viewModel.addSiteDialogState.collectAsStateWithLifecycle()
    val editingSite by viewModel.editingSite.collectAsStateWithLifecycle()
    val deletingSite by viewModel.deletingSite.collectAsStateWithLifecycle()
    val showLoginDialog by viewModel.showLoginDialog.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val showRestartBanner by viewModel.showRestartNoticeBanner.collectAsStateWithLifecycle()
    val browserUrl by viewModel.browserUrl.collectAsStateWithLifecycle()
    val browserTitle by viewModel.browserTitle.collectAsStateWithLifecycle()
    val isUpdatingSites by viewModel.isUpdatingSites.collectAsStateWithLifecycle()
    val selectedSite by viewModel.selectedSite.collectAsStateWithLifecycle()
    val selectedWork by viewModel.selectedWork.collectAsStateWithLifecycle()
    val selectedChapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val downloadedChaptersMap by viewModel.downloadedChaptersMap.collectAsStateWithLifecycle()
    val chapterDownloadStatesMap by viewModel.chapterDownloadStatesMap.collectAsStateWithLifecycle()
    val diagnosticLogs by viewModel.diagnosticLogs.collectAsStateWithLifecycle()
    val readChaptersMap by viewModel.readChaptersMap.collectAsStateWithLifecycle()
    val readingProgressMap by viewModel.readingProgressMap.collectAsStateWithLifecycle()
    val latestUpdates by viewModel.latestUpdates.collectAsStateWithLifecycle()
    val selectedUpdatesSourceId by viewModel.selectedUpdatesSourceId.collectAsStateWithLifecycle()
    val sourceUpdateStatuses by viewModel.sourceUpdateStatuses.collectAsStateWithLifecycle()
    val latestUpdatesHasMore by viewModel.latestUpdatesHasMore.collectAsStateWithLifecycle()
    val latestUpdatesErrorMessage by viewModel.latestUpdatesErrorMessage.collectAsStateWithLifecycle()
    val lastUpdatesSyncedAtMillis by viewModel.lastUpdatesSyncedAtMillis.collectAsStateWithLifecycle()

    // Start with the drawer open on first launch so it matches 00:00 of the reference video
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val scope = rememberCoroutineScope()

    val isDrawerGestureEnabled = currentScreen in setOf(
        AppScreen.STORAGE,
        AppScreen.SITE_CATALOG,
        AppScreen.RECOMMENDATIONS,
        AppScreen.UPDATES,
        AppScreen.FAVORITES,
        AppScreen.HISTORY,
        AppScreen.MANAGE_SITES
    )

    BackHandler(enabled = drawerState.isOpen || currentScreen != AppScreen.STORAGE) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.navigateBack()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isDrawerGestureEnabled,
        drawerContent = {
            AppNavigationDrawerContent(
                currentScreen = currentScreen,
                enabledSites = enabledSites,
                userProfileName = settings.userProfileName,
                userProfileEmail = settings.userProfileEmail,
                selectedSiteId = selectedSite.id,
                showSiteIcons = settings.showSiteIcons,
                onSelectScreen = { target ->
                    scope.launch { drawerState.close() }
                    viewModel.navigateTo(target)
                },
                onOpenSiteInBrowser = { site ->
                    scope.launch { drawerState.close() }
                    viewModel.openSiteCatalog(site)
                },
                onLoginClick = {
                    viewModel.setShowLoginDialog(true)
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                AppScreen.STORAGE -> {
                    StorageLibraryScreen(
                        savedWorks = savedWorks,
                        downloadedWorks = downloadedWorks,
                        showContinueReadingButton = settings.showContinueReadingButton,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onImportUri = { uri -> viewModel.importFileToStorage(uri) },
                        onOpenWork = { work ->
                            if (work.sourceUrl.startsWith("http")) {
                                viewModel.openBrowserWithUrl(work.sourceUrl, work.title)
                            } else {
                                viewModel.showToast("الملف محفوظ في: ${work.localFilePath}")
                            }
                        },
                        onSelectMangaWork = { mangaWork ->
                            viewModel.openMangaDetails(mangaWork)
                        },
                        onOpenDownloadedWork = { entity ->
                            viewModel.openDownloadedWorkFromStorage(entity)
                        },
                        onDeleteDownloadedWork = { workId ->
                            viewModel.deleteDownloadedWorkFromStorage(workId)
                        },
                        onDeleteWork = { work -> viewModel.deleteSavedWork(work) },
                        onContinueReadingClick = {
                            viewModel.resumeReadingCurrentOrLastWork()
                        },
                        onOpenDownloadsScreen = { viewModel.navigateTo(AppScreen.DOWNLOADS) },
                        onOpenStorageSettings = { viewModel.navigateTo(AppScreen.SETTINGS_STORAGE) }
                    )
                }

                AppScreen.SITE_CATALOG -> {
                    val catalogWorks = MangaCatalogData.getCatalogForSite(selectedSite)
                    val siteCachedUpdates = latestUpdates.filter { it.sourceId == selectedSite.id }
                    SiteCatalogScreen(
                        site = selectedSite,
                        works = catalogWorks,
                        cachedSiteUpdates = siteCachedUpdates,
                        showContinueReadingButton = settings.showContinueReadingButton,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onSelectWork = { work ->
                            viewModel.openMangaDetails(work)
                        },
                        onOpenUpdateWorkDetails = { updateItem ->
                            viewModel.openWorkFromLatestUpdate(updateItem)
                        },
                        onOpenUpdateChapter = { updateItem ->
                            viewModel.openChapterFromLatestUpdate(updateItem)
                        },
                        onCacheSiteUpdates = { items ->
                            scope.launch {
                                viewModel.repository.cacheLatestUpdatesDirectly(selectedSite.id, items)
                            }
                        },
                        onContinueReadingClick = {
                            viewModel.resumeReadingCurrentOrLastWork()
                        },
                        onOpenSiteInWebView = { site ->
                            viewModel.openBrowserWithUrl(site.url, site.name)
                        },
                        onShowToast = { msg -> viewModel.showToast(msg) },
                        onSaveWholeMangaToStorage = { work ->
                            viewModel.saveWholeMangaFromCatalog(work)
                        },
                        onOpenDownloadsScreen = {
                            viewModel.navigateTo(AppScreen.DOWNLOADS)
                        },
                        onOpenSettingsScreen = {
                            viewModel.navigateTo(AppScreen.SETTINGS_MAIN)
                        }
                    )
                }

                AppScreen.MANGA_DETAILS -> {
                    val isWorkFav = favorites.any {
                        it.title.equals(selectedWork.title, ignoreCase = true) ||
                            it.url.contains(selectedWork.id, ignoreCase = true)
                    }
                    val downloadedChaptersForWork = downloadedChaptersMap[selectedWork.id] ?: emptySet()
                    val chapterStatesForWork = chapterDownloadStatesMap[selectedWork.id] ?: emptyMap()
                    val readChaptersForWork = readChaptersMap[selectedWork.id] ?: emptySet()
                    val resumeLabel = viewModel.getFormattedResumeLabel(selectedWork.id)

                    MangaDetailsScreen(
                        work = selectedWork,
                        isFavorite = isWorkFav,
                        downloadedChapterNumbers = downloadedChaptersForWork,
                        chapterDownloadStates = chapterStatesForWork,
                        readChapterNumbers = readChaptersForWork,
                        resumeLabel = resumeLabel,
                        addedSites = enabledSites,
                        onBackClick = { viewModel.navigateBack() },
                        onOpenChapter = { work, chapter ->
                            viewModel.openMangaChapterReader(work, chapter)
                        },
                        onToggleFavorite = { work ->
                            viewModel.toggleFavoritePage(
                                title = work.title,
                                url = work.mangaUrl.ifBlank { "${work.siteUrl}/series/${work.id}" },
                                siteName = work.siteName
                            )
                        },
                        onSaveWorkToStorage = { work ->
                            viewModel.saveMangaWorkToStorage(work)
                        },
                        onDownloadChapters = { work, chaptersToDownload, toastMsg ->
                            viewModel.downloadChaptersForWork(work, chaptersToDownload, toastMsg)
                        },
                        onDeleteSingleDownloadedChapter = { work, chapter ->
                            viewModel.deleteSingleDownloadedChapter(work, chapter)
                        },
                        onRemoveWorkFromStorage = { workId ->
                            viewModel.deleteDownloadedWorkFromStorage(workId)
                        },
                        onOpenInWebView = { url, title ->
                            viewModel.openBrowserWithUrl(url, title)
                        },
                        onSelectSiteForSearch = { site ->
                            viewModel.openSiteCatalog(site)
                        },
                        onShowToast = { msg -> viewModel.showToast(msg) }
                    )
                }

                AppScreen.MANGA_READER -> {
                    val isWorkFav = favorites.any {
                        it.title.equals(selectedWork.title, ignoreCase = true) ||
                            it.url.contains(selectedWork.id, ignoreCase = true)
                    }
                    val savedPageIndex = readingProgressMap[selectedWork.id]
                        ?.takeIf { it.first == selectedChapter.number }
                        ?.second
                        ?: selectedChapter.lastReadPage.takeIf { it >= 0 }
                        ?: 0
                    val liveChapterDownloadState = chapterDownloadStatesMap[selectedWork.id]?.get(selectedChapter.number)

                    MangaChapterReaderScreen(
                        work = selectedWork,
                        chapter = selectedChapter,
                        settings = settings,
                        isFavorite = isWorkFav,
                        initialPageIndex = savedPageIndex,
                        liveChapterDownloadState = liveChapterDownloadState,
                        diagnosticLogs = diagnosticLogs,
                        onClearDiagnosticLogs = { viewModel.clearDiagnosticLogs() },
                        onSaveReadingProgress = { pageIdx ->
                            viewModel.saveReadingProgress(selectedWork, selectedChapter, pageIdx)
                        },
                        onBackClick = { viewModel.navigateBack() },
                        onSelectChapter = { targetChapter ->
                            viewModel.openMangaChapterReader(selectedWork, targetChapter)
                        },
                        onToggleFavorite = { work ->
                            viewModel.toggleFavoritePage(
                                title = work.title,
                                url = work.mangaUrl.ifBlank { "${work.siteUrl}/series/${work.id}" },
                                siteName = work.siteName
                            )
                        },
                        onDownloadChapter = { work, chapter ->
                            viewModel.downloadChaptersForWork(
                                work = work,
                                chapters = listOf(chapter),
                                toastMsg = "بدأ تنزيل الفصل ${chapter.number}..."
                            )
                        },
                        onUpdateWebtoonMode = { enabled ->
                            viewModel.setBooleanSetting("webtoonAlwaysOn", enabled)
                        },
                        onOpenReadingSettings = {
                            viewModel.navigateTo(AppScreen.SETTINGS_READING_OPTIONS)
                        },
                        onShowToast = { msg -> viewModel.showToast(msg) }
                    )
                }

                AppScreen.MANAGE_SITES -> {
                    ManageSitesScreen(
                        sites = sites,
                        onBackClick = {
                            if (!viewModel.navigateBack()) {
                                scope.launch { drawerState.open() }
                            }
                        },
                        onAddSiteClick = { viewModel.openAddSiteDialog() },
                        onOpenBrowserClick = {
                            viewModel.openBrowserWithUrl("https://www.google.com", "Google")
                        },
                        onOpenSiteInBrowser = { site ->
                            viewModel.openSiteCatalog(site)
                        },
                        onToggleSiteEnabled = { site, enabled ->
                            viewModel.toggleSiteEnabled(site, enabled)
                        },
                        onEditSite = { site -> viewModel.openEditSiteDialog(site) },
                        onDeleteSite = { site -> viewModel.requestDeleteSite(site) },
                        onMoveSite = { site, up -> viewModel.moveSiteOrder(site, up) },
                        onTestConnection = { site -> viewModel.testSiteConnection(site) },
                        onOpenLastVisitedPage = { site -> viewModel.openSiteLastVisitedPage(site) }
                    )
                }

                AppScreen.BROWSER -> {
                    SourceWebView(
                        initialUrl = browserUrl,
                        currentTitle = browserTitle,
                        coachmarkAlreadyShown = settings.browserCoachmarkShown,
                        onUpdatePageState = { url, title ->
                            viewModel.updateBrowserPageState(url, title)
                        },
                        onRecordVisit = { title, url ->
                            viewModel.recordPageVisit(title, url)
                        },
                        onMarkCoachmarkShown = {
                            viewModel.setBooleanSetting("browserCoachmarkShown", true)
                        },
                        onAddSiteFromBrowser = { currentUrl ->
                            viewModel.openAddSiteDialog(prefillUrl = currentUrl)
                        },
                        onToggleFavorite = { title, url ->
                            viewModel.toggleFavoritePage(title, url)
                        },
                        onSavePageToStorage = { title, url ->
                            viewModel.saveCurrentPageToStorage(title, url)
                        },
                        onOpenCurrentUrlInReader = { title, url ->
                            val adHocWork = MangaWork(
                                id = "web_${url.hashCode()}",
                                title = title.ifBlank { "فصل من المتصفح" },
                                siteName = selectedSite.name.ifBlank { "المتصفح" },
                                siteUrl = selectedSite.url.ifBlank { url },
                                siteIconKey = selectedSite.iconKey,
                                mangaUrl = url
                            )
                            val adHocChapter = MangaChapter(
                                id = "web_ch_${url.hashCode()}",
                                number = 1,
                                title = title.ifBlank { "الفصل 1" },
                                dateText = "الآن",
                                chapterUrl = url
                            )
                            viewModel.openMangaChapterReader(adHocWork, adHocChapter)
                        },
                        onShowToast = { msg -> viewModel.showToast(msg) },
                        onRecordSearch = { viewModel.recordSearchQuery() },
                        onCloseBrowser = { viewModel.navigateBack() }
                    )
                }

                AppScreen.RECOMMENDATIONS -> {
                    RecommendationsScreen(
                        enabledSites = enabledSites,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenSite = { site -> viewModel.openSiteCatalog(site) },
                        onOpenManageSites = { viewModel.navigateTo(AppScreen.MANAGE_SITES) }
                    )
                }

                AppScreen.UPDATES -> {
                    val targetSiteForFilter = enabledSites.find { it.id == selectedUpdatesSourceId }
                    UpdatesScreen(
                        enabledSites = enabledSites,
                        latestUpdates = latestUpdates,
                        selectedSourceId = selectedUpdatesSourceId,
                        sourceStatuses = sourceUpdateStatuses,
                        hasMorePages = latestUpdatesHasMore,
                        errorMessage = latestUpdatesErrorMessage,
                        lastSyncedAtMillis = lastUpdatesSyncedAtMillis,
                        isChecking = isUpdatingSites,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onSelectSourceFilter = { srcId ->
                            viewModel.selectUpdatesSourceFilter(srcId)
                        },
                        onRefreshUpdates = {
                            viewModel.checkSitesForUpdates(
                                targetSite = targetSiteForFilter,
                                loadNextPage = false,
                                showFeedbackToast = true
                            )
                        },
                        onLoadMoreUpdates = {
                            viewModel.checkSitesForUpdates(
                                targetSite = targetSiteForFilter,
                                loadNextPage = true,
                                showFeedbackToast = false
                            )
                        },
                        onOpenUpdateWorkDetails = { item ->
                            viewModel.openWorkFromLatestUpdate(item)
                        },
                        onOpenUpdateChapter = { item ->
                            viewModel.openChapterFromLatestUpdate(item)
                        },
                        onOpenSite = { site -> viewModel.openSiteCatalog(site) }
                    )
                }

                AppScreen.FAVORITES -> {
                    FavoritesScreen(
                        favorites = favorites,
                        categories = categories,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenFavoriteUrl = { _, _ ->
                            viewModel.openMangaDetails(selectedWork)
                        },
                        onRemoveFavorite = { fav -> viewModel.removeFavorite(fav) },
                        onManageCategories = { viewModel.navigateTo(AppScreen.SETTINGS_FAVORITE_CATEGORIES) }
                    )
                }

                AppScreen.HISTORY -> {
                    HistoryScreen(
                        historyItems = history,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenHistoryUrl = { _, _ ->
                            viewModel.openMangaDetails(selectedWork)
                        },
                        onDeleteHistoryItem = { id -> viewModel.deleteHistoryEntry(id) },
                        onClearAllHistory = { viewModel.clearAllHistory() }
                    )
                }

                AppScreen.DOWNLOADS -> {
                    DownloadsScreen(
                        tasks = downloadTasks,
                        isAllPaused = isDownloadsPaused,
                        onBackClick = { viewModel.navigateBack() },
                        onStartNewDownload = { title, url -> viewModel.startRealDownload(title, url) },
                        onDeleteTask = { task -> viewModel.deleteDownloadTask(task) },
                        onTogglePauseTask = { task -> viewModel.togglePauseDownloadTask(task) },
                        onCancelTask = { task -> viewModel.cancelDownloadTask(task) },
                        onRetryTask = { task -> viewModel.retryDownloadTask(task) },
                        onOpenDownloadedTask = { task -> viewModel.openDownloadedTaskInReader(task) },
                        onTogglePauseAll = { viewModel.togglePauseAllDownloads() },
                        onCancelAll = { viewModel.cancelAllDownloads() },
                        onClearHistory = { viewModel.clearDownloadHistory() }
                    )
                }

                AppScreen.SETTINGS_MAIN -> {
                    SettingsMainScreen(
                        onBackClick = {
                            if (!viewModel.navigateBack()) {
                                scope.launch { drawerState.open() }
                            }
                        },
                        onNavigateSubScreen = { sub -> viewModel.navigateTo(sub) },
                        onShowToast = { msg -> viewModel.showToast(msg) }
                    )
                }

                AppScreen.SETTINGS_GENERAL -> {
                    GeneralSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onUpdateStartScreen = { valStr -> viewModel.setStringSetting("startScreen", valStr) },
                        onUpdateRecNotifications = { enabled ->
                            viewModel.setBooleanSetting("recommendationNotifications", enabled)
                        }
                    )
                }

                AppScreen.SETTINGS_APPEARANCE -> {
                    AppearanceSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onSelectTheme = { themeKey -> viewModel.setStringSetting("appTheme", themeKey) },
                        onSelectNightMode = { mode -> viewModel.setStringSetting("nightMode", mode) },
                        onToggleBoolean = { key, value -> viewModel.setBooleanSetting(key, value) }
                    )
                }

                AppScreen.SETTINGS_FAVORITE_CATEGORIES -> {
                    FavoriteCategoriesSettingsScreen(
                        categories = categories,
                        onBackClick = { viewModel.navigateBack() },
                        onAddCategory = { name -> viewModel.addFavoriteCategory(name) },
                        onDeleteCategory = { cat -> viewModel.deleteFavoriteCategory(cat) }
                    )
                }

                AppScreen.SETTINGS_STORAGE -> {
                    StorageSettingsScreen(
                        stats = storageStats,
                        searchHistoryCount = settings.searchHistoryCount,
                        onBackClick = { viewModel.navigateBack() },
                        onClearCache = { viewModel.clearAppCache() },
                        onClearSearchHistory = { viewModel.clearPreviousSearchHistory() },
                        onShowToast = { msg -> viewModel.showToast(msg) }
                    )
                }

                AppScreen.SETTINGS_READING_OPTIONS -> {
                    ReadingOptionsSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onUpdateString = { k, v -> viewModel.setStringSetting(k, v) },
                        onUpdateBoolean = { k, v -> viewModel.setBooleanSetting(k, v) },
                        onUpdateInt = { k, v -> viewModel.setIntSetting(k, v) },
                        onUpdateFloat = { k, v -> viewModel.setFloatSetting(k, v) }
                    )
                }

                AppScreen.SETTINGS_NEW_CHAPTERS -> {
                    NewChaptersSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onUpdateBoolean = { k, v -> viewModel.setBooleanSetting(k, v) }
                    )
                }

                AppScreen.SETTINGS_STATISTICS -> {
                    StatisticsSettingsScreen(
                        totalReadingSeconds = settings.totalReadingSeconds,
                        sitesCount = sites.size,
                        historyCount = history.size,
                        favoritesCount = favorites.size,
                        onBackClick = { viewModel.navigateBack() },
                        onResetStats = { viewModel.resetReadingStats() }
                    )
                }

                AppScreen.SETTINGS_MORE -> {
                    MoreSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onUpdateInt = { k, v -> viewModel.setIntSetting(k, v) },
                        onUpdateBoolean = { k, v -> viewModel.setBooleanSetting(k, v) },
                        onCreateBackup = { viewModel.createBackup() },
                        onRestoreBackup = { viewModel.restoreBackup() },
                        onShowToast = { msg -> viewModel.showToast(msg) }
                    )
                }
            }

            // Global Add Site 2-Step Dialog
            AddSiteFlowDialog(
                state = addSiteDialogState,
                onUrlChange = { viewModel.updateAddSiteUrlInput(it) },
                onStep1Submit = { viewModel.submitAddSiteStep1() },
                onStep2NameChange = { viewModel.updateStep2SiteName(it) },
                onStep2Confirm = { viewModel.confirmAddSiteStep2() },
                onDismiss = { viewModel.dismissAddSiteDialog() }
            )

            // Global Edit & Delete Site Dialogs
            EditSiteDialog(
                site = editingSite,
                onSave = { site, newName, newUrl -> viewModel.saveEditedSite(site, newName, newUrl) },
                onDismiss = { viewModel.dismissEditSiteDialog() }
            )

            DeleteSiteConfirmDialog(
                site = deletingSite,
                onConfirm = { viewModel.confirmDeleteSite() },
                onDismiss = { viewModel.cancelDeleteSite() }
            )

            // Global Login / Profile Dialog
            LoginProfileDialog(
                visible = showLoginDialog,
                currentName = settings.userProfileName,
                currentEmail = settings.userProfileEmail,
                onSave = { name, email -> viewModel.saveUserProfile(name, email) },
                onDismiss = { viewModel.setShowLoginDialog(false) }
            )

            // Global Bottom Pill Toast and Restart Notice Banner
            BottomToastAndNoticeOverlay(
                toastMessage = toastMessage,
                showRestartBanner = showRestartBanner && currentScreen != AppScreen.BROWSER,
                onDismissRestartBanner = { viewModel.dismissRestartNoticeBanner() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
