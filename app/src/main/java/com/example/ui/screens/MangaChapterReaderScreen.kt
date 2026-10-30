package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.components.CloudflareProtectionDialog
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
    onUpdateWebtoonMode: (Boolean) -> Unit,
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
                delay(900)
                isChapterLoaded = true
            }
        }
    }

    // Hidden WebView DOM Bridge only when online and not reading a completed offline chapter
    if ((!isOfflineComplete && targetChapterUrl.startsWith("http")) || refreshTrigger > 0) {
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
    var isWebtoonMode by remember(settings.webtoonAlwaysOn) { mutableStateOf(settings.webtoonAlwaysOn) }

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

    LaunchedEffect(currentPageIndex, isChapterLoaded) {
        if (isChapterLoaded && effectivePageCount > 0) {
            onSaveReadingProgress(currentPageIndex)
        }
    }

    // Auto-scroll support if enabled in settings
    LaunchedEffect(settings.autoScroll, isChapterLoaded, isWebtoonMode, totalPages) {
        if (settings.autoScroll && isChapterLoaded && isWebtoonMode && effectivePageCount > 0) {
            while (true) {
                delay(1800)
                val next = (webtoonListState.firstVisibleItemIndex + 1).coerceAtMost(totalPages - 1)
                webtoonListState.animateScrollToItem(next)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C12))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (showQuickTunePopup) {
                            showQuickTunePopup = false
                        } else {
                            // Toggles HUD visibility smoothly without causing any page image recomposition/reload
                            showReaderHud = !showReaderHud
                        }
                    },
                    onDoubleTap = {
                        if (settings.webtoonZoom) {
                            zoomScale = if (zoomScale > 1.05f) 1f else 1.65f
                        }
                    }
                )
            }
            .testTag("manga_chapter_reader_screen")
    ) {
        // ==================== MAIN READER AREA (OFFLINE LOCAL FILES OR LIVE CHAPTER PAGES) ====================
        if (!isChapterLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = PeriwinklePrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "جارٍ استخراج صفحات ${chapter.title} (${MainViewModel.toArabicDigits(loadingProgress.toString())}٪)",
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        color = PeriwinklePrimary,
                        trackColor = Color(0xFF2D313E),
                        modifier = Modifier
                            .width(170.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        } else if (effectivePageCount == 0) {
            // Prompt to verify Cloudflare, retry, or inspect diagnostics
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PeriwinklePrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (requiresCloudflare) {
                            "يتطلب الفصل تخطي حماية Cloudflare لتحميل جميع صفحات المانجا من الموقع"
                        } else {
                            lastScrapeError ?: "تعذر استخراج صور الفصل تلقائياً — افتح نافذة الحماية لتحميل الصفحات مباشرة"
                        },
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showProtectionModal = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PeriwinklePrimary,
                                contentColor = PeriwinkleOnPrimary
                            )
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تخطي الحماية وجلب الصور", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { refreshTrigger++ }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimaryWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة المحاولة", color = TextPrimaryWhite)
                        }
                    }
                    TextButton(onClick = { showDiagnosticsDialog = true }) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = TextSecondaryGrey, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عرض سجل التشخيص التقني", color = TextSecondaryGrey, fontSize = 12.sp)
                    }
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
                            scaleY = zoomScale
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
                            refererUrl = targetChapterUrl
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
                // Horizontal Paged Mode when "وضع الويبتون" is unchecked
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale
                        )
                        .testTag("reader_horizontal_pager")
                ) { pageIdx ->
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            ChapterPageIllustration(
                                work = work,
                                chapterNumber = chapter.number,
                                pageIndex = pageIdx,
                                totalPages = effectivePageCount,
                                pageImageUrl = livePageUrls.getOrNull(pageIdx),
                                localFilePath = localPagePaths.getOrNull(pageIdx),
                                refererUrl = targetChapterUrl
                            )
                        }
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

                    // Cloudflare / Site Protection Button
                    IconButton(
                        onClick = { showProtectionModal = true },
                        modifier = Modifier.testTag("reader_protection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "حماية الموقع",
                            tint = if (requiresCloudflare) Color(0xFFFFB74D) else PeriwinklePrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Developer Diagnostics Log Button
                    IconButton(
                        onClick = { showDiagnosticsDialog = true },
                        modifier = Modifier.testTag("reader_diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "سجل التشخيص",
                            tint = TextSecondaryGrey,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Quick Reader Settings Button in Top Bar as well
                    IconButton(
                        onClick = { showQuickTunePopup = !showQuickTunePopup },
                        modifier = Modifier.testTag("reader_top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "إعدادات القارئ",
                            tint = if (showQuickTunePopup) PeriwinklePrimary else TextPrimaryWhite,
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
                    .width(215.dp)
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
                                showQuickTunePopup = false
                                showDiagnosticsDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                            .padding(horizontal = 16.dp, vertical = 10.dp)
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
                                        zoomScale = if (zoomScale == 1f) 1.25f else 1f
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

    // Interactive Cloudflare Protection & Live DOM Extraction Modal
    if (showProtectionModal) {
        CloudflareProtectionDialog(
            url = targetChapterUrl,
            siteName = work.siteName,
            jsExtractorScript = LiveMangaSiteScraper.JS_EXTRACT_CHAPTER_PAGES,
            onExtractedDomJson = { rawJson ->
                val domRes = LiveMangaSiteScraper.parseChapterPagesFromDomJson(rawJson, targetChapterUrl)
                if (domRes.pageUrls.isNotEmpty()) {
                    livePageUrls = domRes.pageUrls
                    requiresCloudflare = false
                    isChapterLoaded = true
                }
            },
            onDismissAndRefresh = {
                showProtectionModal = false
                refreshTrigger++
            }
        )
    }
}
