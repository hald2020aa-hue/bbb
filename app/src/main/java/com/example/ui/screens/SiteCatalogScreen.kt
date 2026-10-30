package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaCatalogData
import com.example.data.MangaLatestUpdateItem
import com.example.data.MangaWork
import com.example.data.local.SiteEntity
import com.example.ui.components.CloudflareProtectionDialog
import com.example.ui.components.HiddenSiteWebViewBridge
import com.example.ui.components.MangaCoverArtwork
import com.example.ui.theme.AppCardSurface
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.AppDividerColor
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import androidx.compose.material3.CircularProgressIndicator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SiteCatalogScreen(
    site: SiteEntity,
    works: List<MangaWork>,
    cachedSiteUpdates: List<MangaLatestUpdateItem> = emptyList(),
    showContinueReadingButton: Boolean,
    onOpenDrawer: () -> Unit,
    onSelectWork: (MangaWork) -> Unit,
    onOpenUpdateWorkDetails: (MangaLatestUpdateItem) -> Unit = {},
    onOpenUpdateChapter: (MangaLatestUpdateItem) -> Unit = {},
    onCacheSiteUpdates: (List<MangaLatestUpdateItem>) -> Unit = {},
    onContinueReadingClick: () -> Unit,
    onOpenSiteInWebView: (SiteEntity) -> Unit,
    onShowToast: (String) -> Unit,
    onSaveWholeMangaToStorage: (MangaWork) -> Unit = {},
    onOpenDownloadsScreen: () -> Unit = {},
    onOpenSettingsScreen: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var submittedSearchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf("آخر الاضافات") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheetDialog by remember { mutableStateOf(false) }
    var showProtectionDialog by remember { mutableStateOf(false) }

    // Tab mode inside Site screen: "UPDATES" ("آخر التحديثات") or "CATALOG" ("الأعمال والكتالوج")
    var activeSectionTab by remember(site.id) { mutableStateOf("CATALOG") }

    // Overflow ⋮ Menu states (00:42-00:48, 01:46-01:52, 04:18)
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showViewModeSubMenu by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("متوسط") } // القائمة | صغير | متوسط | كبير
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var currentPageNumber by remember { mutableIntStateOf(1) }

    // Long-press save manga dialog (02:21 - 02:34)
    var pendingSaveWork by remember { mutableStateOf<MangaWork?>(null) }

    var liveWorks by remember(site.id, site.url) { mutableStateOf(works) }
    var isLoadingCatalog by remember(site.id, site.url) { mutableStateOf(true) }
    var requiresCloudflare by remember(site.id, site.url) { mutableStateOf(false) }
    var errorMessage by remember(site.id, site.url) { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Latest Updates state for this site
    var siteLatestUpdates by remember(site.id, site.url) { mutableStateOf(cachedSiteUpdates) }
    var isLoadingSiteUpdates by remember(site.id, site.url) { mutableStateOf(false) }
    var siteUpdatesPage by remember(site.id, site.url) { mutableIntStateOf(1) }
    var siteUpdatesHasMore by remember(site.id, site.url) { mutableStateOf(false) }
    var siteUpdatesError by remember(site.id, site.url) { mutableStateOf<String?>(null) }
    var siteUpdatesShowingCachedOffline by remember(site.id, site.url) { mutableStateOf(false) }
    var siteUpdatesRefreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(cachedSiteUpdates) {
        if (siteLatestUpdates.isEmpty() && cachedSiteUpdates.isNotEmpty()) {
            siteLatestUpdates = cachedSiteUpdates
        }
    }

    val targetCatalogUrl = remember(site.url, submittedSearchQuery, selectedGenre, currentPageNumber) {
        val base = LiveMangaSiteScraper.buildCatalogTargetUrl(
            site = site,
            searchQuery = submittedSearchQuery,
            selectedGenre = selectedGenre
        )
        if (currentPageNumber > 1 && !base.contains("page=")) {
            if (base.contains("?")) "$base&page=$currentPageNumber" else "${base.trimEnd('/')}/page/$currentPageNumber/"
        } else {
            base
        }
    }

    // Fetch live manga list directly from the added website
    LaunchedEffect(site.id, site.url, submittedSearchQuery, selectedGenre, currentPageNumber, refreshTrigger) {
        isLoadingCatalog = true
        errorMessage = null
        val result = LiveMangaSiteScraper.fetchSiteCatalog(
            site = site,
            searchQuery = submittedSearchQuery,
            selectedGenre = selectedGenre
        )
        if (result.works.isNotEmpty()) {
            liveWorks = result.works
            requiresCloudflare = false
            isLoadingCatalog = false
        } else if (result.requiresCloudflare) {
            requiresCloudflare = true
            errorMessage = result.errorMessage
            isLoadingCatalog = false
        } else {
            errorMessage = result.errorMessage
            kotlinx.coroutines.delay(1400)
            isLoadingCatalog = false
        }
    }

    // Fetch Latest Updates from the site source automatically when opened or refreshed
    LaunchedEffect(site.id, site.url, siteUpdatesPage, siteUpdatesRefreshTrigger, activeSectionTab) {
        if (activeSectionTab != "UPDATES" && siteUpdatesRefreshTrigger == 0 && siteUpdatesPage == 1) {
            return@LaunchedEffect
        }
        isLoadingSiteUpdates = true
        siteUpdatesError = null
        val result = LiveMangaSiteScraper.fetchLatestUpdates(site = site, page = siteUpdatesPage)
        if (result.updates.isNotEmpty()) {
            val merged = if (siteUpdatesPage == 1) {
                result.updates
            } else {
                (siteLatestUpdates + result.updates).distinctBy {
                    it.chapterUrl.trim().lowercase().trimEnd('/')
                }
            }
            siteLatestUpdates = merged
            siteUpdatesHasMore = result.hasNextPage
            siteUpdatesShowingCachedOffline = false
            siteUpdatesError = null
            onCacheSiteUpdates(merged)
        } else {
            if (cachedSiteUpdates.isNotEmpty() && siteLatestUpdates.isEmpty()) {
                siteLatestUpdates = cachedSiteUpdates
                siteUpdatesShowingCachedOffline = true
            } else if (siteLatestUpdates.isNotEmpty()) {
                siteUpdatesShowingCachedOffline = true
            }
            siteUpdatesError = result.errorMessage ?: "تعذر جلب آخر التحديثات من ${site.name}"
        }
        isLoadingSiteUpdates = false
    }

    // Background WebView DOM extractor for JS-rendered or Cloudflare-cookie sites
    HiddenSiteWebViewBridge(
        targetUrl = targetCatalogUrl,
        jsExtractorScript = LiveMangaSiteScraper.JS_EXTRACT_CATALOG,
        reloadTrigger = refreshTrigger,
        onResultJson = { rawJson ->
            val domResult = LiveMangaSiteScraper.parseCatalogFromDomJson(rawJson, site)
            if (domResult.works.isNotEmpty()) {
                liveWorks = domResult.works
                requiresCloudflare = false
                errorMessage = null
                isLoadingCatalog = false
            } else if (domResult.requiresCloudflare && liveWorks.isEmpty()) {
                requiresCloudflare = true
                isLoadingCatalog = false
            }
        }
    )

    val displayedWorks = remember(liveWorks, searchQuery, sortMode, selectedStatusFilter) {
        val byLocalSearch = if (searchQuery.isBlank()) {
            liveWorks
        } else {
            liveWorks.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }
        }
        val byStatus = if (selectedStatusFilter == null) {
            byLocalSearch
        } else {
            byLocalSearch.filter { it.status.contains(selectedStatusFilter!!, ignoreCase = true) }
        }
        when (sortMode) {
            "أبجدي", "الأبجدية" -> byStatus.sortedBy { it.title }
            "الاكثر مشاهدة", "الأكثر شعبية", "الشائع" -> byStatus.reversed()
            else -> byStatus
        }
    }

    val displayedSiteUpdates = remember(siteLatestUpdates, searchQuery) {
        if (searchQuery.isBlank()) {
            siteLatestUpdates
        } else {
            siteLatestUpdates.filter {
                it.workTitle.contains(searchQuery, ignoreCase = true) ||
                    it.chapterTitle.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("site_catalog_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar (00:07 - 00:48): Menu ☰ on Right, Search 🔍 + Protection 🛡️ + More ⋮ on Left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("site_catalog_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "القائمة الجانبية",
                        tint = TextPrimaryWhite
                    )
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.length >= 3 || it.isEmpty()) {
                                submittedSearchQuery = it
                            }
                        },
                        placeholder = { Text("ابحث في ${site.name}...", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PeriwinklePrimary,
                            unfocusedBorderColor = Color(0xFF454957),
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .testTag("site_catalog_search_input")
                    )
                    IconButton(onClick = {
                        if (searchQuery.isNotEmpty()) {
                            searchQuery = ""
                            submittedSearchQuery = ""
                        } else {
                            showSearchBar = false
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق البحث", tint = TextPrimaryWhite)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (activeSectionTab == "UPDATES") {
                                    siteUpdatesPage = 1
                                    siteUpdatesRefreshTrigger++
                                } else {
                                    refreshTrigger++
                                }
                            },
                            modifier = Modifier.testTag("site_catalog_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث",
                                tint = TextPrimaryWhite
                            )
                        }
                        IconButton(
                            onClick = { showSearchBar = true },
                            modifier = Modifier.testTag("site_catalog_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = TextPrimaryWhite
                            )
                        }
                        IconButton(
                            onClick = { showProtectionDialog = true },
                            modifier = Modifier.testTag("site_protection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "الحماية وتخطي Cloudflare",
                                tint = if (requiresCloudflare) Color(0xFFFFB74D) else PeriwinklePrimary
                            )
                        }
                        Box {
                            IconButton(
                                onClick = {
                                    showViewModeSubMenu = false
                                    showOverflowMenu = true
                                },
                                modifier = Modifier.testTag("site_more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "خيارات العرض والموقع",
                                    tint = TextPrimaryWhite
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                containerColor = AppDialogSurface
                            ) {
                                if (!showViewModeSubMenu) {
                                    DropdownMenuItem(
                                        text = { Text("نمط العرض", color = TextPrimaryWhite) },
                                        leadingIcon = { Icon(Icons.Default.GridView, null, tint = PeriwinklePrimary) },
                                        onClick = { showViewModeSubMenu = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("المراجع", color = TextPrimaryWhite) },
                                        leadingIcon = { Icon(Icons.Default.Book, null, tint = PeriwinklePrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showBookmarksDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("توجه الى", color = TextPrimaryWhite) },
                                        leadingIcon = { Icon(Icons.Default.Apps, null, tint = PeriwinklePrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showJumpToPageDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("التنزيلات", color = TextPrimaryWhite) },
                                        leadingIcon = { Icon(Icons.Default.Download, null, tint = PeriwinklePrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onOpenDownloadsScreen()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("الاعدادات", color = TextPrimaryWhite) },
                                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = PeriwinklePrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onOpenSettingsScreen()
                                        }
                                    )
                                } else {
                                    listOf("القائمة", "صغير", "متوسط", "كبير").forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (viewMode == mode) "✓ $mode" else mode,
                                                    color = if (viewMode == mode) PeriwinklePrimary else TextPrimaryWhite,
                                                    fontWeight = if (viewMode == mode) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewMode = mode
                                                showViewModeSubMenu = false
                                                showOverflowMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Site Title + "آخر الاضافات" Sort/Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = site.name.substringAfterLast("- ").trim().ifBlank { site.name },
                    color = TextPrimaryWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (activeSectionTab == "CATALOG") {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showFilterSheetDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("site_sort_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "الفلتر",
                            tint = Color(0xFFD5DAE6),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sortMode,
                            color = Color(0xFFD5DAE6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Section Mode Selector: "الأعمال والكتالوج" vs "آخر التحديثات من المصدر"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isCatalogSelected = activeSectionTab == "CATALOG"
                val isUpdatesSelected = activeSectionTab == "UPDATES"

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCatalogSelected) PeriwinklePrimary else Color(0xFF1F232E),
                    border = BorderStroke(1.dp, if (isCatalogSelected) PeriwinklePrimary else Color(0xFF454A59)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeSectionTab = "CATALOG" }
                        .testTag("site_tab_catalog")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "الأعمال والكتالوج",
                            color = if (isCatalogSelected) PeriwinkleOnPrimary else TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isUpdatesSelected) PeriwinklePrimary else Color(0xFF1F232E),
                    border = BorderStroke(1.dp, if (isUpdatesSelected) PeriwinklePrimary else Color(0xFF454A59)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeSectionTab = "UPDATES" }
                        .testTag("site_tab_latest_updates")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "آخر التحديثات",
                            color = if (isUpdatesSelected) PeriwinkleOnPrimary else TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (activeSectionTab == "UPDATES") {
                // Site Latest Updates Section
                if (siteUpdatesShowingCachedOffline) {
                    Surface(
                        color = Color(0xFF282E42),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "يتم عرض آخر التحديثات المحفوظة محليًا (تعذر الاتصال المباشر بالمصدر حاليًا)",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (isLoadingSiteUpdates && displayedSiteUpdates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PeriwinklePrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "جارٍ جلب آخر الفصول المحدثة من ${site.name}...",
                                color = TextSecondaryGrey,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (displayedSiteUpdates.isEmpty()) {
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
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = PeriwinklePrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = siteUpdatesError ?: "لم يتم العثور على تحديثات فصول حديثة في هذا المصدر حاليًا",
                                color = TextPrimaryWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    siteUpdatesPage = 1
                                    siteUpdatesRefreshTrigger++
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PeriwinklePrimary,
                                    contentColor = PeriwinkleOnPrimary
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("site_latest_updates_list"),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedSiteUpdates, key = { it.id }) { updateItem ->
                            LatestUpdateItemCard(
                                item = updateItem,
                                onOpenWorkDetails = { onOpenUpdateWorkDetails(updateItem) },
                                onOpenChapter = { onOpenUpdateChapter(updateItem) }
                            )
                        }
                        if (siteUpdatesHasMore) {
                            item {
                                OutlinedButton(
                                    onClick = { siteUpdatesPage++ },
                                    enabled = !isLoadingSiteUpdates,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .testTag("site_updates_load_more_button")
                                ) {
                                    if (isLoadingSiteUpdates) {
                                        CircularProgressIndicator(
                                            color = PeriwinklePrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = if (isLoadingSiteUpdates) "جارٍ تحميل المزيد..." else "تحميل المزيد من التحديثات",
                                        color = PeriwinklePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
            // Horizontal Scrollable Genre / Category Chips Row (00:07 - 00:15)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MangaCatalogData.siteCategoryChips) { chip ->
                    val isCategoriesButton = chip == "التصنيفات"
                    val isSelected = selectedGenre == chip
                    val containerColor = when {
                        isSelected -> PeriwinklePrimary
                        isCategoriesButton -> Color(0xFF1F232E)
                        else -> Color.Transparent
                    }
                    val textColor = if (isSelected) PeriwinkleOnPrimary else TextPrimaryWhite
                    val borderColor = if (isSelected) PeriwinklePrimary else Color(0xFF4E5363)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clickable {
                                if (isCategoriesButton) {
                                    showFilterSheetDialog = true
                                } else {
                                    selectedGenre = if (selectedGenre == chip) null else chip
                                }
                            }
                            .testTag("genre_chip_$chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCategoriesButton) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = chip,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Content Area
            if (isLoadingCatalog && displayedWorks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "جار التحميل من الموقع",
                        tint = Color(0xFF8C92A4),
                        modifier = Modifier.size(64.dp)
                    )
                }
            } else if (displayedWorks.isEmpty()) {
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
                            imageVector = if (requiresCloudflare) Icons.Default.Security else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = PeriwinklePrimary,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (requiresCloudflare) {
                                "الموقع محمي بواسطة Cloudflare — اضغط لتفعيل الحماية وجلب المانجا مباشرة"
                            } else {
                                errorMessage ?: "جاري جلب قائمة المانجا من الموقع أو لم يتم العثور على نتائج"
                            },
                            color = TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showProtectionDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PeriwinklePrimary,
                                    contentColor = PeriwinkleOnPrimary
                                )
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تخطي الحماية (Cloudflare)", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { refreshTrigger++ },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryWhite)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة المحاولة")
                            }
                        }
                        TextButton(onClick = { onOpenSiteInWebView(site) }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = PeriwinklePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح الموقع في المتصفح الداخلي", color = PeriwinklePrimary)
                        }
                    }
                }
            } else if (viewMode == "القائمة") {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("site_works_list"),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedWorks.size, key = { idx -> "${displayedWorks[idx].id}_$idx" }) { index ->
                        val work = displayedWorks[index]
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onSelectWork(work) },
                                    onLongClick = { pendingSaveWork = work }
                                )
                                .testTag("manga_card_${work.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MangaCoverArtwork(
                                    work = work,
                                    modifier = Modifier
                                        .width(86.dp)
                                        .height(120.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = work.title,
                                        color = TextPrimaryWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (work.genres.isNotEmpty()) work.genres.joinToString(" ، ") else site.name,
                                        color = TextSecondaryGrey,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row {
                                            repeat(5) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFF5A6075),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { pendingSaveWork = work },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("save_work_btn_${work.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "حفظ المانجا",
                                                    tint = PeriwinklePrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            TextButton(onClick = { onSelectWork(work) }) {
                                                Text("إقرأ", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val minColSize = when (viewMode) {
                    "صغير" -> 64.dp
                    "كبير" -> 150.dp
                    else -> 106.dp
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = minColSize),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("site_works_grid"),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayedWorks.size, key = { idx -> "${displayedWorks[idx].id}_$idx" }) { index ->
                        val work = displayedWorks[index]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onSelectWork(work) },
                                    onLongClick = { pendingSaveWork = work }
                                )
                                .testTag("manga_card_${work.id}")
                        ) {
                            MangaCoverArtwork(
                                work = work,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.71f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = work.title,
                                color = TextPrimaryWhite,
                                fontSize = if (viewMode == "صغير") 10.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            } // end of CATALOG branch
        }

        // Bottom-Left Floating "متابعة" Button
        if (showContinueReadingButton) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2A324B),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 18.dp)
                    .clickable { onContinueReadingClick() }
                    .testTag("site_continue_reading_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = Color(0xFFD5DEFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1E2B))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "متابعة",
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Long-press Save Whole Manga Dialog matching 02:23 - 02:34:
    // "سيتم حفظ المانجا (جميع X الفصول)." -> حسنا / إلغاء
    pendingSaveWork?.let { workToSave ->
        val chapterCount = workToSave.chapters.size.coerceAtLeast(1)
        AlertDialog(
            onDismissRequest = { pendingSaveWork = null },
            containerColor = AppDialogSurface,
            text = {
                Text(
                    text = "سيتم حفظ المانجا (جميع $chapterCount الفصول).",
                    color = TextPrimaryWhite,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingSaveWork = null
                        onSaveWholeMangaToStorage(workToSave)
                    },
                    modifier = Modifier.testTag("confirm_save_whole_manga_btn")
                ) {
                    Text("حسنا", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSaveWork = null }) {
                    Text("إلغاء", color = PeriwinklePrimary)
                }
            }
        )
    }

    // Exact Filter Sheet Dialog from 01:38-01:43 & 05:56-06:00:
    // الفلتر + مسح التحديد + تصفح حسب + الحالة + التصنيفات
    if (showFilterSheetDialog) {
        val sortOptions = listOf("آخر الاضافات", "الاكثر مشاهدة", "جديد", "أبجدي", "التقييم", "الشائع")
        val statusOptions = listOf("مكتمل", "مستمر", "ملغى", "متوقف")
        AlertDialog(
            onDismissRequest = { showFilterSheetDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الفلتر", color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    TextButton(
                        onClick = {
                            selectedGenre = null
                            selectedStatusFilter = null
                            sortMode = "آخر الاضافات"
                        }
                    ) {
                        Text("مسح التحديد", color = TextSecondaryGrey, fontSize = 13.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تصفح حسب", color = TextSecondaryGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    sortOptions.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { option ->
                                val active = sortMode == option
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) Color(0xFF2D3652) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (active) PeriwinklePrimary else Color(0xFF454A59)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { sortMode = option }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (active) {
                                            Icon(Icons.Default.Check, null, tint = PeriwinklePrimary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(option, color = TextPrimaryWhite, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = AppDividerColor)
                    Text("الحالة", color = TextSecondaryGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statusOptions.forEach { st ->
                            val active = selectedStatusFilter == st
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (active) Color(0xFF2D3652) else Color.Transparent,
                                border = BorderStroke(1.dp, if (active) PeriwinklePrimary else Color(0xFF454A59)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedStatusFilter = if (active) null else st
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(st, color = TextPrimaryWhite, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = AppDividerColor)
                    Text("التصنيفات", color = TextSecondaryGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    MangaCatalogData.siteCategoryChips.drop(1).chunked(3).forEach { rowChips ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowChips.forEach { genre ->
                                val active = selectedGenre == genre
                                OutlinedButton(
                                    onClick = {
                                        selectedGenre = if (active) null else genre
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (active) PeriwinklePrimary else Color.Transparent,
                                        contentColor = if (active) PeriwinkleOnPrimary else TextPrimaryWhite
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(genre, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterSheetDialog = false }) {
                    Text("تطبيق", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // "المراجع" Dialog (01:47-01:49)
    if (showBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            containerColor = AppDialogSurface,
            title = { Text("المراجع", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("السجل فارغ.", color = TextSecondaryGrey, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarksDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }

    // "توجه الى" Page Jump Dialog (01:51-01:54)
    if (showJumpToPageDialog) {
        AlertDialog(
            onDismissRequest = { showJumpToPageDialog = false },
            containerColor = AppDialogSurface,
            title = { Text("توجه الى", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items((1..20).toList()) { pageNum ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentPageNumber = pageNum
                                    showJumpToPageDialog = false
                                    onShowToast("الانتقال إلى الصفحة $pageNum")
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$pageNum",
                                color = if (currentPageNumber == pageNum) PeriwinklePrimary else TextPrimaryWhite,
                                fontSize = 16.sp,
                                fontWeight = if (currentPageNumber == pageNum) FontWeight.Bold else FontWeight.Normal
                            )
                            if (currentPageNumber == pageNum) {
                                Icon(Icons.Default.Check, null, tint = PeriwinklePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        HorizontalDivider(color = AppDividerColor)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJumpToPageDialog = false }) {
                    Text("إلغاء", color = PeriwinklePrimary)
                }
            }
        )
    }

    // Interactive Cloudflare Protection & Live DOM Verification Modal
    if (showProtectionDialog) {
        CloudflareProtectionDialog(
            url = targetCatalogUrl,
            siteName = site.name,
            jsExtractorScript = LiveMangaSiteScraper.JS_EXTRACT_CATALOG,
            onExtractedDomJson = { rawJson ->
                val domResult = LiveMangaSiteScraper.parseCatalogFromDomJson(rawJson, site)
                if (domResult.works.isNotEmpty()) {
                    liveWorks = domResult.works
                    requiresCloudflare = false
                    errorMessage = null
                }
            },
            onDismissAndRefresh = {
                showProtectionDialog = false
                refreshTrigger++
                onShowToast("تم تحديث جلسة الحماية وجلب المانجا من الموقع")
            }
        )
    }
}

@Composable
fun LatestUpdateItemCard(
    item: MangaLatestUpdateItem,
    onOpenWorkDetails: () -> Unit,
    onOpenChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coverPreviewWork = remember(item.workId, item.workTitle, item.coverImageUrl, item.sourceName, item.sourceUrl) {
        MangaWork(
            id = item.workId,
            title = item.workTitle,
            siteName = item.sourceName,
            siteUrl = item.sourceUrl,
            siteIconKey = item.sourceIconKey,
            mangaUrl = item.workUrl,
            coverImageUrl = item.coverImageUrl
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("latest_update_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                MangaCoverArtwork(
                    work = coverPreviewWork,
                    modifier = Modifier
                        .width(78.dp)
                        .height(110.dp)
                        .clickable { onOpenWorkDetails() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Source badge + Publish date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF283048),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.sourceName,
                                color = PeriwinklePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = item.displayDateOrFallback,
                            color = if (item.publishDateText.isNotBlank()) Color(0xFF81C784) else TextSecondaryGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Work Title
                    Text(
                        text = item.workTitle,
                        color = TextPrimaryWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onOpenWorkDetails() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Chapter Number + Chapter Title
                    val chapterDisplayLine = remember(item.displayChapterNumber, item.chapterTitle) {
                        val numPart = "الفصل ${item.displayChapterNumber}"
                        if (item.chapterTitle.isNotBlank() &&
                            !item.chapterTitle.equals(numPart, ignoreCase = true)
                        ) {
                            "$numPart • ${item.chapterTitle}"
                        } else {
                            numPart
                        }
                    }

                    Text(
                        text = chapterDisplayLine,
                        color = PeriwinklePrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: "تفاصيل العمل" and "فتح الفصل"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenWorkDetails,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF454A59)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("update_open_details_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = TextPrimaryWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تفاصيل العمل",
                        color = TextPrimaryWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onOpenChapter,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PeriwinklePrimary,
                        contentColor = PeriwinkleOnPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("update_open_chapter_btn_${item.id}")
                ) {
                    Text(
                        text = "فتح الفصل ${item.displayChapterNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

