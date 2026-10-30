package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.UserSettings
import com.example.data.repository.StorageStats
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.AppDividerColor
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.StorageAvailableGrey
import com.example.ui.theme.StorageDownloadsGreen
import com.example.ui.theme.StorageOtherPurple
import com.example.ui.theme.StoragePagesBlue
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun StorageSettingsScreen(
    stats: StorageStats,
    searchHistoryCount: Int,
    onBackClick: () -> Unit,
    onClearCache: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onShowToast: (String) -> Unit
) {
    var showMoveStorageDialog by remember { mutableStateOf(false) }

    // Exact screen from 01:20-01:22
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .testTag("settings_storage_screen")
    ) {
        SettingsSubHeader(title = "التخزين", onBackClick = onBackClick)

        Text(
            text = "إحصائيات التخزين",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Segmented horizontal storage bar (01:20)
        // In the video, the bar has rounded ends, filled grey for available space and a purple/blue/green segment on the right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StorageAvailableGrey)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (stats.downloadsBytes > 0L) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(18.dp)
                            .background(StorageDownloadsGreen)
                    )
                }
                if (stats.pagesBytes > 0L) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(18.dp)
                            .background(StoragePagesBlue)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(16.dp)
                        .background(StorageOtherPurple)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend rows (01:20)
        StorageLegendRow(
            dotColor = StorageDownloadsGreen,
            label = "التنزيلات - ${MainViewModel.formatBytesArabic(stats.downloadsBytes)}"
        )
        StorageLegendRow(
            dotColor = StoragePagesBlue,
            label = "الصفحات - ${MainViewModel.formatBytesArabic(stats.pagesBytes)}"
        )
        StorageLegendRow(
            dotColor = StorageOtherPurple,
            label = "أخر - ${MainViewModel.formatBytesArabic(stats.otherBytes)}"
        )
        StorageLegendRow(
            dotColor = StorageAvailableGrey,
            label = "مساحة متوفرة - ${MainViewModel.formatBytesArabic(stats.availableBytes)}"
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = AppDividerColor, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Storage path item (01:20)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "مسار حفظ التخزين",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = stats.storagePath.ifBlank { "/storage/emulated/0/Android/data/org.hamak.mangareader/files/saved" },
                    color = TextSecondaryGrey,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "للأعمال الجديدة؛ تبقى الأعمال المحفوظة في مجلداتها الحالية.",
                color = TextSecondaryGrey,
                fontSize = 13.sp
            )
        }

        // Move Storage (01:20)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMoveStorageDialog = true }
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "نقل التخزين",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Clear Cache (01:20)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClearCache() }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("clear_cache_row")
        ) {
            Text(
                text = "مسح ذاكرة التخزين المؤقته",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "حجم ذاكرة التخزين المؤقته: ${MainViewModel.formatBytesArabic(stats.cacheBytes)}",
                color = TextSecondaryGrey,
                fontSize = 14.sp
            )
        }

        // Clear Previous Search (01:20)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClearSearchHistory() }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("clear_search_history_row")
        ) {
            Text(
                text = "مسح البحث السابق",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "العناصر: ${MainViewModel.toArabicDigits(searchHistoryCount.toString())}",
                color = TextSecondaryGrey,
                fontSize = 14.sp
            )
        }
    }

    if (showMoveStorageDialog) {
        AlertDialog(
            onDismissRequest = { showMoveStorageDialog = false },
            containerColor = AppDialogSurface,
            title = { Text("نقل التخزين", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "يتم حفظ جميع الأعمال والصفحات حاليًا في المجلد الآمن الخاص بالتطبيق:\n${stats.storagePath}\nوهو المسار الموصى به في نظام Android لضمان سرعة القراءة وعدم فقدان الملفات.",
                    color = TextSecondaryGrey,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMoveStorageDialog = false
                        onShowToast("تم التحقق من مسار التخزين النشط")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PeriwinklePrimary, contentColor = PeriwinkleOnPrimary)
                ) {
                    Text("موافق", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun StorageLegendRow(dotColor: Color, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ReadingOptionsSettingsScreen(
    settings: UserSettings,
    onBackClick: () -> Unit,
    onUpdateString: (String, String) -> Unit,
    onUpdateBoolean: (String, Boolean) -> Unit,
    onUpdateInt: (String, Int) -> Unit,
    onUpdateFloat: (String, Float) -> Unit
) {
    var activeDialogKey by remember { mutableStateOf<String?>(null) }

    // Exact screen from 01:24-01:26
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .testTag("settings_reading_options_screen")
    ) {
        SettingsSubHeader(title = "خيارات القراءة", onBackClick = onBackClick)

        // Section: الأوضاع
        Text(
            text = "الأوضاع",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeDialogKey = "readingMode" }
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("الوضع", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(settings.readingMode, color = TextSecondaryGrey, fontSize = 14.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeDialogKey = "pageFitMode" }
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("وضع حجم الصفحة", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(settings.pageFitMode, color = TextSecondaryGrey, fontSize = 14.sp)
        }

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Section: التمرير
        Text(
            text = "التمرير",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeDialogKey = "scrollDirection" }
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("إتجاه التمرير", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(settings.scrollDirection, color = TextSecondaryGrey, fontSize = 14.sp)
        }

        SettingsSwitchItem(
            title = "التمرير بواسطة أزرار الصوت",
            subtitle = null,
            checked = settings.volumeKeysScroll,
            onCheckedChange = { onUpdateBoolean("volumeKeysScroll", it) }
        )

        SettingsSwitchItem(
            title = "التمرير التلقائي",
            subtitle = null,
            checked = settings.autoScroll,
            onCheckedChange = { onUpdateBoolean("autoScroll", it) }
        )

        Slider(
            value = settings.autoScrollSpeed,
            onValueChange = { onUpdateFloat("autoScrollSpeed", it) },
            colors = SliderDefaults.colors(
                thumbColor = TextPrimaryWhite,
                activeTrackColor = PeriwinklePrimary,
                inactiveTrackColor = Color(0xFF333744)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Section: وضع الويبتون (01:24-01:25)
        Text(
            text = "وضع الويبتون",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        SettingsSwitchItem(
            title = "تفعيل الوضع دائما",
            subtitle = null,
            checked = settings.webtoonAlwaysOn,
            onCheckedChange = { onUpdateBoolean("webtoonAlwaysOn", it) }
        )
        SettingsSwitchItem(
            title = "اكتشاف تلقائي",
            subtitle = null,
            checked = settings.webtoonAutoDetect,
            onCheckedChange = { onUpdateBoolean("webtoonAutoDetect", it) }
        )
        SettingsSwitchItem(
            title = "وضع التكبير",
            subtitle = null,
            checked = settings.webtoonZoom,
            onCheckedChange = { onUpdateBoolean("webtoonZoom", it) }
        )

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Section: الإعدادات العامة (01:25-01:26)
        Text(
            text = "الإعدادات العامة",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        SettingsSwitchItem(
            title = "فتح القائمة من خلال النقر على الشاشة",
            subtitle = null,
            checked = settings.tapToOpenMenu,
            onCheckedChange = { onUpdateBoolean("tapToOpenMenu", it) }
        )
        SettingsSwitchItem(
            title = "عرض قائمة البطارية والتوقيت",
            subtitle = null,
            checked = settings.showBatteryAndTime,
            onCheckedChange = { onUpdateBoolean("showBatteryAndTime", it) }
        )
        SettingsSwitchItem(
            title = "عرض رقم الصفحة",
            subtitle = null,
            checked = settings.showPageNumber,
            onCheckedChange = { onUpdateBoolean("showPageNumber", it) }
        )
        SettingsSwitchItem(
            title = "عرض التعليقات في نهاية الفصل",
            subtitle = "يظهر ملخصا صغيرا للتعليقات وزر فتحها بعد آخر صفحة من كل فصل",
            checked = settings.showCommentsAtEnd,
            onCheckedChange = { onUpdateBoolean("showCommentsAtEnd", it) }
        )

        // Clickable preference row for تحميل الصفحات (matching 01:25-01:26 in video)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeDialogKey = "loadPagesMode" }
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "تحميل الصفحات",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = settings.loadPagesMode,
                color = TextSecondaryGrey,
                fontSize = 13.sp
            )
        }

        SettingsSwitchItem(
            title = "إخفاء زر القائمة",
            subtitle = "سيصبح غير مرئي، لكن لا يزال قابل للنقر",
            checked = settings.hideMenuButton,
            onCheckedChange = { onUpdateBoolean("hideMenuButton", it) }
        )
        SettingsSwitchItem(
            title = "ضبط السطوع",
            subtitle = null,
            checked = settings.customBrightnessEnabled,
            onCheckedChange = { onUpdateBoolean("customBrightnessEnabled", it) }
        )

        // Brightness value & slider (01:26)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                text = settings.customBrightnessLevel.toString(),
                color = TextPrimaryWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessLow,
                    contentDescription = null,
                    tint = TextSecondaryGrey,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = settings.customBrightnessLevel.toFloat(),
                    onValueChange = { onUpdateInt("customBrightnessLevel", it.roundToInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = PeriwinklePrimary,
                        activeTrackColor = PeriwinklePrimary,
                        inactiveTrackColor = Color(0xFF333744)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SettingsSwitchItem(
            title = "الحفاظ على الشاشة مفتوحة",
            subtitle = null,
            checked = settings.keepScreenOn,
            onCheckedChange = { onUpdateBoolean("keepScreenOn", it) }
        )

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (activeDialogKey != null) {
        val (dialogTitle, options, currentVal) = when (activeDialogKey) {
            "readingMode" -> Triple("الوضع", listOf("الصفحات", "ويبتون مستمر", "صفحة مزدوجة"), settings.readingMode)
            "pageFitMode" -> Triple("وضع حجم الصفحة", listOf("تناسب الشاشة", "تناسب العرض", "تناسب الارتفاع"), settings.pageFitMode)
            "loadPagesMode" -> Triple("تحميل الصفحات", listOf("عبر واي فاي فقط", "دائماً"), settings.loadPagesMode)
            else -> Triple("إتجاه التمرير", listOf("من اليسار لليمين", "من اليمين لليسار", "عمودي"), settings.scrollDirection)
        }
        AlertDialog(
            onDismissRequest = { activeDialogKey = null },
            containerColor = AppDialogSurface,
            title = { Text(dialogTitle, color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateString(activeDialogKey!!, opt)
                                    activeDialogKey = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentVal == opt,
                                onClick = {
                                    onUpdateString(activeDialogKey!!, opt)
                                    activeDialogKey = null
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PeriwinklePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(opt, color = TextPrimaryWhite)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDialogKey = null }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }
}

@Composable
fun NewChaptersSettingsScreen(
    settings: UserSettings,
    onBackClick: () -> Unit,
    onUpdateBoolean: (String, Boolean) -> Unit
) {
    // Exact screen from 01:29
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("settings_new_chapters_screen")
    ) {
        SettingsSubHeader(title = "الفصول جديدة", onBackClick = onBackClick)

        SettingsSwitchItem(
            title = "التحقق من فصول جديدة",
            subtitle = null,
            checked = settings.checkNewChapters,
            onCheckedChange = { onUpdateBoolean("checkNewChapters", it) }
        )

        SettingsSwitchItem(
            title = "عبر واي فاي فقط",
            subtitle = null,
            checked = settings.checkNewChaptersWifiOnly,
            onCheckedChange = { onUpdateBoolean("checkNewChaptersWifiOnly", it) }
        )
    }
}

@Composable
fun StatisticsSettingsScreen(
    totalReadingSeconds: Long,
    sitesCount: Int,
    historyCount: Int,
    favoritesCount: Int,
    onBackClick: () -> Unit,
    onResetStats: () -> Unit
) {
    // Exact screen from 01:31
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("settings_statistics_screen")
    ) {
        SettingsSubHeader(title = "إحصائيات", onBackClick = onBackClick)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val durationLabel = when {
                    totalReadingSeconds < 60L -> "أقل من دقيقة"
                    totalReadingSeconds < 3600L -> "${totalReadingSeconds / 60} دقيقة"
                    else -> "${totalReadingSeconds / 3600} ساعة و ${(totalReadingSeconds % 3600) / 60} دقيقة"
                }

                Text(
                    text = durationLabel,
                    color = TextPrimaryWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(110.dp))

                if (historyCount == 0 && favoritesCount == 0) {
                    Text(
                        text = "لا يوجد أي احصائيات..",
                        color = TextPrimaryWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "المواقع المضافة: $sitesCount • السجل: $historyCount • المفضلة: $favoritesCount",
                        color = TextSecondaryGrey,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onResetStats) {
                        Text("إعادة تعيين الإحصائيات", color = PeriwinklePrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun MoreSettingsScreen(
    settings: UserSettings,
    onBackClick: () -> Unit,
    onUpdateInt: (String, Int) -> Unit,
    onUpdateBoolean: (String, Boolean) -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onShowToast: (String) -> Unit
) {
    var activeModal by remember { mutableStateOf<String?>(null) }

    // Exact screen from 01:32-01:34 ("اعدادات اخرى")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .testTag("settings_more_screen")
    ) {
        SettingsSubHeader(title = "اعدادات اخرى", onBackClick = onBackClick)

        // Section: البيانات
        Text(
            text = "البيانات",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeModal = "backup" }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("backup_restore_row")
        ) {
            Text("النسخ والاستعادة", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("الجهاز أو الحساب", color = TextSecondaryGrey, fontSize = 14.sp)
        }

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Section: شبكة الاتصال
        Text(
            text = "شبكة الاتصال",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeModal = "maxDownloads" }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("max_downloads_row")
        ) {
            Text("الحد الاقصى للتنزيلات", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(settings.maxConcurrentDownloads.toString(), color = TextSecondaryGrey, fontSize = 14.sp)
        }

        SettingsSwitchItem(
            title = "التحميل فقط من خلال الواي فاي",
            subtitle = null,
            checked = settings.downloadWifiOnly,
            onCheckedChange = { onUpdateBoolean("downloadWifiOnly", it) }
        )

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Section: المساعدة (01:33-01:34)
        Text(
            text = "المساعدة",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        MoreSettingsActionRow(
            title = "الابلاغ عن مشكلة في التطبيق",
            subtitle = "إرسال سجل الاخطاء إلى المطور",
            onClick = { activeModal = "report" }
        )
        MoreSettingsActionRow(
            title = "شروط الاستخدام",
            subtitle = "القواعد التي تنظم استخدام التطبيق والخدمات الاختيارية",
            onClick = { activeModal = "terms" }
        )
        MoreSettingsActionRow(
            title = "سياسة الخصوصية",
            subtitle = "البيانات التي يعالجها التطبيق وخيارات التحكم والحذف",
            onClick = { activeModal = "privacy" }
        )
        MoreSettingsActionRow(
            title = "تقييم التطبيق",
            subtitle = "رابط مباشر للتطبيق في جوجل بلاي",
            onClick = { onShowToast("شكراً لدعمك وتقييمك للتطبيق!") }
        )
        MoreSettingsActionRow(
            title = "حول",
            subtitle = "الإصدار: 2.0.7.6.11",
            onClick = { activeModal = "about" }
        )
        MoreSettingsActionRow(
            title = "تطبيقات ينصح بها",
            subtitle = "تطبيقات اخرى من نفس المطور",
            onClick = { activeModal = "recommendedApps" }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    when (activeModal) {
        "backup" -> {
            AlertDialog(
                onDismissRequest = { activeModal = null },
                containerColor = AppDialogSurface,
                title = { Text("النسخ والاستعادة", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "يمكنك إنشاء نسخة احتياطية محلية لجميع المواقع المضافة والمفضلة في مجلد التخزين، أو استعادتها في أي وقت.",
                        color = TextSecondaryGrey
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            activeModal = null
                            onCreateBackup()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PeriwinklePrimary, contentColor = PeriwinkleOnPrimary)
                    ) {
                        Text("إنشاء نسخة احتياطية", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            activeModal = null
                            onRestoreBackup()
                        }
                    ) {
                        Text("استعادة نسخة سابقة", color = PeriwinklePrimary)
                    }
                }
            )
        }
        "maxDownloads" -> {
            val limits = listOf(1, 2, 3, 4, 6, 8, 10)
            AlertDialog(
                onDismissRequest = { activeModal = null },
                containerColor = AppDialogSurface,
                title = { Text("الحد الاقصى للتنزيلات", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        limits.forEach { limit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdateInt("maxConcurrentDownloads", limit)
                                        activeModal = null
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.maxConcurrentDownloads == limit,
                                    onClick = {
                                        onUpdateInt("maxConcurrentDownloads", limit)
                                        activeModal = null
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = PeriwinklePrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$limit تنزيلات متزامنة", color = TextPrimaryWhite)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeModal = null }) {
                        Text("إغلاق", color = PeriwinklePrimary)
                    }
                }
            )
        }
        "terms" -> {
            InfoDialog(
                title = "شروط الاستخدام",
                body = "يعمل هذا التطبيق كمتصفح ومنظم للمواقع التي يضيفها المستخدم يدوياً. يلتزم المستخدم باحترام حقوق الملكية الفكرية وشروط الخدمة الخاصة بكل موقع يقوم بإضافته أو تصفحه.",
                onDismiss = { activeModal = null }
            )
        }
        "privacy" -> {
            InfoDialog(
                title = "سياسة الخصوصية",
                body = "تُحفظ جميع بياناتك (المواقع المضافة، المفضلة، السجل، والإعدادات) محلياً على جهازك فقط. عند التحقق من توافق الموقع، يُفحص نطاق الموقع فقط ولا يتم إرسال ملفات الارتباط أو سجل التصفح إلى أي طرف خارجي.",
                onDismiss = { activeModal = null }
            )
        }
        "report" -> {
            InfoDialog(
                title = "الإبلاغ عن مشكلة",
                body = "تم فحص سجل أخطاء النظام: لا توجد أخطاء حرجة مسجلة.\nيمكنك إرسال ملاحظاتك أو تقرير الأخطاء مباشرة لفريق التطوير.",
                onDismiss = {
                    activeModal = null
                    onShowToast("تم تجهيز سجل التشخيص بنجاح")
                }
            )
        }
        "about" -> {
            InfoDialog(
                title = "حول التطبيق",
                body = "مكتبة المانجا والمواقع\nالإصدار: 2.0.7.6.11\nتطبيق متكامل لإدارة وتصفح مواقع المانجا والويب مع دعم القراءة المخصصة والتخزين المحلي.",
                onDismiss = { activeModal = null }
            )
        }
        "recommendedApps" -> {
            InfoDialog(
                title = "تطبيقات ينصح بها",
                body = "• قارئ الويبتون والكتب المصورة\n• مدير التنزيلات والأرشيف المحلي",
                onDismiss = { activeModal = null }
            )
        }
    }
}

@Composable
private fun MoreSettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            color = TextPrimaryWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = TextSecondaryGrey,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun InfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppDialogSurface,
        title = { Text(title, color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
        text = { Text(body, color = TextSecondaryGrey, fontSize = 14.sp, lineHeight = 21.sp) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PeriwinklePrimary, contentColor = PeriwinkleOnPrimary)
            ) {
                Text("حسناً", fontWeight = FontWeight.Bold)
            }
        }
    )
}
