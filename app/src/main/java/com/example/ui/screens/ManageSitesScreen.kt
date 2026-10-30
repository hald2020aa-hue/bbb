package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SiteEntity
import com.example.data.source.MangaSourceAdapterRegistry
import com.example.ui.components.SiteIconBadge
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.AppDividerColor
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey

@Composable
fun ManageSitesScreen(
    sites: List<SiteEntity>,
    onBackClick: () -> Unit,
    onAddSiteClick: () -> Unit,
    onOpenBrowserClick: () -> Unit,
    onOpenSiteInBrowser: (SiteEntity) -> Unit,
    onToggleSiteEnabled: (SiteEntity, Boolean) -> Unit,
    onEditSite: (SiteEntity) -> Unit,
    onDeleteSite: (SiteEntity) -> Unit,
    onMoveSite: (SiteEntity, Boolean) -> Unit,
    onTestConnection: (SiteEntity) -> Unit = {},
    onOpenLastVisitedPage: (SiteEntity) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("manage_sites_screen")
    ) {
        // Top App Bar (00:01 & 00:50)
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
                    modifier = Modifier.testTag("manage_sites_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = TextPrimaryWhite
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إدارة المواقع",
                    color = TextPrimaryWhite,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (sites.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onAddSiteClick,
                        modifier = Modifier.testTag("topbar_add_site_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "إضافة موقع",
                            tint = TextPrimaryWhite
                        )
                    }
                    IconButton(
                        onClick = onOpenBrowserClick,
                        modifier = Modifier.testTag("topbar_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "المتصفح",
                            tint = TextPrimaryWhite
                        )
                    }
                }
            }
        }

        if (sites.isEmpty()) {
            // Exact Empty State (00:01, 00:47)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = Color(0xFF4C505E),
                        modifier = Modifier.size(104.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "لم تضف أي مواقع بعد",
                        color = TextPrimaryWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onAddSiteClick,
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PeriwinklePrimary,
                            contentColor = PeriwinkleOnPrimary
                        ),
                        modifier = Modifier
                            .width(205.dp)
                            .height(48.dp)
                            .testTag("empty_add_site_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إضافة موقع",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onOpenBrowserClick,
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, Color(0xFF6B7080)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PeriwinklePrimary
                        ),
                        modifier = Modifier
                            .width(205.dp)
                            .height(48.dp)
                            .testTag("empty_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = PeriwinklePrimary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "المتصفح",
                            color = PeriwinklePrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp)
            ) {
                items(sites, key = { it.id }) { site ->
                    SiteRowItem(
                        site = site,
                        onOpen = { onOpenSiteInBrowser(site) },
                        onToggleEnabled = { enabled -> onToggleSiteEnabled(site, enabled) },
                        onEdit = { onEditSite(site) },
                        onDelete = { onDeleteSite(site) },
                        onMoveUp = { onMoveSite(site, true) },
                        onMoveDown = { onMoveSite(site, false) },
                        onTestConnection = { onTestConnection(site) },
                        onOpenLastVisited = { onOpenLastVisitedPage(site) }
                    )
                    HorizontalDivider(color = AppDividerColor, thickness = 0.8.dp)
                }
            }
        }
    }
}

@Composable
private fun SiteRowItem(
    site: SiteEntity,
    onOpen: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTestConnection: () -> Unit,
    onOpenLastVisited: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val capabilities = remember(site.id, site.url, site.adapterType) {
        MangaSourceAdapterRegistry.getCapabilitiesForSite(site)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("site_row_${site.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMoveUp,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "ترتيب الموقع",
                tint = Color(0xFFC5CAD6),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Switch(
            checked = site.isEnabled,
            onCheckedChange = onToggleEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF1E253A),
                checkedTrackColor = PeriwinklePrimary,
                uncheckedThumbColor = Color(0xFF8E93A2),
                uncheckedTrackColor = Color(0xFF282C37)
            ),
            modifier = Modifier.testTag("site_switch_${site.id}")
        )

        Spacer(modifier = Modifier.width(12.dp))

        SiteIconBadge(
            iconKey = site.iconKey,
            siteName = site.name,
            size = 44.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = site.name,
                color = TextPrimaryWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = site.url,
                    color = TextSecondaryGrey,
                    fontSize = 13.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (capabilities.supportsChapterDownload) {
                    "يدعم القارئ المدمج وتنزيل الفصول"
                } else {
                    "وضع التصفح المباشر (WebView)"
                },
                color = if (capabilities.supportsChapterDownload) Color(0xFF81C784) else TextSecondaryGrey,
                fontSize = 11.sp
            )
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.testTag("site_more_menu_${site.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "خيارات الموقع",
                    tint = TextPrimaryWhite
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = AppDialogSurface
            ) {
                DropdownMenuItem(
                    text = { Text("فتح الموقع", color = TextPrimaryWhite) },
                    leadingIcon = { Icon(Icons.Default.OpenInBrowser, null, tint = PeriwinklePrimary) },
                    onClick = {
                        menuExpanded = false
                        onOpen()
                    }
                )
                DropdownMenuItem(
                    text = { Text("آخر صفحة تمت زيارتها", color = TextPrimaryWhite) },
                    leadingIcon = { Icon(Icons.Default.History, null, tint = PeriwinklePrimary) },
                    onClick = {
                        menuExpanded = false
                        onOpenLastVisited()
                    }
                )
                DropdownMenuItem(
                    text = { Text("فحص حالة الاتصال", color = TextPrimaryWhite) },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF81C784)) },
                    onClick = {
                        menuExpanded = false
                        onTestConnection()
                    }
                )
                DropdownMenuItem(
                    text = { Text("تعديل الموقع", color = TextPrimaryWhite) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = PeriwinklePrimary) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("تحريك لأعلى", color = TextPrimaryWhite) },
                    leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null, tint = TextSecondaryGrey) },
                    onClick = {
                        menuExpanded = false
                        onMoveUp()
                    }
                )
                DropdownMenuItem(
                    text = { Text("تحريك لأسفل", color = TextPrimaryWhite) },
                    leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextSecondaryGrey) },
                    onClick = {
                        menuExpanded = false
                        onMoveDown()
                    }
                )
                DropdownMenuItem(
                    text = { Text("حذف الموقع", color = Color(0xFFFF6B6B)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
