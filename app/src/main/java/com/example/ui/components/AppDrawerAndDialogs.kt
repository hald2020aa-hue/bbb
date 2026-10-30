package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.data.LiveMangaSiteScraper
import com.example.data.local.SiteEntity
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.AppDividerColor
import com.example.ui.theme.AppDrawerSelectedPill
import com.example.ui.theme.AppDrawerSurface
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.viewmodel.AddSiteDialogState
import com.example.ui.viewmodel.AppScreen

@Composable
fun SiteIconBadge(
    iconKey: String,
    siteName: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    when {
        iconKey == "mangalek" || siteName.contains("مانجا ليك") || siteName.contains("Mangalek", ignoreCase = true) -> {
            // Exact white rounded square with stylized red & blue M from 00:48 & 00:50
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F6FA)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize(0.75f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val redPath = Path().apply {
                        moveTo(w * 0.1f, h * 0.85f)
                        lineTo(w * 0.1f, h * 0.15f)
                        lineTo(w * 0.5f, h * 0.65f)
                        lineTo(w * 0.9f, h * 0.15f)
                        lineTo(w * 0.9f, h * 0.85f)
                    }
                    drawPath(
                        path = redPath,
                        color = Color(0xFFD93829),
                        style = Stroke(width = w * 0.18f, cap = StrokeCap.Round)
                    )
                    drawLine(
                        color = Color(0xFF2874A6),
                        start = Offset(w * 0.72f, h * 0.55f),
                        end = Offset(w * 0.92f, h * 0.85f),
                        strokeWidth = w * 0.16f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        iconKey == "teamx" || siteName.contains("Team-X", ignoreCase = true) || siteName.contains("olympu", ignoreCase = true) -> {
            // Exact dark circle/rounded badge with red & silver X from 00:06 & 00:25
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2028))
                    .border(1.dp, Color(0xFF343846), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize(0.72f)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawArc(
                        color = Color(0xFF8A8F9E),
                        startAngle = 120f,
                        sweepAngle = 220f,
                        useCenter = false,
                        style = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
                    )
                    drawLine(
                        color = Color(0xFFD62828),
                        start = Offset(w * 0.2f, h * 0.2f),
                        end = Offset(w * 0.85f, h * 0.85f),
                        strokeWidth = w * 0.18f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF9E2A2B),
                        start = Offset(w * 0.85f, h * 0.2f),
                        end = Offset(w * 0.2f, h * 0.85f),
                        strokeWidth = w * 0.16f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        iconKey == "mangadar" || siteName.contains("MangaDar", ignoreCase = true) -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF5E35B1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = siteName,
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize(0.65f)
                )
            }
        }
        iconKey == "azora" || siteName.contains("ازورا") || siteName.contains("Azora", ignoreCase = true) -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF211A16))
                    .border(1.dp, Color(0xFFFF8F00), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔥",
                    fontSize = (size.value * 0.52f).sp
                )
            }
        }
        iconKey == "mangatek" || siteName.contains("مانجاتك") || siteName.contains("Mangatek", ignoreCase = true) -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1A1D))
                    .border(1.dp, Color(0xFFFF5722), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    color = Color(0xFFFF7043),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.55f).sp
                )
            }
        }
        iconKey == "procomic" || siteName.contains("ProComic", ignoreCase = true) -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1565C0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.55f).sp
                )
            }
        }
        iconKey == "swat" || siteName.contains("Swat", ignoreCase = true) -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF263238))
                    .border(1.dp, Color(0xFF90A4AE), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = Color(0xFFECEFF1),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.55f).sp
                )
            }
        }
        else -> {
            // Stylized dark globe badge matching 00:42 & 01:02
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF23262F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = siteName,
                    tint = Color(0xFF7B8191),
                    modifier = Modifier.fillMaxSize(0.72f)
                )
            }
        }
    }
}

@Composable
fun AppNavigationDrawerContent(
    currentScreen: AppScreen,
    enabledSites: List<SiteEntity>,
    userProfileName: String,
    userProfileEmail: String = "",
    selectedSiteId: Long? = null,
    showSiteIcons: Boolean,
    onSelectScreen: (AppScreen) -> Unit,
    onOpenSiteInBrowser: (SiteEntity) -> Unit,
    onLoginClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(305.dp)
            .testTag("navigation_drawer"),
        color = AppDrawerSurface,
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            // Top Profile Header (00:02 - 00:05 of reference video)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLoginClick() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .testTag("drawer_login_header"),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF282D3C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "الحساب الشخصي",
                        tint = Color(0xFFB5C0DB),
                        modifier = Modifier.size(60.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (userProfileName.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = userProfileName,
                            color = TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (userProfileEmail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userProfileEmail,
                            color = TextSecondaryGrey,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        text = "انقر هنا لتسجيل الدخول",
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = AppDividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Primary Navigation Items
            DrawerMenuItem(
                title = "التخزين",
                icon = if (currentScreen == AppScreen.STORAGE) Icons.Filled.Book else Icons.Outlined.Book,
                selected = currentScreen == AppScreen.STORAGE,
                testTag = "drawer_item_storage",
                onClick = { onSelectScreen(AppScreen.STORAGE) }
            )
            DrawerMenuItem(
                title = "التوصيات",
                icon = if (currentScreen == AppScreen.RECOMMENDATIONS) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                selected = currentScreen == AppScreen.RECOMMENDATIONS,
                testTag = "drawer_item_recommendations",
                onClick = { onSelectScreen(AppScreen.RECOMMENDATIONS) }
            )
            DrawerMenuItem(
                title = "التحديثات",
                icon = Icons.Default.Sync,
                selected = currentScreen == AppScreen.UPDATES,
                testTag = "drawer_item_updates",
                onClick = { onSelectScreen(AppScreen.UPDATES) }
            )
            DrawerMenuItem(
                title = "المفضلة",
                icon = Icons.Default.StarBorder,
                selected = currentScreen == AppScreen.FAVORITES,
                testTag = "drawer_item_favorites",
                onClick = { onSelectScreen(AppScreen.FAVORITES) }
            )
            DrawerMenuItem(
                title = "السجل",
                icon = Icons.Default.History,
                selected = currentScreen == AppScreen.HISTORY,
                testTag = "drawer_item_history",
                onClick = { onSelectScreen(AppScreen.HISTORY) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = AppDividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Manage Sites
            DrawerMenuItem(
                title = "إدارة المواقع",
                icon = if (currentScreen == AppScreen.MANAGE_SITES) Icons.Filled.Extension else Icons.Outlined.Extension,
                selected = currentScreen == AppScreen.MANAGE_SITES,
                testTag = "drawer_item_manage_sites",
                onClick = { onSelectScreen(AppScreen.MANAGE_SITES) }
            )

            // Added Sites Section (only user-added sites)
            val drawerSites = enabledSites

            if (drawerSites.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = AppDividerColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "المواقع المضافة",
                    color = TextSecondaryGrey,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )

                drawerSites.forEach { site ->
                    val drawerSiteLabel = if (site.domain.contains("mangalik") && !site.name.contains("Mangalek", ignoreCase = true)) {
                        "مانجا ليك Mangalek"
                    } else {
                        site.name
                    }
                    val isSiteActive = currentScreen == AppScreen.SITE_CATALOG && selectedSiteId == site.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (isSiteActive) AppDrawerSelectedPill else Color.Transparent)
                            .clickable { onOpenSiteInBrowser(site) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("drawer_added_site_${site.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showSiteIcons) {
                            SiteIconBadge(
                                iconKey = site.iconKey,
                                siteName = site.name,
                                size = 26.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(
                            text = drawerSiteLabel,
                            color = if (isSiteActive) PeriwinklePrimary else TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = if (isSiteActive) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = AppDividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Settings
            DrawerMenuItem(
                title = "الاعدادات",
                icon = if (currentScreen.name.startsWith("SETTINGS")) Icons.Filled.Settings else Icons.Outlined.Settings,
                selected = currentScreen.name.startsWith("SETTINGS"),
                testTag = "drawer_item_settings",
                onClick = { onSelectScreen(AppScreen.SETTINGS_MAIN) }
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor = if (selected) AppDrawerSelectedPill else Color.Transparent
    val contentColor = if (selected) Color(0xFFD6E0FF) else TextPrimaryWhite

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AddSiteFlowDialog(
    state: AddSiteDialogState,
    onUrlChange: (String) -> Unit,
    onStep1Submit: () -> Unit,
    onStep2NameChange: (String) -> Unit,
    onStep2Confirm: () -> Unit,
    onDismiss: () -> Unit,
    onVerificationCompleted: (String?) -> Unit = {}
) {
    if (state is AddSiteDialogState.Hidden) return

    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_site_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = AppDialogSurface,
            tonalElevation = 8.dp
        ) {
            when (state) {
                is AddSiteDialogState.Step1InputUrl -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "إضافة موقع",
                            color = TextPrimaryWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Row with Paste icon on the right (Start in RTL) and OutlinedTextField next to it (matching 00:33, 00:54)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        onUrlChange(clipText.trim())
                                    }
                                },
                                enabled = !state.isChecking,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("paste_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "لصق الرابط",
                                    tint = Color(0xFFC5CAD6)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                OutlinedTextField(
                                    value = state.urlInput,
                                    onValueChange = onUrlChange,
                                    enabled = !state.isChecking,
                                    singleLine = true,
                                    label = {
                                        Text(
                                            text = "أدخل عنوان موقع",
                                            fontSize = 12.sp
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            text = "example.com",
                                            color = TextMutedGrey
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PeriwinklePrimary,
                                        unfocusedBorderColor = Color(0xFF6B7080),
                                        focusedLabelColor = PeriwinklePrimary,
                                        unfocusedLabelColor = TextSecondaryGrey,
                                        focusedTextColor = TextPrimaryWhite,
                                        unfocusedTextColor = TextPrimaryWhite,
                                        disabledTextColor = TextSecondaryGrey,
                                        disabledBorderColor = Color(0xFF454954)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("site_url_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "للتحقق من التوافق، يُرسل اسم نطاق الموقع فقط.\nلا يُرسل مسار الرابط أو محتوى الصفحة أو ملفات الارتباط.",
                            color = TextSecondaryGrey,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (state.errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = state.errorMessage,
                                color = Color(0xFFFF6B6B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (state.isChecking && state.statusMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = state.statusMessage,
                                color = TextPrimaryWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = PeriwinklePrimary,
                                trackColor = Color(0xFF2D3242)
                            )
                        }

                        // Initial Site Verification WebView (00:08 - 00:22 of reference video: runs once when adding the site)
                        if (state.verificationUrl != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF141720))
                                    .border(1.dp, Color(0xFF2E3446), RoundedCornerShape(12.dp))
                            ) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            val cm = CookieManager.getInstance()
                                            cm.setAcceptCookie(true)
                                            cm.setAcceptThirdPartyCookies(this, true)
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.databaseEnabled = true
                                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            val ua = settings.userAgentString
                                            if (!ua.isNullOrBlank()) {
                                                LiveMangaSiteScraper.activeUserAgent = ua.replace("; wv", "")
                                                settings.userAgentString = LiveMangaSiteScraper.activeUserAgent
                                            }
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    try {
                                                        CookieManager.getInstance().flush()
                                                    } catch (_: Throwable) {}
                                                    val pageTitle = view?.title
                                                    if (!pageTitle.isNullOrBlank() &&
                                                        !pageTitle.contains("Just a moment", ignoreCase = true) &&
                                                        !pageTitle.contains("Cloudflare", ignoreCase = true)
                                                    ) {
                                                        view.postDelayed({
                                                            onVerificationCompleted(pageTitle)
                                                        }, 400L)
                                                    }
                                                }
                                            }
                                            loadUrl(state.verificationUrl)
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Buttons aligned to the left (End in RTL) matching 00:33 & 00:54
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("cancel_add_site_button")
                            ) {
                                Text(
                                    text = "إلغاء",
                                    color = PeriwinklePrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = onStep1Submit,
                                enabled = !state.isChecking,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PeriwinklePrimary,
                                    contentColor = PeriwinkleOnPrimary,
                                    disabledContainerColor = Color(0xFF2A2E39),
                                    disabledContentColor = Color(0xFF6E7382)
                                ),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("submit_step1_add_site_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إضافة",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                is AddSiteDialogState.Step2ConfirmManual -> {
                    // Exact Step 2 Dialog: "إضافة موقع يدويًا" (00:42-00:43, 01:02-01:03)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إضافة موقع يدويًا",
                            color = TextPrimaryWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // Large rounded globe icon preview in center (00:42, 01:02)
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF252830)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color(0xFF8C919E),
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = state.siteName,
                            onValueChange = onStep2NameChange,
                            singleLine = true,
                            label = {
                                Text("اسم الموقع")
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PeriwinklePrimary,
                                unfocusedBorderColor = Color(0xFF6B7080),
                                focusedLabelColor = PeriwinklePrimary,
                                unfocusedLabelColor = TextSecondaryGrey,
                                focusedTextColor = TextPrimaryWhite,
                                unfocusedTextColor = TextPrimaryWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("site_name_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("cancel_step2_button")
                            ) {
                                Text(
                                    text = "إلغاء",
                                    color = PeriwinklePrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = onStep2Confirm,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PeriwinklePrimary,
                                    contentColor = PeriwinkleOnPrimary
                                ),
                                modifier = Modifier
                                    .height(44.dp)
                                    .padding(horizontal = 4.dp)
                                    .testTag("confirm_step2_add_site_button")
                            ) {
                                Text(
                                    text = "تأكيد",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                AddSiteDialogState.Hidden -> {}
            }
        }
    }
}

@Composable
fun EditSiteDialog(
    site: SiteEntity?,
    onSave: (SiteEntity, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (site == null) return
    var name by remember(site.id) { mutableStateOf(site.name) }
    var url by remember(site.id) { mutableStateOf(site.url) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppDialogSurface,
        title = {
            Text("تعديل الموقع", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الموقع") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("رابط الموقع") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(site, name, url) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PeriwinklePrimary,
                    contentColor = PeriwinkleOnPrimary
                )
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = PeriwinklePrimary)
            }
        }
    )
}

@Composable
fun DeleteSiteConfirmDialog(
    site: SiteEntity?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (site == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppDialogSurface,
        title = {
            Text("حذف الموقع", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = "هل أنت متأكد من حذف موقع «${site.name}» من قائمة المواقع المضافة؟",
                color = TextSecondaryGrey
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE05252),
                    contentColor = Color.White
                )
            ) {
                Text("حذف", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = PeriwinklePrimary)
            }
        }
    )
}

@Composable
fun LoginProfileDialog(
    visible: Boolean,
    currentName: String,
    currentEmail: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    var name by remember(currentName) { mutableStateOf(currentName) }
    var email by remember(currentEmail) { mutableStateOf(currentEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppDialogSurface,
        title = {
            Text("الملف الشخصي وتسجيل الدخول", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "سجّل بيانات ملفك الشخصي لحفظ تفضيلاتك ومزامنة الإعدادات محليًا.",
                    color = TextSecondaryGrey,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المستخدم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, email) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PeriwinklePrimary,
                    contentColor = PeriwinkleOnPrimary
                )
            ) {
                Text("حفظ الدخول", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (currentName.isNotBlank()) {
                    TextButton(onClick = { onSave("", "") }) {
                        Text("تسجيل الخروج", color = Color(0xFFFF6B6B))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("إلغاء", color = PeriwinklePrimary)
                }
            }
        }
    )
}

@Composable
fun AppBadgeIcon(modifier: Modifier = Modifier) {
    // Blue/yellow circular app badge shown inside the toast/snackbar in 00:44, 00:47, 01:04, 01:06
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0xFF1565C0))
            .border(1.5.dp, Color(0xFFFFCA28), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            tint = Color(0xFFFFEB3B),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun BottomToastAndNoticeOverlay(
    toastMessage: String?,
    showRestartBanner: Boolean,
    onDismissRestartBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF282B34),
                tonalElevation = 8.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = toastMessage ?: "",
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AppBadgeIcon()
                }
            }
        }

        AnimatedVisibility(
            visible = showRestartBanner && toastMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // Exact banner from 00:47, 00:48, 01:06, 01:08
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF262932),
                tonalElevation = 8.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable { onDismissRestartBanner() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "عليك إغلاق التطبيق واعادة تشغيله\nمن اجل تطبيق التغييرات",
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AppBadgeIcon()
                }
            }
        }
    }
}
