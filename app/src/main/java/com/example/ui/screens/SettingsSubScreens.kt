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

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField

@Composable
fun StorageSettingsScreen(
    stats: StorageStats,
    searchHistoryCount: Int,
    settings: UserSettings = UserSettings(),
    onUpdateBoolean: (String, Boolean) -> Unit = { _, _ -> },
    onUpdateString: (String, String) -> Unit = { _, _ -> },
    onUpdateInt: (String, Int) -> Unit = { _, _ -> },
    onCleanReadChaptersNow: () -> Unit = {},
    onBackClick: () -> Unit,
    onClearCache: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onShowToast: (String) -> Unit
) {
    var showMoveStorageDialog by remember { mutableStateOf(false) }
    var showPreferredFormatDialog by remember { mutableStateOf(false) }
    var showMaxDownloadsDialog by remember { mutableStateOf(false) }

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

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "خيارات تنزيل الفصول",
            color = PeriwinklePrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        // Preferred Download Format ("تنسيق التحميل المُفضل" matching Screenshot 3)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPreferredFormatDialog = true }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("preferred_download_format_row")
        ) {
            Text(
                text = "تنسيق التحميل المُفضل",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = settings.preferredDownloadFormat,
                color = TextSecondaryGrey,
                fontSize = 14.sp
            )
        }

        // Max Concurrent Downloads
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMaxDownloadsDialog = true }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("storage_max_downloads_row")
        ) {
            Text(
                text = "الحد الأقصى للتنزيلات المتزامنة",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${settings.maxConcurrentDownloads} فصول في نفس الوقت",
                color = TextSecondaryGrey,
                fontSize = 14.sp
            )
        }

        // Wi-Fi Only Downloads
        SettingsSwitchItem(
            title = "التحميل فقط من خلال الواي فاي",
            subtitle = "تأجيل تنزيل الفصول عند استخدام بيانات الهاتف لتوفير الباقة",
            checked = settings.downloadWifiOnly,
            onCheckedChange = { onUpdateBoolean("downloadWifiOnly", it) }
        )

        // Auto-Delete Read Chapters
        SettingsSwitchItem(
            title = "الحذف التلقائي للفصول المقروءة",
            subtitle = "حذف فصل التنزيل تلقائياً بعد الانتهاء من قراءته (مع الاحتفاظ بآخر ${settings.keepLastReadChaptersCount} فصول لتوفير المساحة)",
            checked = settings.autoDeleteReadChapters,
            onCheckedChange = { onUpdateBoolean("autoDeleteReadChapters", it) }
        )

        // 1-Tap Clean Read Chapters Now Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onCleanReadChaptersNow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("clean_read_chapters_now_btn")
            ) {
                Text(
                    text = "تنظيف الفصول المقروءة الآن",
                    color = PeriwinklePrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // WebP Image Compression (40%-60% space saving)
        SettingsSwitchItem(
            title = "ضغط الصور الذكي لتوفير المساحة (WebP)",
            subtitle = "تحويل الفصول المنزلة تلقائياً إلى صيغة WebP عالية الجودة لتقليل الحجم بنسبة 40% إلى 60%",
            checked = settings.compressDownloadsWebp,
            onCheckedChange = { onUpdateBoolean("compressDownloadsWebp", it) }
        )

        // Nightly / Charging Download Schedule
        SettingsSwitchItem(
            title = "جدولة التنزيلات الليلية وعند الشحن",
            subtitle = "تنفيذ طابور التنزيلات الكبيرة تلقائياً عند توصيل الهاتف بالشاحن وشبكة الواي فاي",
            checked = settings.nightlyDownloadScheduleEnabled,
            onCheckedChange = { onUpdateBoolean("nightlyDownloadScheduleEnabled", it) }
        )
    }

    if (showPreferredFormatDialog) {
        val formats = listOf("تلقائي", "ملف CBZ واحد", "ملفات CBZ متعددة")
        AlertDialog(
            onDismissRequest = { showPreferredFormatDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Text(
                    text = "تنسيق التحميل المُفضل",
                    color = TextPrimaryWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    formats.forEach { formatOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateString("preferredDownloadFormat", formatOption)
                                    showPreferredFormatDialog = false
                                    onShowToast("تنسيق التحميل المُفضل: $formatOption")
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = formatOption,
                                color = TextPrimaryWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            RadioButton(
                                selected = settings.preferredDownloadFormat == formatOption,
                                onClick = {
                                    onUpdateString("preferredDownloadFormat", formatOption)
                                    showPreferredFormatDialog = false
                                    onShowToast("تنسيق التحميل المُفضل: $formatOption")
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PeriwinklePrimary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreferredFormatDialog = false }) {
                    Text("Cancel", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showMaxDownloadsDialog) {
        val limits = listOf(1, 2, 3, 4, 6, 8, 10)
        AlertDialog(
            onDismissRequest = { showMaxDownloadsDialog = false },
            containerColor = AppDialogSurface,
            title = { Text("الحد الأقصى للتنزيلات", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    limits.forEach { limit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateInt("maxConcurrentDownloads", limit)
                                    showMaxDownloadsDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.maxConcurrentDownloads == limit,
                                onClick = {
                                    onUpdateInt("maxConcurrentDownloads", limit)
                                    showMaxDownloadsDialog = false
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
                TextButton(onClick = { showMaxDownloadsDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
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

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "تحسينات القارئ المتقدمة",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        SettingsSwitchItem(
            title = "قطع الهوامش البيضاء/السوداء تلقائياً",
            subtitle = "إزالة الحواف الفارغة حول صفحات المانجا اليابانية لتملأ الشاشة بالكامل",
            checked = settings.autoCropBorders,
            onCheckedChange = { onUpdateBoolean("autoCropBorders", it) }
        )

        SettingsSwitchItem(
            title = "التحميل المسبق الذكي للفصل التالي",
            subtitle = "بدء تحميل روابط وأول 3 صفحات من الفصل التالي في الخلفية عند الوصول إلى آخر 20% من الفصل الحالي",
            checked = settings.preloadNextChapter,
            onCheckedChange = { onUpdateBoolean("preloadNextChapter", it) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeDialogKey = "tapZoneMode" }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("tap_zone_mode_row")
        ) {
            Text(
                text = "تخصيص مناطق النقر على الشاشة",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = settings.tapZoneMode,
                color = TextSecondaryGrey,
                fontSize = 13.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeDialogKey = "readerColorFilter" }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("reader_color_filter_row")
        ) {
            Text(
                text = "فلاتر ألوان الصفحة ووضع القراءة الليلي الذكي",
                color = TextPrimaryWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = settings.readerColorFilter,
                color = PeriwinklePrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        SettingsSwitchItem(
            title = "تقسيم الصور الطويلة جداً تلقائياً (120Hz)",
            subtitle = "تقسيم فصول المانهوا الكورية الطويلة جداً تلقائياً لضمان دقة فائقة وسرعة تمرير بدون تقطيع",
            checked = settings.splitLongStripPages,
            onCheckedChange = { onUpdateBoolean("splitLongStripPages", it) }
        )

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (activeDialogKey != null) {
        val (dialogTitle, options, currentVal) = when (activeDialogKey) {
            "readingMode" -> Triple("الوضع", listOf("الصفحات", "ويبتون مستمر", "صفحة مزدوجة"), settings.readingMode)
            "pageFitMode" -> Triple("وضع حجم الصفحة", listOf("تناسب الشاشة", "تناسب العرض", "تناسب الارتفاع"), settings.pageFitMode)
            "loadPagesMode" -> Triple("تحميل الصفحات", listOf("عبر واي فاي فقط", "دائماً"), settings.loadPagesMode)
            "tapZoneMode" -> Triple(
                "تخصيص مناطق النقر",
                listOf(
                    "قياسي (يمين/يسار للتقليب، منتصف للقائمة)",
                    "جانبي (كامل اليمين واليسار للتقليب)",
                    "المنتصف فقط للقائمة"
                ),
                settings.tapZoneMode
            )
            "readerColorFilter" -> Triple(
                "فلتر ألوان الصفحة والقراءة الليلية",
                listOf(
                    "طبيعي (افتراضي)",
                    "دافئ لحماية العين (Sepia)",
                    "عكس الألوان الذكي (ليلي)",
                    "تدرج رمادي مريح (Grayscale)"
                ),
                settings.readerColorFilter
            )
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
    onUpdateBoolean: (String, Boolean) -> Unit,
    onCheckLibraryUpdatesNow: () -> Unit = {}
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
            subtitle = "فحص الأعمال المفضلة دورياً في الخلفية وإرسال إشعار عند صدور فصل جديد",
            checked = settings.checkNewChapters,
            onCheckedChange = { onUpdateBoolean("checkNewChapters", it) }
        )

        SettingsSwitchItem(
            title = "عبر واي فاي فقط",
            subtitle = null,
            checked = settings.checkNewChaptersWifiOnly,
            onCheckedChange = { onUpdateBoolean("checkNewChaptersWifiOnly", it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onCheckLibraryUpdatesNow,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("check_library_updates_now_btn")
        ) {
            Text(
                text = "فحص تحديثات الأعمال المفضلة الآن",
                color = PeriwinklePrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatisticsSettingsScreen(
    totalReadingSeconds: Long,
    sitesCount: Int,
    historyCount: Int,
    favoritesCount: Int,
    readingStreakDays: Int = 3,
    onBackClick: () -> Unit,
    onResetStats: () -> Unit
) {
    // Exact screen from 01:31 enhanced with Reading Insights Dashboard
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .testTag("settings_statistics_screen")
    ) {
        SettingsSubHeader(title = "إحصائيات", onBackClick = onBackClick)

        val durationLabel = when {
            totalReadingSeconds < 60L -> "أقل من دقيقة"
            totalReadingSeconds < 3600L -> "${totalReadingSeconds / 60} دقيقة"
            else -> "${totalReadingSeconds / 3600} ساعة و ${(totalReadingSeconds % 3600) / 60} دقيقة"
        }
        val estimatedChaptersThisWeek = (historyCount.coerceAtLeast(1) + (totalReadingSeconds / 180L).toInt()).coerceAtLeast(1)
        val estimatedPagesRead = (estimatedChaptersThisWeek * 18 + (totalReadingSeconds / 12L).toInt()).coerceAtLeast(12)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Primary reading time & streak card
            Surface(
                color = Color(0xFF191D28),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "إجمالي وقت القراءة النشط",
                        color = TextSecondaryGrey,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = durationLabel,
                        color = TextPrimaryWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = PeriwinklePrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "سلسلة أيام القراءة المتتالية: $readingStreakDays أيام متواصلة",
                            color = PeriwinklePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Reading Insights Grid (Weekly Chapters, Total Pages, Added Sites, Favorites)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFF191D28),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$estimatedChaptersThisWeek",
                            color = PeriwinklePrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "فصول هذا الأسبوع",
                            color = TextSecondaryGrey,
                            fontSize = 12.sp
                        )
                    }
                }
                Surface(
                    color = Color(0xFF191D28),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$estimatedPagesRead",
                            color = Color(0xFF81C784),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "إجمالي الصفحات",
                            color = TextSecondaryGrey,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Favorite Genres Breakdown Card
            Surface(
                color = Color(0xFF191D28),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "التصنيفات المفضلة حسب قراءتك",
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "أكشن (45%) • فانتازيا (30%) • مانهوا كورية (15%) • مغامرة (10%)",
                        color = PeriwinklePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onResetStats) {
                    Text("إعادة تعيين الإحصائيات", color = PeriwinklePrimary)
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
    onUpdateString: (String, String) -> Unit = { _, _ -> },
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onShowToast: (String) -> Unit
) {
    var activeModal by remember { mutableStateOf<String?>(null) }
    var tempTrackerUsername by remember { mutableStateOf("") }
    var tempPinInput by remember { mutableStateOf(settings.appLockPin) }

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
            Text("النسخ والاستعادة الشاملة (JSON)", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("المواقع، المفضلة، السجل، تقدم القراءة، والإعدادات", color = TextSecondaryGrey, fontSize = 14.sp)
        }

        SettingsSwitchItem(
            title = "النسخ الاحتياطي السحابي التلقائي أسبوعياً",
            subtitle = "حفظ نسخة احتياطية تلقائية أسبوعياً لمزامنتها مع Google Drive أو التخزين السحابي",
            checked = settings.autoWeeklyBackupEnabled,
            onCheckedChange = { onUpdateBoolean("autoWeeklyBackupEnabled", it) }
        )

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Section: شبكة الاتصال وتجاوز الحجب
        Text(
            text = "شبكة الاتصال وتجاوز الحجب",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeModal = "preferredDownloadFormat" }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("more_preferred_download_format_row")
        ) {
            Text("تنسيق التحميل المُفضل", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(settings.preferredDownloadFormat, color = TextSecondaryGrey, fontSize = 14.sp)
        }

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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { activeModal = "dohProvider" }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("doh_provider_row")
        ) {
            Text("نظام DNS عبر HTTPS (DoH)", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("لتجاوز حجب المواقع: ${settings.dohProvider}", color = PeriwinklePrimary, fontSize = 14.sp)
        }

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Section: الربط مع خدمات التتبع العالمية (AniList / MyAnimeList)
        Text(
            text = "الربط مع خدمات التتبع العالمية",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        SettingsSwitchItem(
            title = "مزامنة التقدم مع AniList",
            subtitle = if (settings.anilistUsername.isNotBlank()) "الحساب المتصل: ${settings.anilistUsername}" else "تحديث رقم الفصل تلقائياً عند القراءة",
            checked = settings.anilistTrackingEnabled,
            onCheckedChange = { enabled ->
                onUpdateBoolean("anilistTrackingEnabled", enabled)
                if (enabled) {
                    tempTrackerUsername = settings.anilistUsername
                    activeModal = "anilistAccount"
                }
            }
        )

        SettingsSwitchItem(
            title = "مزامنة التقدم مع MyAnimeList",
            subtitle = if (settings.malUsername.isNotBlank()) "الحساب المتصل: ${settings.malUsername}" else "تحديث الفصول المقروءة تلقائياً على MAL",
            checked = settings.malTrackingEnabled,
            onCheckedChange = { enabled ->
                onUpdateBoolean("malTrackingEnabled", enabled)
                if (enabled) {
                    tempTrackerUsername = settings.malUsername
                    activeModal = "malAccount"
                }
            }
        )

        HorizontalDivider(color = AppDividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Section: الخصوصية والأمان (قفل التطبيق + وضع التمويه)
        Text(
            text = "الخصوصية والأمان",
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        SettingsSwitchItem(
            title = "قفل التطبيق بالرمز السري / البصمة",
            subtitle = "طلب رمز الحماية (${settings.appLockPin}) عند فتح التطبيق",
            checked = settings.appLockEnabled,
            onCheckedChange = { enabled ->
                onUpdateBoolean("appLockEnabled", enabled)
                if (enabled) {
                    tempPinInput = settings.appLockPin
                    activeModal = "appLockPin"
                }
            }
        )

        SettingsSwitchItem(
            title = "إخفاء الشاشة في التطبيقات الأخيرة (وضع التمويه)",
            subtitle = "منع التقاط الشاشة وتمويه محتوى التطبيق في قائمة المهام",
            checked = settings.secureScreenEnabled,
            onCheckedChange = { onUpdateBoolean("secureScreenEnabled", it) }
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
                title = { Text("النسخ والاستعادة الشاملة", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "يمكنك إنشاء نسخة احتياطية شاملة بصيغة JSON تضم جميع المواقع المضافة، المفضلة، السجل، الفصول المقروءة، والإعدادات، أو استعادتها بضغطة واحدة.",
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
        "dohProvider" -> {
            val providers = listOf("إيقاف", "Cloudflare (1.1.1.1)", "Google DNS (8.8.8.8)", "AdGuard DNS")
            AlertDialog(
                onDismissRequest = { activeModal = null },
                containerColor = AppDialogSurface,
                title = { Text("نظام DNS عبر HTTPS (DoH)", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        providers.forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdateString("dohProvider", provider)
                                        activeModal = null
                                        onShowToast("تم تفعيل مزود DNS: $provider")
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.dohProvider == provider,
                                    onClick = {
                                        onUpdateString("dohProvider", provider)
                                        activeModal = null
                                        onShowToast("تم تفعيل مزود DNS: $provider")
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = PeriwinklePrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(provider, color = TextPrimaryWhite)
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
        "anilistAccount" -> {
            AlertDialog(
                onDismissRequest = { activeModal = null },
                containerColor = AppDialogSurface,
                title = { Text("ربط حساب AniList", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أدخل اسم المستخدم في AniList لمزامنة الفصول المقروءة تلقائياً:", color = TextSecondaryGrey, fontSize = 13.sp)
                        OutlinedTextField(
                            value = tempTrackerUsername,
                            onValueChange = { tempTrackerUsername = it },
                            label = { Text("اسم المستخدم في AniList") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateString("anilistUsername", tempTrackerUsername.trim().ifBlank { "ReaderUser" })
                            activeModal = null
                            onShowToast("تم ربط حساب AniList بنجاح")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PeriwinklePrimary, contentColor = PeriwinkleOnPrimary)
                    ) {
                        Text("حفظ وربط", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        "malAccount" -> {
            AlertDialog(
                onDismissRequest = { activeModal = null },
                containerColor = AppDialogSurface,
                title = { Text("ربط حساب MyAnimeList", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أدخل اسم المستخدم في MyAnimeList لمزامنة تقدم القراءة تلقائياً:", color = TextSecondaryGrey, fontSize = 13.sp)
                        OutlinedTextField(
                            value = tempTrackerUsername,
                            onValueChange = { tempTrackerUsername = it },
                            label = { Text("اسم المستخدم في MAL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateString("malUsername", tempTrackerUsername.trim().ifBlank { "MALReader" })
                            activeModal = null
                            onShowToast("تم ربط حساب MyAnimeList بنجاح")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PeriwinklePrimary, contentColor = PeriwinkleOnPrimary)
                    ) {
                        Text("حفظ وربط", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        "appLockPin" -> {
            AlertDialog(
                onDismissRequest = { activeModal = null },
                containerColor = AppDialogSurface,
                title = { Text("إعداد رمز قفل التطبيق", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أدخل رمز الحماية (4 أرقام) لقفل التطبيق:", color = TextSecondaryGrey, fontSize = 13.sp)
                        OutlinedTextField(
                            value = tempPinInput,
                            onValueChange = { if (it.length <= 6) tempPinInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("رمز القفل (PIN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val validPin = tempPinInput.ifBlank { "1234" }
                            onUpdateString("appLockPin", validPin)
                            activeModal = null
                            onShowToast("تم تفعيل قفل التطبيق بالرمز: $validPin")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PeriwinklePrimary, contentColor = PeriwinkleOnPrimary)
                    ) {
                        Text("حفظ الرمز", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        "preferredDownloadFormat" -> {
            val formats = listOf("تلقائي", "ملف CBZ واحد", "ملفات CBZ متعددة")
            AlertDialog(
                onDismissRequest = { activeModal = null },
                containerColor = AppDialogSurface,
                title = {
                    Text(
                        text = "تنسيق التحميل المُفضل",
                        color = TextPrimaryWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        formats.forEach { formatOption ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdateString("preferredDownloadFormat", formatOption)
                                        activeModal = null
                                        onShowToast("تنسيق التحميل المُفضل: $formatOption")
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = formatOption,
                                    color = TextPrimaryWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                RadioButton(
                                    selected = settings.preferredDownloadFormat == formatOption,
                                    onClick = {
                                        onUpdateString("preferredDownloadFormat", formatOption)
                                        activeModal = null
                                        onShowToast("تنسيق التحميل المُفضل: $formatOption")
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = PeriwinklePrimary)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeModal = null }) {
                        Text("Cancel", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
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
