package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FavoriteCategoryEntity
import com.example.data.preferences.UserSettings
import com.example.ui.theme.AppCardSurface
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.AppDividerColor
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.PremiumGold
import com.example.ui.theme.PremiumGoldText
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.viewmodel.AppScreen

@Composable
fun SettingsMainScreen(
    onBackClick: () -> Unit,
    onNavigateSubScreen: (AppScreen) -> Unit,
    onShowToast: (String) -> Unit
) {
    var showPremiumDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("settings_main_screen")
    ) {
        // Top Back Arrow on the Right (Start in RTL) (01:08, 01:10)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = TextPrimaryWhite
                )
            }
        }

        Text(
            text = "الاعدادات",
            color = TextPrimaryWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main 8 Settings Items (01:08, 01:10)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsMenuRow(
                title = "عام",
                icon = Icons.Default.Home,
                testTag = "settings_item_general",
                onClick = { onNavigateSubScreen(AppScreen.SETTINGS_GENERAL) }
            )
            SettingsMenuRow(
                title = "المظهر",
                icon = Icons.Default.Palette,
                testTag = "settings_item_appearance",
                onClick = { onNavigateSubScreen(AppScreen.SETTINGS_APPEARANCE) }
            )
            SettingsMenuRow(
                title = "فئات المفضلة",
                icon = Icons.Default.Label,
                testTag = "settings_item_categories",
                onClick = { onNavigateSubScreen(AppScreen.SETTINGS_FAVORITE_CATEGORIES) }
            )
            SettingsMenuRow(
                title = "التخزين",
                icon = Icons.Default.Storage,
                testTag = "settings_item_storage",
                onClick = { onNavigateSubScreen(AppScreen.SETTINGS_STORAGE) }
            )
            SettingsMenuRow(
                title = "خيارات القراءة",
                icon = Icons.Default.MenuBook,
                testTag = "settings_item_reading",
                onClick = { onNavigateSubScreen(AppScreen.SETTINGS_READING_OPTIONS) }
            )
            SettingsMenuRow(
                title = "الفصول جديدة",
                icon = Icons.Default.Notifications,
                testTag = "settings_item_new_chapters",
                onClick = { onNavigateSubScreen(AppScreen.SETTINGS_NEW_CHAPTERS) }
            )
            SettingsMenuRow(
                title = "إحصائيات",
                icon = Icons.Default.Analytics,
                testTag = "settings_item_statistics",
                onClick = { onNavigateSubScreen(AppScreen.SETTINGS_STATISTICS) }
            )
            SettingsMenuRow(
                title = "المزيد",
                icon = Icons.Default.MoreHoriz,
                testTag = "settings_item_more",
                onClick = { onNavigateSubScreen(AppScreen.SETTINGS_MORE) }
            )
        }

        // Bottom Pager Dots + "العضوية المميزة" Card (01:10, 01:17, 01:35)
        var currentBannerSlide by remember { androidx.compose.runtime.mutableIntStateOf(2) }
        val bannerSlides = remember {
            listOf(
                "العضوية المميزة" to "تصفح سريع وبدون إعلانات مزعجة",
                "النسخ الاحتياطي" to "احفظ مواقعك ومفضلتك واستعدها بضغطة زر",
                "العضوية المميزة" to "بدون اعلانات + علامة التوثيق ومزايا إضافية"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0..2) {
                    val isSelectedDot = currentBannerSlide == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelectedDot) 8.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (isSelectedDot) Color(0xFF9EA4B8) else Color(0xFF555966))
                            .clickable { currentBannerSlide = index }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF171A24),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        currentBannerSlide = (currentBannerSlide + 1) % bannerSlides.size
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF8C9EFF),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = bannerSlides[currentBannerSlide].first,
                                color = TextPrimaryWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = bannerSlides[currentBannerSlide].second,
                                color = TextSecondaryGrey,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showPremiumDialog = true },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumGold,
                            contentColor = PremiumGoldText
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            text = "اشترك",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            containerColor = AppDialogSurface,
            title = { Text("العضوية المميزة", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "جميع الميزات الأساسية مثل إضافة المواقع، المتصفح الداخلي، التخزين المحلي، تخصيص السمات، والنسخ الاحتياطي مفعلة بالكامل ومجانية في هذه النسخة.",
                    color = TextSecondaryGrey
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumDialog = false
                        onShowToast("جميع الميزات مفعلة بالفعل!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PeriwinklePrimary, contentColor = PeriwinkleOnPrimary)
                ) {
                    Text("حسناً", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingsMenuRow(
    title: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 15.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PeriwinklePrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            color = TextPrimaryWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GeneralSettingsScreen(
    settings: UserSettings,
    onBackClick: () -> Unit,
    onUpdateStartScreen: (String) -> Unit,
    onUpdateRecNotifications: (Boolean) -> Unit
) {
    var showStartScreenDialog by remember { mutableStateOf(false) }
    val startScreenOptions = listOf("التخزين", "التوصيات", "التحديثات", "المفضلة", "السجل", "إدارة المواقع")

    // Exact screen from 01:09
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("settings_general_screen")
    ) {
        SettingsSubHeader(title = "عام", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showStartScreenDialog = true }
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "إفتح عند البدأ",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = settings.startScreen,
                color = TextSecondaryGrey,
                fontSize = 14.sp
            )
        }

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "خيارات التوصيات",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        SettingsSwitchItem(
            title = "إشعارات التوصيات",
            subtitle = null,
            checked = settings.recommendationNotifications,
            onCheckedChange = onUpdateRecNotifications
        )
    }

    if (showStartScreenDialog) {
        AlertDialog(
            onDismissRequest = { showStartScreenDialog = false },
            containerColor = AppDialogSurface,
            title = { Text("إفتح عند البدأ", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    startScreenOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateStartScreen(option)
                                    showStartScreenDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.startScreen == option,
                                onClick = {
                                    onUpdateStartScreen(option)
                                    showStartScreenDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PeriwinklePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option, color = TextPrimaryWhite, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStartScreenDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }
}

private data class ThemePreviewOption(
    val key: String,
    val label: String,
    val cardBg: Color,
    val topBarColor: Color,
    val accentBubble: Color,
    val bottomCircle: Color
)

@Composable
fun AppearanceSettingsScreen(
    settings: UserSettings,
    onBackClick: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onSelectNightMode: (String) -> Unit,
    onToggleBoolean: (String, Boolean) -> Unit
) {
    var showNightModeDialog by remember { mutableStateOf(false) }

    // Exact theme preview cards from 01:11-01:16
    val themeOptions = remember {
        listOf(
            ThemePreviewOption(
                key = "DEFAULT",
                label = "افتراضي",
                cardBg = Color(0xFF15161E),
                topBarColor = Color(0xFF393B47),
                accentBubble = Color(0xFF9C8CE8),
                bottomCircle = Color(0xFFA090F0)
            ),
            ThemePreviewOption(
                key = "DYNAMIC",
                label = "ديناميك",
                cardBg = Color(0xFF141720),
                topBarColor = Color(0xFF3A3F4E),
                accentBubble = Color(0xFF9BA8EE),
                bottomCircle = Color(0xFF81C784)
            ),
            ThemePreviewOption(
                key = "GREEN",
                label = "أخضر",
                cardBg = Color(0xFF131915),
                topBarColor = Color(0xFF344238),
                accentBubble = Color(0xFF7DC686),
                bottomCircle = Color(0xFFEC809E)
            ),
            ThemePreviewOption(
                key = "TIDAL",
                label = "موجة مد و جزر",
                cardBg = Color(0xFF0E1B28),
                topBarColor = Color(0xFF283D52),
                accentBubble = Color(0xFF5AB2E6),
                bottomCircle = Color(0xFF72C5F2)
            ),
            ThemePreviewOption(
                key = "YIN_YANG",
                label = "ين & يانغ",
                cardBg = Color(0xFF161616),
                topBarColor = Color(0xFF424242),
                accentBubble = Color(0xFFE0E0E0),
                bottomCircle = Color(0xFFFFFFFF)
            ),
            ThemePreviewOption(
                key = "YOTSUBA",
                label = "يوتسوبا",
                cardBg = Color(0xFF201512),
                topBarColor = Color(0xFF473530),
                accentBubble = Color(0xFFE89980),
                bottomCircle = Color(0xFFF2A48B)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .testTag("settings_appearance_screen")
    ) {
        SettingsSubHeader(title = "المظهر", onBackClick = onBackClick)

        Text(
            text = "السمة",
            color = TextPrimaryWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Text(
            text = "مظهر التطبيق",
            color = TextPrimaryWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        // Horizontal Scrollable Phone Mockup Theme Cards (01:11-01:16)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themeOptions, key = { it.key }) { theme ->
                val isSelected = settings.appTheme == theme.key
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(108.dp)
                        .clickable { onSelectTheme(theme.key) }
                        .testTag("theme_card_${theme.key}")
                ) {
                    Box(
                        modifier = Modifier
                            .width(104.dp)
                            .height(166.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(theme.cardBg)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) PeriwinklePrimary else Color(0xFF2E323E),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(10.dp)
                    ) {
                        // Top mockup bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(theme.topBarColor)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(PeriwinklePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "محدد",
                                        tint = PeriwinkleOnPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Middle accent pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(36.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.accentBubble)
                        )

                        // Bottom mockup bar with circle
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.topBarColor)
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(theme.bottomCircle)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = theme.label,
                        color = TextPrimaryWhite,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showNightModeDialog = true }
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text("الوضع الليلي", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(settings.nightMode, color = TextSecondaryGrey, fontSize = 14.sp)
        }

        SettingsSwitchItem(
            title = "وضع شاشة OLED باللون الأسود النقي (AMOLED #000000)",
            subtitle = "خلفية سوداء نقية 100% لتوفير البطارية وإبراز ألوان أغلفة المانهوا على شاشات سامسونج",
            checked = settings.pureDarkMode,
            onCheckedChange = { onToggleBoolean("pureDarkMode", it) }
        )

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "تخصيص القوائم",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        SettingsSwitchItem(
            title = "ضبط القوائم من اليمين لليسار",
            subtitle = null,
            checked = settings.rtlMenus,
            onCheckedChange = { onToggleBoolean("rtlMenus", it) }
        )
        SettingsSwitchItem(
            title = "إخفاء شريط الأدوات عند التمرير",
            subtitle = null,
            checked = settings.hideToolbarOnScroll,
            onCheckedChange = { onToggleBoolean("hideToolbarOnScroll", it) }
        )
        SettingsSwitchItem(
            title = "عرض زر متابعة القراءة",
            subtitle = null,
            checked = settings.showContinueReadingButton,
            onCheckedChange = { onToggleBoolean("showContinueReadingButton", it) }
        )
        SettingsSwitchItem(
            title = "عرض أيقونات المواقع",
            subtitle = null,
            checked = settings.showSiteIcons,
            onCheckedChange = { onToggleBoolean("showSiteIcons", it) }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showNightModeDialog) {
        val modes = listOf("اتبع مظهر النظام", "مفعل دائماً", "معطل")
        AlertDialog(
            onDismissRequest = { showNightModeDialog = false },
            containerColor = AppDialogSurface,
            title = { Text("الوضع الليلي", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    modes.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectNightMode(mode)
                                    showNightModeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.nightMode == mode,
                                onClick = {
                                    onSelectNightMode(mode)
                                    showNightModeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PeriwinklePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode, color = TextPrimaryWhite)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNightModeDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }
}

@Composable
fun FavoriteCategoriesSettingsScreen(
    categories: List<FavoriteCategoryEntity>,
    onBackClick: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (FavoriteCategoryEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("settings_categories_screen")
    ) {
        SettingsSubHeader(title = "فئات المفضلة", onBackClick = onBackClick)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "خصّص فئات لتنظيم أعمالك المفضلة",
                color = TextSecondaryGrey,
                fontSize = 14.sp
            )
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PeriwinklePrimary,
                    contentColor = PeriwinkleOnPrimary
                )
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة فئة", fontWeight = FontWeight.Bold)
            }
        }

        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لم تضف أي فئات مفضلة مخصصة بعد.", color = TextSecondaryGrey, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Label, null, tint = PeriwinklePrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(category.name, color = TextPrimaryWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { onDeleteCategory(category) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFFF6B6B))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = AppDialogSurface,
            title = { Text("إضافة فئة جديدة", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("اسم الفئة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName)
                            newCategoryName = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PeriwinklePrimary, contentColor = PeriwinkleOnPrimary)
                ) {
                    Text("إضافة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = PeriwinklePrimary)
                }
            }
        )
    }
}

@Composable
fun SettingsSubHeader(
    title: String,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("subsettings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = TextPrimaryWhite
                )
            }
        }
        Text(
            text = title,
            color = TextPrimaryWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // In the reference video (01:09, 01:11, 01:24, 01:29), the title is on the right (Start in RTL)
    // and the Switch is on the left (End in RTL)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = TextSecondaryGrey,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF1E253A),
                checkedTrackColor = PeriwinklePrimary,
                uncheckedThumbColor = Color(0xFF8E93A2),
                uncheckedTrackColor = Color(0xFF282C37)
            )
        )
    }
}
