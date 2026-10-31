package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiagnosticLevel
import com.example.data.DiagnosticLogEntry
import com.example.data.DiagnosticLogger
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaChapter
import com.example.data.MangaWork
import com.example.data.local.DownloadedChapterEntity
import com.example.data.preferences.UserSettings
import com.example.ui.components.ChapterPageIllustration
import com.example.ui.components.HiddenSiteWebViewBridge
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaChapterReaderScreen(
    work: MangaWork,
    chapter: MangaChapter,
    settings: UserSettings,
    isFavorite: Boolean,
    initialPageIndex: Int = 0,
    liveChapterDownloadState: DownloadedChapterEntity? = null,
    diagnosticLogs: List<DiagnosticLogEntry> = emptyList(),
    onClearDiagnosticLogs: () -> Unit = {},
    onSaveReadingProgress: (Int) -> Unit = {},
    onBackClick: () -> Unit,
    onSelectChapter: (MangaChapter) -> Unit,
    onToggleFavorite: (MangaWork) -> Unit,
    onDownloadChapter: (MangaWork, MangaChapter) -> Unit,
    onExportChapterPdf: (MangaWork, MangaChapter) -> Unit = { _, _ -> },
    onExportChapterCbz: (MangaWork, MangaChapter) -> Unit = { _, _ -> },
    onUpdateWebtoonMode: (Boolean) -> Unit,
    onUpdateAutoCropBorders: (Boolean) -> Unit = {},
    onUpdateColorFilter: (String) -> Unit = {},
    onUpdateAutoScroll: (Boolean) -> Unit = {},
    onUpdateAutoScrollSpeed: (Float) -> Unit = {},
    onOpenReadingSettings: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    val effectiveDownloadStatus = liveChapterDownloadState?.status?.ifBlank { chapter.downloadStatus }
        ?: chapter.downloadStatus

    val isOfflineComplete = remember(chapter.id, chapter.localPagePaths, effectiveDownloadStatus) {
        chapter.localPagePaths.isNotEmpty() && effectiveDownloadStatus == "COMPLETED"
    }
    val isOfflineIncomplete = remember(chapter.id, chapter.localPagePaths, effectiveDownloadStatus) {
        effectiveDownloadStatus == "INCOMPLETE" ||
            (chapter.localPagePaths.isNotEmpty() && effectiveDownloadStatus != "COMPLETED")
    }
    val isActivelyDownloading = effectiveDownloadStatus == "DOWNLOADING" || effectiveDownloadStatus == "QUEUED"

    var localPagePaths by remember(chapter.id, chapter.localPagePaths) {
        mutableStateOf(chapter.localPagePaths)
    }
    var livePageUrls by remember(chapter.id, chapter.chapterUrl, chapter.pageImageUrls) {
        mutableStateOf(chapter.pageImageUrls)
    }
    var loadingProgress by remember(chapter.id, isOfflineComplete) {
        mutableIntStateOf(
            if (isOfflineComplete || chapter.localPagePaths.isNotEmpty() || chapter.pageImageUrls.isNotEmpty()) 100 else 5
        )
    }
    var isChapterLoaded by remember(chapter.id, isOfflineComplete) {
        mutableStateOf(
            isOfflineComplete || chapter.localPagePaths.isNotEmpty() || chapter.pageImageUrls.isNotEmpty()
        )
    }
    var requiresCloudflare by remember(chapter.id, chapter.chapterUrl) { mutableStateOf(false) }
    var lastScrapeError by remember(chapter.id) { mutableStateOf<String?>(null) }
    var showProtectionModal by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val targetChapterUrl = remember(chapter.chapterUrl, work.siteUrl, work.id, chapter.number) {
        chapter.chapterUrl.ifBlank {
            "${work.siteUrl.trimEnd('/')}/series/${work.id}/${chapter.number}"
        }
    }

    // Only fetch from network if NOT reading a completed local chapter (100% offline support in Airplane mode!)
    LaunchedEffect(chapter.id, targetChapterUrl, isOfflineComplete, refreshTrigger) {
        if (isOfflineComplete && refreshTrigger == 0) {
            loadingProgress = 100
            isChapterLoaded = true
            return@LaunchedEffect
        }
        if ((livePageUrls.isEmpty() && localPagePaths.isEmpty()) || refreshTrigger > 0) {
            if (localPagePaths.isEmpty()) {
                isChapterLoaded = false
                loadingProgress = 12
            }
            val res = LiveMangaSiteScraper.fetchChapterPages(work, chapter.copy(chapterUrl = targetChapterUrl))
            loadingProgress = 68
            if (res.pageUrls.isNotEmpty()) {
                // Keep whichever list has more pages so we never regress to 1 page
                if (res.pageUrls.size >= livePageUrls.size) {
                    livePageUrls = res.pageUrls
                }
                requiresCloudflare = false
                lastScrapeError = null
                loadingProgress = 100
                isChapterLoaded = true
            } else if (res.requiresCloudflare) {
                requiresCloudflare = true
                lastScrapeError = res.errorMessage
                loadingProgress = 97
                isChapterLoaded = true
            } else {
                lastScrapeError = res.errorMessage
                loadingProgress = 97
                delay(250)
                isChapterLoaded = true
            }
        }
    }

    // Hidden WebView DOM Bridge only when pages were not already resolved by OkHttp/cache
    if ((!isOfflineComplete && targetChapterUrl.startsWith("http") && livePageUrls.isEmpty() && localPagePaths.isEmpty()) || refreshTrigger > 0) {
        HiddenSiteWebViewBridge(
            targetUrl = targetChapterUrl,
            jsExtractorScript = LiveMangaSiteScraper.JS_EXTRACT_CHAPTER_PAGES,
            reloadTrigger = refreshTrigger,
            onResultJson = { rawJson ->
                val domRes = LiveMangaSiteScraper.parseChapterPagesFromDomJson(rawJson, targetChapterUrl)
                if (domRes.pageUrls.isNotEmpty()) {
                    if (domRes.pageUrls.size > livePageUrls.size) {
                        livePageUrls = domRes.pageUrls
                    }
                    requiresCloudflare = false
                    lastScrapeError = null
                    loadingProgress = 100
                    isChapterLoaded = true
                } else if (domRes.requiresCloudflare && livePageUrls.isEmpty() && localPagePaths.isEmpty()) {
                    requiresCloudflare = true
                    isChapterLoaded = true
                }
            }
        )
    }

    val effectivePageCount = when {
        isOfflineComplete -> localPagePaths.size
        isOfflineIncomplete -> maxOf(localPagePaths.size, livePageUrls.size, chapter.pageCount)
        livePageUrls.isNotEmpty() -> livePageUrls.size
        localPagePaths.isNotEmpty() -> localPagePaths.size
        else -> 0
    }
    val totalPages = effectivePageCount.coerceAtLeast(1)

    var showReaderHud by remember { mutableStateOf(true) }
    var showQuickTunePopup by remember { mutableStateOf(false) }
    var showCommentsDialog by remember { mutableStateOf(false) }
    var showPagesGridDialog by remember { mutableStateOf(false) }
    var showChaptersListSheet by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var isBookmarked by remember(chapter.id) { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var zoomOffsetX by remember { mutableFloatStateOf(0f) }
    var zoomOffsetY by remember { mutableFloatStateOf(0f) }
    var isWebtoonMode by remember(settings.webtoonAlwaysOn) { mutableStateOf(settings.webtoonAlwaysOn) }
    var isDualPageSpread by remember { mutableStateOf(false) }
    var localAutoCrop by remember(settings.autoCropBorders) { mutableStateOf(settings.autoCropBorders) }
    var localColorFilter by remember(settings.readerColorFilter) { mutableStateOf(settings.readerColorFilter) }
    var localAutoScroll by remember(settings.autoScroll) { mutableStateOf(settings.autoScroll) }
    var localAutoScrollSpeed by remember(settings.autoScrollSpeed) {
        mutableFloatStateOf(settings.autoScrollSpeed.coerceIn(0.1f, 1.0f))
    }
    var isUserTouchingScreen by remember { mutableStateOf(false) }
    val readerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            readerFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    val safeInitialPage = remember(chapter.id, initialPageIndex, totalPages) {
        initialPageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    }

    val webtoonListState = rememberLazyListState(
        initialFirstVisibleItemIndex = safeInitialPage
    )
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { totalPages }
    )

    val currentPageIndex by remember {
        derivedStateOf {
            if (isWebtoonMode) {
                val baseIdx = webtoonListState.firstVisibleItemIndex
                val adjusted = if (isOfflineIncomplete && baseIdx > 0) baseIdx - 1 else baseIdx
                adjusted.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            } else {
                pagerState.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            }
        }
    }

    val readerContext = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(currentPageIndex, isChapterLoaded) {
        if (isChapterLoaded && effectivePageCount > 0) {
            onSaveReadingProgress(currentPageIndex)
        }
    }

    // Active lookahead prefetcher: preloads current + next 5 pages in background so scrolling has zero lag
    LaunchedEffect(currentPageIndex, livePageUrls) {
        if (livePageUrls.isNotEmpty() && !isOfflineComplete) {
            val loader = com.example.ui.components.MangaCoilImageLoader.get(readerContext)
            val referer = targetChapterUrl.ifBlank { work.siteUrl }
            val startIdx = currentPageIndex.coerceAtLeast(0)
            val endIdx = (startIdx + 5).coerceAtMost(livePageUrls.lastIndex)
            for (idx in startIdx..endIdx) {
                val url = livePageUrls.getOrNull(idx) ?: continue
                if (url.startsWith("http")) {
                    val req = coil.request.ImageRequest.Builder(readerContext)
                        .data(url)
                        .setHeader("Referer", referer)
                        .setHeader("User-Agent", LiveMangaSiteScraper.activeUserAgent.replace("; wv", ""))
                        .memoryCacheKey(url)
                        .diskCacheKey(url)
                        .build()
                    loader.enqueue(req)
                }
            }
        }
    }

    // Smart Touch-Aware Auto-Scroll: pauses immediately on finger touch and resumes on release
    LaunchedEffect(localAutoScroll, localAutoScrollSpeed, isUserTouchingScreen, isChapterLoaded, isWebtoonMode, totalPages) {
        if (localAutoScroll && !isUserTouchingScreen && isChapterLoaded && effectivePageCount > 0) {
            if (isWebtoonMode) {
                while (true) {
                    delay(32L)
                    if (!isUserTouchingScreen) {
                        val pxPerFrame = (2.5f + localAutoScrollSpeed * 16f)
                        webtoonListState.scrollBy(pxPerFrame)
                    }
                }
            } else {
                val intervalMs = (3600L - (localAutoScrollSpeed * 2400f).toLong()).coerceIn(900L, 4000L)
                while (true) {
                    delay(intervalMs)
                    if (!isUserTouchingScreen) {
                        val next = (pagerState.currentPage + 1).coerceAtMost(totalPages - 1)
                        pagerState.animateScrollToPage(next)
                    }
                }
            }
        }
    }

    val commentsList = remember {
        mutableStateListOf<Pair<String, String>>()
    }
    var newCommentText by remember { mutableStateOf("") }

    fun jumpToPage(targetIndex: Int) {
        val clamped = targetIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
        scope.launch {
            if (isWebtoonMode) {
                val targetListIdx = if (isOfflineIncomplete) clamped + 1 else clamped
                webtoonListState.animateScrollToItem(targetListIdx)
            } else {
                pagerState.animateScrollToPage(clamped)
            }
        }
    }

    val sortedWorkChapters = remember(work.chapters) {
        work.chapters.sortedBy { it.number }
    }
    val prevChapter = remember(sortedWorkChapters, chapter.number) {
        sortedWorkChapters.lastOrNull { it.number < chapter.number }
            ?: work.chapters.find { it.number == chapter.number - 1 }
    }
    val nextChapter = remember(sortedWorkChapters, chapter.number) {
        sortedWorkChapters.firstOrNull { it.number > chapter.number }
            ?: work.chapters.find { it.number == chapter.number + 1 }
    }

    fun scrollOrTurnNext() {
        scope.launch {
            if (isWebtoonMode) {
                webtoonListState.scrollBy(620f)
            } else {
                val nextIdx = (pagerState.currentPage + 1).coerceAtMost(totalPages - 1)
                pagerState.animateScrollToPage(nextIdx)
            }
        }
    }

    fun scrollOrTurnPrev() {
        scope.launch {
            if (isWebtoonMode) {
                webtoonListState.scrollBy(-620f)
            } else {
                val prevIdx = (pagerState.currentPage - 1).coerceAtLeast(0)
                pagerState.animateScrollToPage(prevIdx)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C12))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .focusRequester(readerFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (!settings.volumeKeysScroll) return@onKeyEvent false
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            scrollOrTurnNext()
                            true
                        }
                        KeyEvent.KEYCODE_VOLUME_UP -> {
                            scrollOrTurnPrev()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        isUserTouchingScreen = event.changes.any { it.pressed }
                    }
                }
            }
            .pointerInput(settings.tapZoneMode, settings.webtoonZoom, isWebtoonMode, totalPages) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        if (showQuickTunePopup) {
                            showQuickTunePopup = false
                            return@detectTapGestures
                        }
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val relX = tapOffset.x / width
                        when (settings.tapZoneMode) {
                            "المنتصف فقط للقائمة" -> {
                                showReaderHud = !showReaderHud
                            }
                            "جانبي (كامل اليمين واليسار للتقليب)" -> {
                                when {
                                    relX < 0.42f -> scrollOrTurnNext()
                                    relX > 0.58f -> scrollOrTurnPrev()
                                    else -> showReaderHud = !showReaderHud
                                }
                            }
                            else -> {
                                // قياسي (يمين/يسار للتقليب، منتصف للقائمة)
                                when {
                                    relX < 0.28f -> scrollOrTurnNext()
                                    relX > 0.72f -> scrollOrTurnPrev()
                                    else -> showReaderHud = !showReaderHud
                                }
                            }
                        }
                    },
                    onDoubleTap = { tapOffset ->
                        if (settings.webtoonZoom) {
                            val nextScale = when {
                                zoomScale < 1.8f -> 2.25f
                                zoomScale < 2.7f -> 3.0f
                                else -> 1f
                            }
                            zoomScale = nextScale
                            if (nextScale > 1.05f) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val maxPanX = (size.width * (nextScale - 1f)) / 2f
                                val maxPanY = (size.height * (nextScale - 1f)) / 2f
                                zoomOffsetX = ((centerX - tapOffset.x) * (nextScale - 1f)).coerceIn(-maxPanX, maxPanX)
                                zoomOffsetY = ((centerY - tapOffset.y) * (nextScale - 1f)).coerceIn(-maxPanY, maxPanY)
                            } else {
                                zoomOffsetX = 0f
                                zoomOffsetY = 0f
                            }
                        }
                    }
                )
            }
            .pointerInput(settings.webtoonZoom) {
                if (settings.webtoonZoom) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                        zoomScale = newScale
                        if (newScale > 1.02f) {
                            val maxPanX = (size.width * (newScale - 1f)) / 2f
                            val maxPanY = (size.height * (newScale - 1f)) / 2f
                            zoomOffsetX = (zoomOffsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                            zoomOffsetY = (zoomOffsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                        } else {
                            zoomOffsetX = 0f
                            zoomOffsetY = 0f
                        }
                    }
                }
            }
            .testTag("manga_chapter_reader_screen")
    ) {
        // ==================== MAIN READER AREA (OFFLINE LOCAL FILES OR LIVE CHAPTER PAGES) ====================
        if (!isChapterLoaded) {
            // Exact Reader Loading Screen from Video (00:40 - 00:45):
            // Black background + "جار التحميل..." then "التحميل: X٪" with horizontal progress bar
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (loadingProgress <= 15) {
                            "جار التحميل..."
                        } else {
                            "التحميل: ${MainViewModel.toArabicDigits(loadingProgress.toString())}٪"
                        },
                        color = TextPrimaryWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (loadingProgress > 15) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { loadingProgress / 100f },
                            color = PeriwinklePrimary,
                            trackColor = Color(0xFF2D313E),
                            modifier = Modifier
                                .width(150.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        } else if (effectivePageCount == 0) {
            // Exact Error Screen from Video (01:44): "خطأ في التحميل" + underlined "حاول مجددا"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "خطأ في التحميل",
                        color = TextPrimaryWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "حاول مجددا",
                        color = PeriwinklePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { refreshTrigger++ }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("reader_retry_empty_button")
                    )
                }
            }
        } else {
            if (isWebtoonMode) {
                // Continuous Vertical Webtoon Strip with zero unnecessary gaps between panels
                LazyColumn(
                    state = webtoonListState,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = zoomOffsetX,
                            translationY = zoomOffsetY
                        )
                        .testTag("reader_webtoon_list"),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (isOfflineIncomplete) {
                        item(key = "incomplete_warning") {
                            Surface(
                                color = Color(0xFF3E2723),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("reader_incomplete_warning_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF8A80))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "هذا الفصل غير مكتمل (${localPagePaths.size} من $effectivePageCount صفحة محفوظة)",
                                                color = TextPrimaryWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (chapter.downloadError.isNotBlank()) {
                                                Text(
                                                    text = chapter.downloadError,
                                                    color = Color(0xFFFFCCBC),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            onDownloadChapter(
                                                work,
                                                chapter.copy(
                                                    pageImageUrls = livePageUrls,
                                                    pageCount = effectivePageCount
                                                )
                                            )
                                        },
                                        modifier = Modifier.testTag("reader_resume_incomplete_button")
                                    ) {
                                        Text("استكمال التنزيل", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    items(effectivePageCount, key = { "page_${chapter.id}_$it" }) { pageIdx ->
                        ChapterPageIllustration(
                            work = work,
                            chapterNumber = chapter.number,
                            pageIndex = pageIdx,
                            totalPages = effectivePageCount,
                            pageImageUrl = livePageUrls.getOrNull(pageIdx),
                            localFilePath = localPagePaths.getOrNull(pageIdx),
                            refererUrl = targetChapterUrl,
                            autoCropBorders = localAutoCrop,
                            colorFilterMode = localColorFilter,
                            splitLongStrip = settings.splitLongStripPages
                        )
                    }

                    // End of Chapter Summary & Next/Prev Chapter Card
                    item(key = "chapter_footer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF12151E))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "نهاية ${chapter.title} (${effectivePageCount} صفحة)",
                                color = TextPrimaryWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (prevChapter != null) {
                                    OutlinedButton(
                                        onClick = { onSelectChapter(prevChapter) },
                                        shape = RoundedCornerShape(22.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("الفصل السابق (${prevChapter.number})", color = TextPrimaryWhite)
                                    }
                                }
                                if (nextChapter != null) {
                                    Button(
                                        onClick = { onSelectChapter(nextChapter) },
                                        shape = RoundedCornerShape(22.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PeriwinklePrimary,
                                            contentColor = PeriwinkleOnPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("الفصل التالي (${nextChapter.number})", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Horizontal Paged Mode (or Dual-Page Book Spread for Galaxy Z Fold 4 Unfolded Display)
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = zoomScale <= 1.05f,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = zoomOffsetX,
                            translationY = zoomOffsetY
                        )
                        .testTag("reader_horizontal_pager")
                ) { pageIdx ->
                    if (isDualPageSpread && pageIdx + 1 < effectivePageCount) {
                        // Dual-Page Book Spread (RTL Manga order: Right page = pageIdx, Left page = pageIdx + 1)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("reader_dual_page_spread"),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        ChapterPageIllustration(
                                            work = work,
                                            chapterNumber = chapter.number,
                                            pageIndex = pageIdx,
                                            totalPages = effectivePageCount,
                                            pageImageUrl = livePageUrls.getOrNull(pageIdx),
                                            localFilePath = localPagePaths.getOrNull(pageIdx),
                                            refererUrl = targetChapterUrl,
                                            autoCropBorders = localAutoCrop,
                                            colorFilterMode = localColorFilter,
                                            splitLongStrip = settings.splitLongStripPages
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        ChapterPageIllustration(
                                            work = work,
                                            chapterNumber = chapter.number,
                                            pageIndex = pageIdx + 1,
                                            totalPages = effectivePageCount,
                                            pageImageUrl = livePageUrls.getOrNull(pageIdx + 1),
                                            localFilePath = localPagePaths.getOrNull(pageIdx + 1),
                                            refererUrl = targetChapterUrl,
                                            autoCropBorders = localAutoCrop,
                                            colorFilterMode = localColorFilter,
                                            splitLongStrip = settings.splitLongStripPages
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                ChapterPageIllustration(
                                    work = work,
                                    chapterNumber = chapter.number,
                                    pageIndex = pageIdx,
                                    totalPages = effectivePageCount,
                                    pageImageUrl = livePageUrls.getOrNull(pageIdx),
                                    localFilePath = localPagePaths.getOrNull(pageIdx),
                                    refererUrl = targetChapterUrl,
                                    autoCropBorders = localAutoCrop,
                                    colorFilterMode = localColorFilter,
                                    splitLongStrip = settings.splitLongStripPages
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Smart Auto-Scroll Speed Controller Pill (pauses automatically when touching screen)
        AnimatedVisibility(
            visible = localAutoScroll,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
        ) {
            Surface(
                color = Color(0xE6181C26),
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 8.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.testTag("reader_auto_scroll_floating_bar")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isUserTouchingScreen) "مؤقت ✋" else "تمرير تلقائي",
                        color = if (isUserTouchingScreen) Color(0xFFFFB74D) else PeriwinklePrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            val updated = (localAutoScrollSpeed + 0.15f).coerceIn(0.1f, 1.0f)
                            localAutoScrollSpeed = updated
                            onUpdateAutoScrollSpeed(updated)
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", color = TextPrimaryWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = String.format(java.util.Locale.US, "%.1fx", 0.5f + localAutoScrollSpeed * 2.0f),
                        color = TextPrimaryWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            val updated = (localAutoScrollSpeed - 0.15f).coerceIn(0.1f, 1.0f)
                            localAutoScrollSpeed = updated
                            onUpdateAutoScrollSpeed(updated)
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("−", color = TextPrimaryWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            localAutoScroll = false
                            onUpdateAutoScroll(false)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إيقاف التمرير التلقائي",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Custom Brightness Dimming Overlay if enabled in settings
        if (settings.customBrightnessEnabled) {
            val dimAlpha = ((100 - settings.customBrightnessLevel).coerceIn(0, 80)) / 120f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }

        // ==================== COMPACT TOP READER BAR ====================
        AnimatedVisibility(
            visible = showReaderHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color(0xF2151821),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimaryWhite
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = work.title,
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleStatus = when {
                            isOfflineComplete -> "${chapter.title} • محفوظ بدون إنترنت (${localPagePaths.size} ص)"
                            isOfflineIncomplete -> "${chapter.title} • غير مكتمل (${localPagePaths.size}/$effectivePageCount ص)"
                            isActivelyDownloading -> {
                                val done = liveChapterDownloadState?.downloadedPagesCount ?: 0
                                val tot = liveChapterDownloadState?.pageCount ?: effectivePageCount
                                "${chapter.title} • جارٍ التنزيل ($done/$tot)"
                            }
                            else -> "${chapter.title} • صفحة ${currentPageIndex + 1} من $totalPages"
                        }
                        Text(
                            text = subtitleStatus,
                            color = when {
                                isOfflineComplete -> Color(0xFF81C784)
                                isOfflineIncomplete -> Color(0xFFFFB74D)
                                isActivelyDownloading -> PeriwinklePrimary
                                else -> TextSecondaryGrey
                            },
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Chapters Switcher Button ("قائمة الفصول")
                    IconButton(
                        onClick = { showChaptersListSheet = true },
                        modifier = Modifier.testTag("reader_chapters_list_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "قائمة الفصول",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // Pages Grid Jumper Button
                    IconButton(
                        onClick = { showPagesGridDialog = true },
                        modifier = Modifier.testTag("reader_pages_grid_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "الانتقال بين الصفحات",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // ==================== QUICK TUNE POPUP (وضع الويبتون ☑ / الاعدادات) ====================
        AnimatedVisibility(
            visible = showReaderHud && showQuickTunePopup,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 132.dp)
        ) {
            Surface(
                color = Color(0xFF1F232E),
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .width(245.dp)
                    .testTag("reader_quick_tune_popup")
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = !isWebtoonMode
                                isWebtoonMode = updated
                                onUpdateWebtoonMode(updated)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("toggle_webtoon_mode_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Checkbox(
                            checked = isWebtoonMode,
                            onCheckedChange = {
                                isWebtoonMode = it
                                onUpdateWebtoonMode(it)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PeriwinklePrimary,
                                checkmarkColor = PeriwinkleOnPrimary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "وضع الويبتون العمودي",
                            color = TextPrimaryWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = !localAutoScroll
                                localAutoScroll = updated
                                onUpdateAutoScroll(updated)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("toggle_auto_scroll_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Checkbox(
                            checked = localAutoScroll,
                            onCheckedChange = {
                                localAutoScroll = it
                                onUpdateAutoScroll(it)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PeriwinklePrimary,
                                checkmarkColor = PeriwinkleOnPrimary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "التمرير التلقائي الذكي",
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dual-Page Book Spread for Galaxy Z Fold 4 Unfolded Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val nextDual = !isDualPageSpread
                                isDualPageSpread = nextDual
                                if (nextDual && isWebtoonMode) {
                                    isWebtoonMode = false
                                    onUpdateWebtoonMode(false)
                                }
                                onShowToast(if (nextDual) "تم تفعيل وضع الكتاب المزدوج (صفحتان متجاورتان)" else "تم إيقاف وضع الكتاب المزدوج")
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("toggle_dual_page_spread_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Checkbox(
                            checked = isDualPageSpread,
                            onCheckedChange = {
                                isDualPageSpread = it
                                if (it && isWebtoonMode) {
                                    isWebtoonMode = false
                                    onUpdateWebtoonMode(false)
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PeriwinklePrimary,
                                checkmarkColor = PeriwinkleOnPrimary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "وضع الكتاب المزدوج (Fold)",
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Auto Crop White/Black Borders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val nextCrop = !localAutoCrop
                                localAutoCrop = nextCrop
                                onUpdateAutoCropBorders(nextCrop)
                                onShowToast(if (nextCrop) "تم تفعيل القص التلقائي للحواشي البيضاء" else "تم إيقاف قص الحواشي")
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("toggle_auto_crop_borders_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Checkbox(
                            checked = localAutoCrop,
                            onCheckedChange = {
                                localAutoCrop = it
                                onUpdateAutoCropBorders(it)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PeriwinklePrimary,
                                checkmarkColor = PeriwinkleOnPrimary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "القص التلقائي للحواشي البيضاء",
                            color = TextPrimaryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Reader Color Filter quick cycle row (Sepia / Smart Night Invert / Grayscale)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val modes = listOf(
                                    "طبيعي (افتراضي)",
                                    "دافئ لحماية العين (Sepia)",
                                    "عكس الألوان الذكي (ليلي)",
                                    "تدرج رمادي مريح (Grayscale)"
                                )
                                val nextIdx = (modes.indexOf(localColorFilter) + 1) % modes.size
                                val nextFilter = modes[nextIdx]
                                localColorFilter = nextFilter
                                onUpdateColorFilter(nextFilter)
                                onShowToast("فلتر القراءة: $nextFilter")
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("cycle_color_filter_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = localColorFilter.substringBefore(" ("),
                            color = PeriwinklePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "فلتر ألوان العين / ليلي",
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 1-Tap Save as PDF or CBZ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showQuickTunePopup = false
                                onExportChapterPdf(
                                    work,
                                    chapter.copy(
                                        pageImageUrls = livePageUrls,
                                        localPagePaths = localPagePaths,
                                        pageCount = effectivePageCount
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("reader_export_pdf_btn")
                        ) {
                            Text("حفظ PDF", color = PeriwinklePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                showQuickTunePopup = false
                                onExportChapterCbz(
                                    work,
                                    chapter.copy(
                                        pageImageUrls = livePageUrls,
                                        localPagePaths = localPagePaths,
                                        pageCount = effectivePageCount
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("reader_export_cbz_btn")
                        ) {
                            Text("حفظ CBZ", color = PeriwinklePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showQuickTunePopup = false
                                showDiagnosticsDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "سجل تشخيص الصفحات",
                            color = PeriwinklePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showQuickTunePopup = false
                                onOpenReadingSettings()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("open_reading_settings_from_reader"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "إعدادات القراءة الكاملة",
                            color = TextPrimaryWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==================== BOTTOM READER BAR ====================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = showReaderHud,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xF2141720),
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        // Fast Thumbnail Page Scrubber Strip above the Page Slider
                        if (totalPages > 1) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .testTag("reader_thumbnail_scrubber"),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(totalPages, key = { "thumb_scrub_$it" }) { idx ->
                                    val isCurrent = idx == currentPageIndex
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) PeriwinklePrimary else Color(0xFF232734),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCurrent) PeriwinklePrimary else Color(0xFF394054)
                                        ),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .clickable { jumpToPage(idx) }
                                            .testTag("reader_thumb_pill_$idx")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 9.dp)
                                        ) {
                                            Text(
                                                text = "ص ${idx + 1}",
                                                color = if (isCurrent) PeriwinkleOnPrimary else TextPrimaryWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Row 1: Next/Prev Chapter Circular Buttons + Dotted Page Slider
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (prevChapter != null) Color(0xFF232734) else Color(0xFF191C24),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            if (prevChapter != null) {
                                                onSelectChapter(prevChapter)
                                            } else {
                                                onShowToast("أنت في الفصل الأول")
                                            }
                                        }
                                        .testTag("reader_prev_chapter_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "الفصل السابق",
                                            tint = if (prevChapter != null) TextPrimaryWhite else TextSecondaryGrey.copy(alpha = 0.45f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF232734))
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                                        val count = totalPages.coerceIn(2, 24)
                                        val step = size.width / (count - 1)
                                        for (i in 0 until count) {
                                            drawCircle(
                                                color = Color(0xFF6E758A),
                                                radius = 2.5f,
                                                center = Offset(i * step, size.height / 2f)
                                            )
                                        }
                                    }

                                    Slider(
                                        value = currentPageIndex.toFloat(),
                                        onValueChange = { jumpToPage(it.roundToInt()) },
                                        valueRange = 0f..(totalPages - 1).coerceAtLeast(1).toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = PeriwinklePrimary,
                                            activeTrackColor = PeriwinklePrimary,
                                            inactiveTrackColor = Color(0xFF394054)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (nextChapter != null) Color(0xFF232734) else Color(0xFF191C24),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            if (nextChapter != null) {
                                                onSelectChapter(nextChapter)
                                            } else {
                                                onShowToast("هذا هو أحدث فصل متاح")
                                            }
                                        }
                                        .testTag("reader_next_chapter_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "الفصل التالي",
                                            tint = if (nextChapter != null) TextPrimaryWhite else TextSecondaryGrey.copy(alpha = 0.45f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 2: Action Icons from Left to Right (♡ Favorite, 💾 Download/Save, 🔖 Bookmark, 🔄 Zoom, 🎛 Settings)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleFavorite(work) },
                                    modifier = Modifier.testTag("reader_favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "المفضلة",
                                        tint = if (isFavorite) PeriwinklePrimary else TextPrimaryWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isOfflineComplete) {
                                            onShowToast("هذا الفصل محفوظ بالكامل وجاهز للقراءة بدون إنترنت (${localPagePaths.size} صفحة)")
                                        } else {
                                            onDownloadChapter(
                                                work,
                                                chapter.copy(
                                                    pageImageUrls = livePageUrls,
                                                    pageCount = effectivePageCount.coerceAtLeast(chapter.pageCount)
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("reader_save_chapter_button")
                                ) {
                                    if (isActivelyDownloading) {
                                        CircularProgressIndicator(
                                            color = PeriwinklePrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isOfflineComplete) Icons.Default.CloudDone else Icons.Default.Save,
                                            contentDescription = "حفظ الفصل",
                                            tint = when {
                                                isOfflineComplete -> Color(0xFF81C784)
                                                isOfflineIncomplete -> Color(0xFFFFB74D)
                                                else -> TextPrimaryWhite
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        isBookmarked = !isBookmarked
                                        onShowToast(
                                            if (isBookmarked) "تم حفظ علامة مرجعية عند الصفحة ${currentPageIndex + 1}"
                                            else "تمت إزالة العلامة المرجعية"
                                        )
                                    },
                                    modifier = Modifier.testTag("reader_bookmark_button")
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "علامة مرجعية",
                                        tint = if (isBookmarked) PeriwinklePrimary else TextPrimaryWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (zoomScale > 1.05f) {
                                            zoomScale = 1f
                                            zoomOffsetX = 0f
                                            zoomOffsetY = 0f
                                        } else {
                                            zoomScale = 1.5f
                                        }
                                        onShowToast(if (zoomScale > 1f) "تم تكبير عرض الصفحة" else "تمت استعادة الحجم الطبيعي")
                                    },
                                    modifier = Modifier.testTag("reader_rotate_fit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "تدوير وملاءمة الشاشة",
                                        tint = TextPrimaryWhite,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showQuickTunePopup = !showQuickTunePopup },
                                    modifier = Modifier.testTag("reader_tune_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "إعدادات القراءة السريعة",
                                        tint = if (showQuickTunePopup) PeriwinklePrimary else TextPrimaryWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Mini Status Strip: Page "١/١٢" in Center
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC0A0C12))
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOfflineComplete) "OFFLINE" else "ONLINE",
                        color = if (isOfflineComplete) Color(0xFF81C784) else TextSecondaryGrey,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (settings.showPageNumber) {
                        val curAr = MainViewModel.toArabicDigits((currentPageIndex + 1).toString())
                        val totAr = MainViewModel.toArabicDigits(totalPages.toString())
                        Text(
                            text = "$curAr / $totAr",
                            color = TextPrimaryWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("reader_page_indicator_text")
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = "الفصل ${MainViewModel.toArabicDigits(chapter.number.toString())}",
                        color = TextPrimaryWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Chapters Switcher Modal Dialog ("قائمة الفصول")
    if (showChaptersListSheet) {
        AlertDialog(
            onDismissRequest = { showChaptersListSheet = false },
            containerColor = AppDialogSurface,
            title = {
                Text(
                    text = "فصول ${work.title} (${work.chapters.size})",
                    color = TextPrimaryWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (work.chapters.isEmpty()) {
                    Text("لا توجد فصول إضافية محمّلة في القائمة الحالية.", color = TextSecondaryGrey, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(work.chapters.sortedByDescending { it.number }, key = { it.id }) { itemChapter ->
                            val isCurrent = itemChapter.number == chapter.number
                            Surface(
                                color = if (isCurrent) PeriwinklePrimary.copy(alpha = 0.2f) else Color(0xFF222632),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showChaptersListSheet = false
                                        if (!isCurrent) {
                                            onSelectChapter(itemChapter)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = itemChapter.title,
                                            color = if (isCurrent) PeriwinklePrimary else TextPrimaryWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (itemChapter.dateText.isNotBlank()) {
                                            Text(
                                                text = itemChapter.dateText,
                                                color = TextSecondaryGrey,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (itemChapter.isDownloaded || itemChapter.downloadStatus == "COMPLETED") {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "محفوظ",
                                            tint = Color(0xFF81C784),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChaptersListSheet = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }

    // Developer Diagnostics Log Dialog
    if (showDiagnosticsDialog) {
        val currentLogs = if (diagnosticLogs.isNotEmpty()) diagnosticLogs else DiagnosticLogger.logs.value
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سجل التشخيص التقني", color = TextPrimaryWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "الصفحات: $effectivePageCount",
                        color = PeriwinklePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "المصدر: ${work.siteName} • الفصل ${chapter.number} • محلي: ${localPagePaths.size} • مباشر: ${livePageUrls.size}",
                        color = TextSecondaryGrey,
                        fontSize = 11.sp
                    )
                    HorizontalDivider(color = Color(0xFF2C3140))
                    if (currentLogs.isEmpty()) {
                        Text(
                            text = "لا توجد أخطاء أو أحداث مسجلة حتى الآن.",
                            color = TextSecondaryGrey,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(currentLogs, key = { it.id }) { entry ->
                                val badgeColor = when (entry.level) {
                                    DiagnosticLevel.ERROR -> Color(0xFFE57373)
                                    DiagnosticLevel.WARN -> Color(0xFFFFB74D)
                                    DiagnosticLevel.INFO -> Color(0xFF81C784)
                                }
                                Surface(
                                    color = Color(0xFF1A1E29),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "[${entry.category.labelAr}] ${entry.level.labelAr}",
                                                color = badgeColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = entry.formattedTime,
                                                color = TextSecondaryGrey,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Text(
                                            text = entry.message,
                                            color = TextPrimaryWhite,
                                            fontSize = 12.sp
                                        )
                                        if (entry.url.isNotBlank()) {
                                            Text(
                                                text = entry.url,
                                                color = TextSecondaryGrey,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            },
            dismissButton = {
                if (currentLogs.isNotEmpty()) {
                    TextButton(onClick = onClearDiagnosticLogs) {
                        Text("مسح السجل", color = TextSecondaryGrey)
                    }
                }
            }
        )
    }

    // Chapter Comments Dialog
    if (showCommentsDialog) {
        AlertDialog(
            onDismissRequest = { showCommentsDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Text("تعليقات ${chapter.title}", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (commentsList.isEmpty()) {
                        Text(
                            text = "لا توجد تعليقات مضافة بعد على هذا الفصل.",
                            color = TextSecondaryGrey,
                            fontSize = 13.sp
                        )
                    } else {
                        commentsList.forEach { (comment, time) ->
                            Surface(
                                color = Color(0xFF232734),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(comment, color = TextPrimaryWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(time, color = TextSecondaryGrey, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("أضف تعليقك على الفصل...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            commentsList.add(0, newCommentText.trim() to "الآن")
                            newCommentText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PeriwinklePrimary,
                        contentColor = PeriwinkleOnPrimary
                    )
                ) {
                    Text("إرسال", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommentsDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }

    // Pages Grid Jumper Dialog ("⊞" button in Top Bar)
    if (showPagesGridDialog) {
        AlertDialog(
            onDismissRequest = { showPagesGridDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Text("الانتقال السريع بين الصفحات ($totalPages صفحة)", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "${chapter.title} • اختر رقم الصفحة للانتقال المباشر:",
                            color = TextSecondaryGrey,
                            fontSize = 13.sp
                        )
                    }
                    val rows = (1..totalPages).chunked(4)
                    items(rows.size) { rowIdx ->
                        val rowPages = rows[rowIdx]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPages.forEach { pageNum ->
                                val isCur = (currentPageIndex + 1) == pageNum
                                OutlinedButton(
                                    onClick = {
                                        showPagesGridDialog = false
                                        jumpToPage(pageNum - 1)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isCur) PeriwinklePrimary else Color.Transparent,
                                        contentColor = if (isCur) PeriwinkleOnPrimary else TextPrimaryWhite
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("ص $pageNum", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPagesGridDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }
}
