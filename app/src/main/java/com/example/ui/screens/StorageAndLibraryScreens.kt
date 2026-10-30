package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.data.MangaLatestUpdateItem
import com.example.data.MangaWork
import com.example.data.SourceUpdateSyncStatus
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.DownloadedWorkEntity
import com.example.data.local.FavoriteCategoryEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.SavedWorkEntity
import com.example.data.local.SiteEntity
import com.example.ui.components.MangaCoverArtwork
import com.example.ui.components.SiteIconBadge
import com.example.ui.theme.AppCardSurface
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.viewmodel.MainViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StorageLibraryScreen(
    savedWorks: List<SavedWorkEntity>,
    downloadedWorks: List<DownloadedWorkEntity> = emptyList(),
    showContinueReadingButton: Boolean,
    onOpenDrawer: () -> Unit,
    onImportUri: (Uri) -> Unit,
    onOpenWork: (SavedWorkEntity) -> Unit,
    onSelectMangaWork: (MangaWork) -> Unit,
    onOpenDownloadedWork: (DownloadedWorkEntity) -> Unit = {},
    onDeleteDownloadedWork: (String) -> Unit = {},
    onDeleteWork: (SavedWorkEntity) -> Unit,
    onContinueReadingClick: () -> Unit,
    onOpenDownloadsScreen: () -> Unit,
    onOpenStorageSettings: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("آخر الاضافات") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showViewModeSubMenu by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("متوسط") } // القائمة | صغير | متوسط | كبير
    var showBookmarksDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportUri(uri)
        }
    }

    val combinedWorks = remember(downloadedWorks, savedWorks, searchQuery, sortMode) {
        val offlineItems = downloadedWorks.map { entity ->
            val remoteCover = entity.remoteCoverUrl.ifBlank { entity.coverUrl }
            MangaWork(
                id = entity.workId,
                sourceId = entity.sourceId,
                title = entity.title,
                subtitle = entity.subtitle,
                siteName = entity.sourceName.ifBlank { "التخزين" },
                siteUrl = entity.sourceUrl,
                siteIconKey = entity.siteIconKey,
                mangaUrl = entity.mangaUrl,
                coverImageUrl = remoteCover,
                coverUrl = remoteCover,
                rating = entity.rating,
                status = entity.status,
                type = entity.type,
                author = entity.author,
                genres = entity.genresCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                synopsis = entity.synopsis,
                localCoverPath = entity.localCoverPath,
                isFromLocalStorage = true,
                storageSizeBytes = entity.totalStorageBytes,
                savedChaptersCount = entity.savedChaptersCount
            ) to entity
        }
        val importedItems = savedWorks
            .filter { it.fileType == "IMPORT" || it.fileType == "PAGE" }
            .map { saved ->
                MangaWork(
                    id = "saved_${saved.id}",
                    title = saved.title,
                    subtitle = "",
                    siteName = "التخزين",
                    siteUrl = saved.sourceUrl.substringBefore("/series").ifBlank { saved.sourceUrl },
                    siteIconKey = "globe",
                    mangaUrl = saved.sourceUrl,
                    coverUrl = "",
                    isFromLocalStorage = true,
                    storageSizeBytes = saved.fileSizeBytes
                ) to (null as DownloadedWorkEntity?)
            }

        val all = offlineItems + importedItems
        val matching = if (searchQuery.isBlank()) {
            all
        } else {
            all.filter { (work, _) ->
                work.title.contains(searchQuery, ignoreCase = true) ||
                    work.siteName.contains(searchQuery, ignoreCase = true)
            }
        }
        when (sortMode) {
            "الاسم", "أبجدي" -> matching.sortedBy { it.first.title }
            "الحجم" -> matching.sortedByDescending { it.first.storageSizeBytes }
            else -> matching.sortedByDescending { it.second?.updatedAt ?: 0L }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("storage_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar (00:01 & 07:13): Menu ☰ on Right, Search 🔍 + More ⋮ on Left
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
                    modifier = Modifier.testTag("open_drawer_button")
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
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث في التخزين...", fontSize = 13.sp) },
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
                    )
                    IconButton(onClick = {
                        searchQuery = ""
                        showSearchBar = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق البحث", tint = TextPrimaryWhite)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSearchBar = true },
                            modifier = Modifier.testTag("storage_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = TextPrimaryWhite
                            )
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    showViewModeSubMenu = false
                                    showMoreMenu = true
                                },
                                modifier = Modifier.testTag("storage_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "المزيد",
                                    tint = TextPrimaryWhite
                                )
                            }
                            // Exact 5-item menu from 07:13-07:15: نمط العرض، المراجع، استيراد، التنزيلات، الاعدادات
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
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
                                            showMoreMenu = false
                                            showBookmarksDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("استيراد", color = TextPrimaryWhite) },
                                        leadingIcon = { Icon(Icons.Default.FolderOpen, null, tint = PeriwinklePrimary) },
                                        onClick = {
                                            showMoreMenu = false
                                            filePickerLauncher.launch(arrayOf("*/*"))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("التنزيلات", color = TextPrimaryWhite) },
                                        leadingIcon = { Icon(Icons.Default.Download, null, tint = PeriwinklePrimary) },
                                        onClick = {
                                            showMoreMenu = false
                                            onOpenDownloadsScreen()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("الاعدادات", color = TextPrimaryWhite) },
                                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = PeriwinklePrimary) },
                                        onClick = {
                                            showMoreMenu = false
                                            onOpenStorageSettings()
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
                                                showMoreMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Title Row: "التخزين" on the right, "آخر الاضافات" filter on the left (00:01 & 03:09)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "التخزين",
                    color = TextPrimaryWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("storage_sort_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "ترتيب",
                            tint = Color(0xFFD5DAE6),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sortMode,
                            color = Color(0xFFD5DAE6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = AppDialogSurface
                    ) {
                        listOf("آخر الاضافات", "أبجدي", "الحجم").forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode, color = TextPrimaryWhite) },
                                onClick = {
                                    sortMode = mode
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Empty State matching 00:06: (-_-) + لم تحفظ اي عمل في التخزين. + استيراد pill button
            if (combinedWorks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "(-_-)",
                            color = TextPrimaryWhite,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "لم تحفظ اي عمل في التخزين.",
                            color = Color(0xFFD5DAE6),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFF4C5264)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PeriwinklePrimary),
                            modifier = Modifier.testTag("storage_empty_import_button")
                        ) {
                            Text(
                                text = "استيراد",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else if (viewMode == "القائمة") {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("storage_manga_list"),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(combinedWorks, key = { it.first.id }) { (mangaWork, downloadedEntity) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (downloadedEntity != null) {
                                        onOpenDownloadedWork(downloadedEntity)
                                    } else {
                                        onSelectMangaWork(mangaWork)
                                    }
                                }
                                .testTag("storage_manga_card_${mangaWork.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MangaCoverArtwork(
                                    work = mangaWork,
                                    modifier = Modifier
                                        .width(82.dp)
                                        .height(114.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mangaWork.title,
                                        color = TextPrimaryWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (mangaWork.genres.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = mangaWork.genres.joinToString(" ، "),
                                            color = TextSecondaryGrey,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val savedChaptersLabel = if (downloadedEntity != null) {
                                        "${downloadedEntity.savedChaptersCount} فصول محفوظة • ${MainViewModel.formatBytesArabic(mangaWork.storageSizeBytes)}"
                                    } else {
                                        MainViewModel.formatBytesArabic(mangaWork.storageSizeBytes)
                                    }
                                    Text(
                                        text = savedChaptersLabel,
                                        color = Color(0xFF9AC5F4),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "جاهز بدون إنترنت",
                                            color = Color(0xFF81C784),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (downloadedEntity != null) {
                                                IconButton(
                                                    onClick = { onDeleteDownloadedWork(downloadedEntity.workId) },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("storage_delete_work_${mangaWork.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف التنزيل",
                                                        tint = Color(0xFFFF8A80),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    if (downloadedEntity != null) {
                                                        onOpenDownloadedWork(downloadedEntity)
                                                    } else {
                                                        onSelectMangaWork(mangaWork)
                                                    }
                                                }
                                            ) {
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
                val minCellSize = when (viewMode) {
                    "صغير" -> 66.dp
                    "كبير" -> 150.dp
                    else -> 104.dp
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = minCellSize),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("storage_manga_grid"),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 96.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(combinedWorks, key = { it.first.id }) { (mangaWork, downloadedEntity) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (downloadedEntity != null) {
                                        onOpenDownloadedWork(downloadedEntity)
                                    } else {
                                        onSelectMangaWork(mangaWork)
                                    }
                                }
                                .testTag("storage_manga_card_${mangaWork.id}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                MangaCoverArtwork(
                                    work = mangaWork,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.71f),
                                    showArabicTitleBanner = false
                                )
                                if (downloadedEntity != null && downloadedEntity.savedChaptersCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xD9151924),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "${downloadedEntity.savedChaptersCount} فصول",
                                            color = PeriwinklePrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = mangaWork.title,
                                color = TextPrimaryWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp)
                            )
                            if (downloadedEntity != null) {
                                Text(
                                    text = "${downloadedEntity.savedChaptersCount} فصول محفوظة",
                                    color = TextSecondaryGrey,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Exact Bottom-Left "متابعة" Floating Button from 00:00, 01:39, 03:09
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
                    .testTag("continue_reading_button")
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
}

@Composable
fun DownloadsScreen(
    tasks: List<DownloadTaskEntity>,
    isAllPaused: Boolean = false,
    onBackClick: () -> Unit,
    onStartNewDownload: (String, String) -> Unit,
    onDeleteTask: (DownloadTaskEntity) -> Unit,
    onTogglePauseTask: (DownloadTaskEntity) -> Unit = {},
    onCancelTask: (DownloadTaskEntity) -> Unit = {},
    onRetryTask: (DownloadTaskEntity) -> Unit = {},
    onOpenDownloadedTask: (DownloadTaskEntity) -> Unit = {},
    onTogglePauseAll: () -> Unit = {},
    onCancelAll: () -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val dfPercent = remember { DecimalFormat("0.0") }

    val activeTasks = remember(tasks) {
        tasks.filter { it.status == "DOWNLOADING" || it.status == "QUEUED" || it.status == "PAUSED" }
    }
    val finishedTasks = remember(tasks) {
        tasks.filter { it.status != "DOWNLOADING" && it.status != "QUEUED" && it.status != "PAUSED" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("downloads_screen")
    ) {
        // Exact Top Bar from 02:40 - 03:08 & 07:16 - 08:01:
        // Right: Back arrow + "التنزيلات"
        // Left: "إيقاف ⏸" button + "⋮" menu ("إلغاء الكل", "حذف السجل")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("downloads_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = TextPrimaryWhite
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "التنزيلات",
                    color = TextPrimaryWhite,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onTogglePauseAll,
                    modifier = Modifier.testTag("downloads_pause_all_button")
                ) {
                    Text(
                        text = if (isAllPaused) "استئناف" else "إيقاف",
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isAllPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = TextPrimaryWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.testTag("downloads_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات التنزيلات",
                            tint = TextPrimaryWhite
                        )
                    }
                    // Exact menu from 03:00: إلغاء الكل، حذف السجل
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        containerColor = AppDialogSurface
                    ) {
                        DropdownMenuItem(
                            text = { Text("إلغاء الكل", color = TextPrimaryWhite) },
                            onClick = {
                                showMoreMenu = false
                                onCancelAll()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف السجل", color = TextPrimaryWhite) },
                            onClick = {
                                showMoreMenu = false
                                onClearHistory()
                            }
                        )
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "(-_-)",
                        color = TextPrimaryWhite,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد عمليات تنزيل حاليًا",
                        color = TextSecondaryGrey,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("downloads_list"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "قيد التنفيذ",
                            color = PeriwinklePrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    items(activeTasks, key = { it.id }) { task ->
                        DownloadTaskCard(
                            task = task,
                            dfPercent = dfPercent,
                            onTogglePause = { onTogglePauseTask(task) },
                            onCancel = { onCancelTask(task) },
                            onRetry = { onRetryTask(task) },
                            onOpen = { onOpenDownloadedTask(task) },
                            onDelete = { onDeleteTask(task) }
                        )
                    }
                }

                if (finishedTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "الآن",
                            color = PeriwinklePrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    items(finishedTasks, key = { it.id }) { task ->
                        DownloadTaskCard(
                            task = task,
                            dfPercent = dfPercent,
                            onTogglePause = { onTogglePauseTask(task) },
                            onCancel = { onCancelTask(task) },
                            onRetry = { onRetryTask(task) },
                            onOpen = { onOpenDownloadedTask(task) },
                            onDelete = { onDeleteTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadTaskCard(
    task: DownloadTaskEntity,
    dfPercent: DecimalFormat,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = task.status == "DOWNLOADING" || task.status == "QUEUED" || task.status == "PAUSED"
    val workForThumb = remember(task.workId, task.title, task.coverUrl, task.coverPath) {
        MangaWork(
            id = task.workId.ifBlank { "task_${task.id}" },
            title = task.title,
            siteName = task.sourceName,
            siteUrl = "",
            siteIconKey = "globe",
            coverImageUrl = task.coverUrl,
            coverUrl = task.coverUrl,
            localCoverPath = task.coverPath
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = task.status == "COMPLETED") { onOpen() }
            .testTag("download_task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            MangaCoverArtwork(
                work = workForThumb,
                modifier = Modifier
                    .width(68.dp)
                    .height(94.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.substringBefore(" - ").ifBlank { task.title },
                    color = TextPrimaryWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (isActive) {
                    val etaOrStatus = when {
                        task.status == "PAUSED" -> "متوقف مؤقتاً"
                        task.etaSeconds > 0 -> "جار التنزيل...  خلال ${task.etaSeconds} ثانية"
                        else -> "جار التنزيل...  عالقة"
                    }
                    Text(
                        text = etaOrStatus,
                        color = TextSecondaryGrey,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val exactPercent = if (task.totalPages > 0) {
                        (task.downloadedPages.toDouble() * 100.0 / task.totalPages.toDouble()).coerceIn(0.0, 100.0)
                    } else {
                        task.progress.toDouble()
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${dfPercent.format(exactPercent)}%",
                            color = TextSecondaryGrey,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${task.downloadedPages}/${task.totalPages.coerceAtLeast(1)}",
                            color = TextSecondaryGrey,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (task.progress / 100f).coerceIn(0f, 1f) },
                        color = PeriwinklePrimary,
                        trackColor = Color(0xFF2B2F3C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Action buttons matching 02:41: إيقاف + إلغاء
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onTogglePause,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF4C5264)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("download_pause_btn_${task.id}")
                        ) {
                            Text(
                                text = if (task.status == "PAUSED") "استئناف" else "إيقاف",
                                color = PeriwinklePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF4C5264)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("download_cancel_btn_${task.id}")
                        ) {
                            Text(
                                text = "إلغاء",
                                color = PeriwinklePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    val statusLine1 = when (task.status) {
                        "COMPLETED" -> "اكتمل التنزيل"
                        "CANCELLED" -> "ألغيت"
                        "INCOMPLETE" -> "غير مكتمل (${task.downloadedPages}/${task.totalPages} صور)"
                        "FAILED" -> "فشل التنزيل"
                        else -> task.status
                    }
                    val statusColor = when (task.status) {
                        "COMPLETED" -> TextSecondaryGrey
                        "INCOMPLETE", "FAILED" -> Color(0xFFFF8A80)
                        else -> TextSecondaryGrey
                    }
                    Text(
                        text = statusLine1,
                        color = statusColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (task.status == "COMPLETED") {
                            "فصل واحد • ${MainViewModel.formatBytesArabic(task.bytesDownloaded)}"
                        } else {
                            task.errorMessage ?: "فصل ${task.chapterNumber}"
                        },
                        color = TextSecondaryGrey,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.status == "COMPLETED") {
                            TextButton(
                                onClick = onOpen,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("download_open_btn_${task.id}")
                            ) {
                                Text("قراءة بدون إنترنت", color = PeriwinklePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (task.status == "FAILED" || task.status == "INCOMPLETE" || task.status == "CANCELLED") {
                            TextButton(
                                onClick = onRetry,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("download_retry_btn_${task.id}")
                            ) {
                                Text("إعادة المحاولة", color = PeriwinklePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("download_delete_btn_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = TextSecondaryGrey,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationsScreen(
    enabledSites: List<SiteEntity>,
    onOpenDrawer: () -> Unit,
    onOpenSite: (SiteEntity) -> Unit,
    onOpenManageSites: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("recommendations_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = TextPrimaryWhite)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("التوصيات", color = TextPrimaryWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        // Exact Empty State from 03:55: ( ⊙д⊙) + لا تتوفر تصنيفات كافية لتوصياتك. يمكنك تصفح «الكل». + إعادة المحاولة
        if (enabledSites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = "( ⊙д⊙)",
                        color = TextPrimaryWhite,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا تتوفر تصنيفات كافية لتوصياتك. يمكنك تصفح «الكل».",
                        color = Color(0xFFD5DAE6),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    OutlinedButton(
                        onClick = onOpenManageSites,
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF4C5264)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PeriwinklePrimary)
                    ) {
                        Text(
                            text = "إعادة المحاولة",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "تصفح التوصيات المباشرة من مواقعك المفعلة:",
                        color = TextSecondaryGrey,
                        fontSize = 14.sp
                    )
                }
                items(enabledSites, key = { it.id }) { site ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSite(site) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SiteIconBadge(iconKey = site.iconKey, siteName = site.name, size = 46.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(site.name, color = TextPrimaryWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(site.url, color = TextSecondaryGrey, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "فتح", tint = PeriwinklePrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesScreen(
    enabledSites: List<SiteEntity>,
    latestUpdates: List<MangaLatestUpdateItem> = emptyList(),
    selectedSourceId: Long? = null,
    sourceStatuses: Map<Long, SourceUpdateSyncStatus> = emptyMap(),
    hasMorePages: Boolean = false,
    errorMessage: String? = null,
    lastSyncedAtMillis: Long = 0L,
    isChecking: Boolean,
    onOpenDrawer: () -> Unit,
    onSelectSourceFilter: (Long?) -> Unit = {},
    onRefreshUpdates: () -> Unit,
    onLoadMoreUpdates: () -> Unit = {},
    onOpenUpdateWorkDetails: (MangaLatestUpdateItem) -> Unit = {},
    onOpenUpdateChapter: (MangaLatestUpdateItem) -> Unit = {},
    onOpenSite: (SiteEntity) -> Unit
) {
    val syncDateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    // Auto-fetch latest updates on first open when enabled sites exist and not yet synced
    LaunchedEffect(enabledSites.size) {
        if (enabledSites.isNotEmpty() && lastSyncedAtMillis == 0L && !isChecking) {
            onRefreshUpdates()
        }
    }

    val filteredUpdates = remember(latestUpdates, selectedSourceId) {
        if (selectedSourceId == null) {
            latestUpdates
        } else {
            latestUpdates.filter { it.sourceId == selectedSourceId }
        }
    }

    val failedSources = remember(sourceStatuses, selectedSourceId) {
        sourceStatuses.values.filter { status ->
            !status.isLoading && status.errorMessage != null &&
                (selectedSourceId == null || status.sourceId == selectedSourceId)
        }
    }

    val latestCachedSyncTime = remember(lastSyncedAtMillis, filteredUpdates) {
        val fromItems = filteredUpdates.maxOfOrNull { it.syncedAt } ?: 0L
        maxOf(lastSyncedAtMillis, fromItems)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("updates_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = TextPrimaryWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("آخر التحديثات", color = TextPrimaryWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    if (latestCachedSyncTime > 0L) {
                        Text(
                            text = "آخر مزامنة: ${syncDateFormat.format(Date(latestCachedSyncTime))}",
                            color = TextSecondaryGrey,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            IconButton(
                onClick = onRefreshUpdates,
                enabled = !isChecking,
                modifier = Modifier.testTag("updates_refresh_button")
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = PeriwinklePrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = TextPrimaryWhite)
                }
            }
        }

        if (enabledSites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "(⊙_⊙)",
                        color = TextPrimaryWhite,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("لا توجد تحديثات فصول جديدة", color = TextPrimaryWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("أضف مواقع الأعمال والمفضلة لمتابعة الفصول الجديدة.", color = TextSecondaryGrey, fontSize = 13.sp)
                }
            }
        } else {
            // Multi-source filter chips ("جميع المصادر" + each enabled site)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedSourceId == null,
                        onClick = { onSelectSourceFilter(null) },
                        label = { Text("جميع المصادر (${latestUpdates.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PeriwinklePrimary,
                            selectedLabelColor = PeriwinkleOnPrimary
                        ),
                        modifier = Modifier.testTag("updates_filter_all")
                    )
                }
                items(enabledSites, key = { it.id }) { site ->
                    val countForSite = latestUpdates.count { it.sourceId == site.id }
                    val status = sourceStatuses[site.id]
                    val labelSuffix = when {
                        status?.isLoading == true -> " • ⏳"
                        status?.errorMessage != null && countForSite == 0 -> " • ⚠️"
                        countForSite > 0 -> " ($countForSite)"
                        else -> ""
                    }
                    FilterChip(
                        selected = selectedSourceId == site.id,
                        onClick = { onSelectSourceFilter(site.id) },
                        label = { Text("${site.name}$labelSuffix") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PeriwinklePrimary,
                            selectedLabelColor = PeriwinkleOnPrimary
                        ),
                        modifier = Modifier.testTag("updates_filter_site_${site.id}")
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("updates_items_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedButton(
                        onClick = onRefreshUpdates,
                        enabled = !isChecking,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("updates_sync_now_button")
                    ) {
                        Icon(Icons.Default.Sync, null, tint = PeriwinklePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isChecking) "جار التحقق من الفصول الجديدة..." else "فحص وتحديث الفصول الآن",
                            color = PeriwinklePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Isolated per-source error notice (if one site failed while others succeeded or cached results exist)
                if (failedSources.isNotEmpty() || errorMessage != null) {
                    item {
                        Surface(
                            color = Color(0xFF2D232A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF8A80).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val msg = errorMessage ?: failedSources.joinToString("\n") {
                                    "• ${it.sourceName}: ${it.errorMessage}"
                                }
                                Text(
                                    text = msg,
                                    color = Color(0xFFFFB4AB),
                                    fontSize = 12.sp
                                )
                                if (filteredUpdates.isNotEmpty() && latestCachedSyncTime > 0L) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "يتم عرض النتائج المتاحة والمحفوظة محليًا من آخر مزامنة ناجحة (${syncDateFormat.format(Date(latestCachedSyncTime))}).",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredUpdates.isNotEmpty()) {
                    items(filteredUpdates, key = { it.id }) { updateItem ->
                        LatestUpdateItemCard(
                            item = updateItem,
                            onOpenWorkDetails = { onOpenUpdateWorkDetails(updateItem) },
                            onOpenChapter = { onOpenUpdateChapter(updateItem) }
                        )
                    }

                    if (hasMorePages) {
                        item {
                            OutlinedButton(
                                onClick = onLoadMoreUpdates,
                                enabled = !isChecking,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("updates_load_more_button")
                            ) {
                                Text(
                                    text = if (isChecking) "جارٍ تحميل المزيد..." else "تحميل المزيد من الفصول المحدثة",
                                    color = PeriwinklePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Show connected sites cards when no chapter updates have been fetched yet
                    items(enabledSites, key = { it.id }) { site ->
                        val siteStatus = sourceStatuses[site.id]
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSite(site) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SiteIconBadge(iconKey = site.iconKey, siteName = site.name, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(site.name, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = when {
                                            siteStatus?.isLoading == true -> "جارٍ فحص أحدث الفصول..."
                                            siteStatus?.errorMessage != null -> siteStatus.errorMessage
                                            else -> "متصل وجاهز لتصفح أحدث الفصول"
                                        },
                                        color = if (siteStatus?.errorMessage != null) Color(0xFFFF8A80) else Color(0xFF81C784),
                                        fontSize = 12.sp
                                    )
                                }
                                Text("فتح", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteEntity>,
    categories: List<FavoriteCategoryEntity>,
    onOpenDrawer: () -> Unit,
    onOpenFavoriteUrl: (String, String) -> Unit,
    onRemoveFavorite: (FavoriteEntity) -> Unit,
    onManageCategories: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("الكل") }

    val filtered = remember(favorites, selectedCategory) {
        if (selectedCategory == "الكل") favorites
        else favorites.filter { it.category == selectedCategory }
    }

    val defaultChips = remember(categories, favorites) {
        val base = listOf("الكل", "قراءة الآن", "قراءة لاحقا")
        val custom = categories.map { it.name }.filter { it !in base }
        base + custom
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("favorites_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = TextPrimaryWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("المفضلة", color = TextPrimaryWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onManageCategories) {
                Text("فئات المفضلة", color = PeriwinklePrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        // Category chips matching 04:00: الكل 0 ، قراءة الآن 0 ، قراءة لاحقا 0
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(defaultChips) { chipName ->
                val count = if (chipName == "الكل") {
                    favorites.size
                } else {
                    favorites.count { it.category == chipName }
                }
                FilterChip(
                    selected = selectedCategory == chipName,
                    onClick = { selectedCategory = chipName },
                    label = { Text("$chipName  $count") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2E354B),
                        selectedLabelColor = TextPrimaryWhite
                    )
                )
            }
        }

        // Exact Empty State from 04:00: (⊙_⊙) + المفضلة فارغة.
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "(⊙_⊙)",
                        color = TextPrimaryWhite,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "المفضلة فارغة.",
                        color = Color(0xFFD5DAE6),
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { fav ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFavoriteUrl(fav.url, fav.title) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fav.title, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(fav.url, color = TextSecondaryGrey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onRemoveFavorite(fav) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFFF6B6B))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    historyItems: List<HistoryEntity>,
    onOpenDrawer: () -> Unit,
    onOpenHistoryUrl: (String, String) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearAllHistory: () -> Unit
) {
    var showConfirmClear by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("history_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = TextPrimaryWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("السجل", color = TextPrimaryWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            if (historyItems.isNotEmpty()) {
                IconButton(
                    onClick = { showConfirmClear = true },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "مسح السجل", tint = Color(0xFFFF6B6B))
                }
            }
        }

        // Exact Empty State from 04:01 & 04:07: (´; ω ; ｀) + السجل فارغ.
        if (historyItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "(´; ω ; ｀)",
                        color = TextPrimaryWhite,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "السجل فارغ.",
                        color = Color(0xFFD5DAE6),
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyItems, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenHistoryUrl(item.url, item.title) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = PeriwinklePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.url, color = TextSecondaryGrey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(dateFormat.format(Date(item.visitedAt)), color = Color(0xFF72788A), fontSize = 11.sp)
                            }
                            IconButton(onClick = { onDeleteHistoryItem(item.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "حذف", tint = TextSecondaryGrey, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            containerColor = AppDialogSurface,
            title = { Text("مسح السجل", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد مسح جميع عناصر سجل التصفح؟", color = TextSecondaryGrey) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClear = false
                        onClearAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE05252), contentColor = Color.White)
                ) {
                    Text("مسح الكل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("إلغاء", color = PeriwinklePrimary)
                }
            }
        )
    }
}
