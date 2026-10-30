package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.AppBackgroundDark
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import java.net.URLEncoder

/**
 * Native Android SourceWebView alias matching Section 2 of the executive specification.
 */
@Composable
fun SourceWebView(
    initialUrl: String,
    currentTitle: String,
    coachmarkAlreadyShown: Boolean,
    onUpdatePageState: (String, String) -> Unit,
    onRecordVisit: (String, String) -> Unit,
    onMarkCoachmarkShown: () -> Unit,
    onAddSiteFromBrowser: (String) -> Unit,
    onToggleFavorite: (String, String) -> Unit,
    onSavePageToStorage: (String, String) -> Unit,
    onOpenCurrentUrlInReader: (String, String) -> Unit = { _, _ -> },
    onShowToast: (String) -> Unit,
    onRecordSearch: () -> Unit,
    onCloseBrowser: () -> Unit
) {
    InAppBrowserScreen(
        initialUrl = initialUrl,
        currentTitle = currentTitle,
        coachmarkAlreadyShown = coachmarkAlreadyShown,
        onUpdatePageState = onUpdatePageState,
        onRecordVisit = onRecordVisit,
        onMarkCoachmarkShown = onMarkCoachmarkShown,
        onAddSiteFromBrowser = onAddSiteFromBrowser,
        onToggleFavorite = onToggleFavorite,
        onSavePageToStorage = onSavePageToStorage,
        onOpenCurrentUrlInReader = onOpenCurrentUrlInReader,
        onShowToast = onShowToast,
        onRecordSearch = onRecordSearch,
        onCloseBrowser = onCloseBrowser
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppBrowserScreen(
    initialUrl: String,
    currentTitle: String,
    coachmarkAlreadyShown: Boolean,
    onUpdatePageState: (String, String) -> Unit,
    onRecordVisit: (String, String) -> Unit,
    onMarkCoachmarkShown: () -> Unit,
    onAddSiteFromBrowser: (String) -> Unit,
    onToggleFavorite: (String, String) -> Unit,
    onSavePageToStorage: (String, String) -> Unit,
    onOpenCurrentUrlInReader: (String, String) -> Unit = { _, _ -> },
    onShowToast: (String) -> Unit,
    onRecordSearch: () -> Unit,
    onCloseBrowser: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var activeUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var activeTitle by remember(currentTitle) { mutableStateOf(currentTitle.ifBlank { initialUrl }) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var showCoachmark by remember { mutableStateOf(false) }

    val isNonGoogleSite = remember(activeUrl) {
        val lower = activeUrl.lowercase()
        lower.startsWith("http") && !lower.contains("google.")
    }

    BackHandler {
        val wv = webViewRef
        if (showSearchBar) {
            showSearchBar = false
        } else if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onCloseBrowser()
        }
    }

    fun navigateToInput(rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return
        onRecordSearch()
        val targetUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://www.google.com/search?q=${URLEncoder.encode(trimmed, "UTF-8")}"
        }
        loadError = null
        activeUrl = targetUrl
        webViewRef?.loadUrl(targetUrl)
        showSearchBar = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundDark)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("in_app_browser_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Browser Top Bar (matching 00:02, 00:21, 00:26)
            Surface(
                color = Color(0xFF12141C),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showSearchBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showSearchBar = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق البحث", tint = TextPrimaryWhite)
                        }
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text("ابحث في Google أو اكتب رابط موقع...", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { navigateToInput(searchInput) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PeriwinklePrimary,
                                unfocusedBorderColor = Color(0xFF454A59),
                                focusedTextColor = TextPrimaryWhite,
                                unfocusedTextColor = TextPrimaryWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("browser_search_input")
                        )
                        IconButton(
                            onClick = { navigateToInput(searchInput) },
                            modifier = Modifier.testTag("browser_search_go_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "انتقال", tint = PeriwinklePrimary)
                        }
                    }
                } else {
                    // Standard Top Bar: Close (X) on Right, Title+URL in Center, Search + 3-dots on Left
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCloseBrowser,
                            modifier = Modifier.testTag("browser_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق المتصفح",
                                tint = TextPrimaryWhite
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    searchInput = activeUrl
                                    showSearchBar = true
                                }
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (activeTitle.isNotBlank() && activeTitle != activeUrl) {
                                Text(
                                    text = activeTitle,
                                    color = TextPrimaryWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = activeUrl,
                                    color = if (activeTitle.isBlank() || activeTitle == activeUrl) TextPrimaryWhite else TextSecondaryGrey,
                                    fontSize = if (activeTitle.isBlank() || activeTitle == activeUrl) 16.sp else 12.sp,
                                    fontWeight = if (activeTitle.isBlank() || activeTitle == activeUrl) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                searchInput = ""
                                showSearchBar = true
                            },
                            modifier = Modifier.testTag("browser_search_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث أو إدخال رابط",
                                tint = TextPrimaryWhite
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("browser_more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "المزيد من الخيارات",
                                    tint = TextPrimaryWhite
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                containerColor = Color(0xFF23262F)
                            ) {
                                if (isNonGoogleSite) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "فتح الصفحة في القارئ المدمج",
                                                color = TextPrimaryWhite,
                                                fontSize = 15.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.MenuBook, null, tint = PeriwinklePrimary)
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            onOpenCurrentUrlInReader(activeTitle, activeUrl)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "فتح في المتصفح",
                                            color = TextPrimaryWhite,
                                            fontSize = 15.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.OpenInBrowser, null, tint = TextSecondaryGrey)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            onShowToast("تعذر فتح المتصفح الخارجي")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "نسخ URL",
                                            color = TextPrimaryWhite,
                                            fontSize = 15.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCopy, null, tint = TextSecondaryGrey)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        clipboardManager.setText(AnnotatedString(activeUrl))
                                        onShowToast("تم نسخ الرابط إلى الحافظة")
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "حفظ الصفحة في التخزين",
                                            color = TextPrimaryWhite,
                                            fontSize = 15.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Download, null, tint = PeriwinklePrimary)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onSavePageToStorage(activeTitle, activeUrl)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "إضافة إلى المفضلة",
                                            color = TextPrimaryWhite,
                                            fontSize = 15.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.BookmarkBorder, null, tint = PeriwinklePrimary)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onToggleFavorite(activeTitle, activeUrl)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "إعادة التحميل",
                                            color = TextPrimaryWhite,
                                            fontSize = 15.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, null, tint = TextSecondaryGrey)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        loadError = null
                                        webViewRef?.reload()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading && loadingProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { loadingProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = PeriwinklePrimary,
                    trackColor = Color.Transparent
                )
            }

            // Content Area: Either Error State or WebView
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadsImagesAutomatically = true
                                // Section 11: Strict WebView security settings
                                allowFileAccess = false
                                allowContentAccess = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                                userAgentString =
                                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    loadError = null
                                    if (!url.isNullOrBlank()) {
                                        activeUrl = url
                                        onUpdatePageState(url, activeTitle)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    try {
                                        CookieManager.getInstance().flush()
                                    } catch (_: Exception) {
                                    }
                                    val finalUrl = url ?: activeUrl
                                    val pageTitle = view?.title ?: ""
                                    activeUrl = finalUrl
                                    if (pageTitle.isNotBlank()) {
                                        activeTitle = pageTitle
                                    }
                                    onUpdatePageState(finalUrl, activeTitle)
                                    onRecordVisit(activeTitle, finalUrl)

                                    if (!finalUrl.lowercase().contains("google.") && !coachmarkAlreadyShown) {
                                        showCoachmark = true
                                        onMarkCoachmarkShown()
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        loadError = error?.description?.toString()
                                            ?: "تعذر تحميل الصفحة. تحقق من اتصالك بالإنترنت أو صحة الرابط."
                                    }
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadingProgress = newProgress
                                    if (newProgress == 100) isLoading = false
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) {
                                        activeTitle = title
                                        onUpdatePageState(activeUrl, title)
                                    }
                                }
                            }
                            loadUrl(initialUrl)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    }
                )

                // Error / Offline Overlay if main frame fails
                if (loadError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppBackgroundDark)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFF687089),
                                modifier = Modifier.size(76.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "تعذر فتح الصفحة داخل المتصفح",
                                color = TextPrimaryWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "قد يكون الموقع محميًا ضد العرض المدمج أو لا يوجد اتصال بالإنترنت حاليًا.",
                                color = TextSecondaryGrey,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        loadError = null
                                        webViewRef?.reload()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PeriwinklePrimary,
                                        contentColor = PeriwinkleOnPrimary
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onAddSiteFromBrowser(activeUrl)
                                    },
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = PeriwinklePrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة الموقع", color = PeriwinklePrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Spotlight Coachmark Overlay from 00:27-00:31 when visiting a site
        AnimatedVisibility(
            visible = showCoachmark && isNonGoogleSite,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showCoachmark = false
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xD97986CB),
                        radius = size.width * 0.78f,
                        center = Offset(x = size.width * 0.25f, y = size.height * 0.88f)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(start = 32.dp, end = 28.dp, bottom = 128.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Surface(
                        color = Color(0xFF3E4663),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "إضافة موقع",
                            color = TextPrimaryWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF3E4663),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "أضف هذا الموقع إلى قائمتك",
                            color = Color(0xFFE2E6F3),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Bottom-Left Floating "إضافة موقع" Button (00:27-00:46)
        Button(
            onClick = {
                showCoachmark = false
                onAddSiteFromBrowser(activeUrl)
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E2942),
                contentColor = PeriwinklePrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 18.dp)
                .height(50.dp)
                .testTag("browser_fab_add_site")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null,
                tint = PeriwinklePrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "إضافة موقع",
                color = PeriwinklePrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
