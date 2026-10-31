package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_user_settings")

data class UserSettings(
    val startScreen: String = "التخزين",
    val recommendationNotifications: Boolean = true,
    val appTheme: String = "DYNAMIC", // "DEFAULT", "DYNAMIC", "GREEN", "TIDAL", "YIN_YANG", "YOTSUBA"
    val nightMode: String = "اتبع مظهر النظام",
    val pureDarkMode: Boolean = false,
    val rtlMenus: Boolean = false,
    val hideToolbarOnScroll: Boolean = true,
    val showContinueReadingButton: Boolean = true,
    val showSiteIcons: Boolean = true,
    val readingMode: String = "الصفحات",
    val pageFitMode: String = "تناسب الشاشة",
    val scrollDirection: String = "من اليسار لليمين",
    val volumeKeysScroll: Boolean = false,
    val autoScroll: Boolean = false,
    val autoScrollSpeed: Float = 0.25f,
    val webtoonAlwaysOn: Boolean = true,
    val webtoonAutoDetect: Boolean = true,
    val webtoonZoom: Boolean = true,
    val tapToOpenMenu: Boolean = false,
    val showBatteryAndTime: Boolean = false,
    val showPageNumber: Boolean = true,
    val showCommentsAtEnd: Boolean = true,
    val loadPagesWifiOnly: Boolean = true,
    val loadPagesMode: String = "عبر واي فاي فقط",
    val hideMenuButton: Boolean = true,
    val customBrightnessEnabled: Boolean = false,
    val customBrightnessLevel: Int = 20,
    val keepScreenOn: Boolean = false,
    val checkNewChapters: Boolean = true,
    val checkNewChaptersWifiOnly: Boolean = true,
    val maxConcurrentDownloads: Int = 4,
    val downloadWifiOnly: Boolean = false,
    val userProfileName: String = "",
    val userProfileEmail: String = "",
    val searchHistoryCount: Int = 0,
    val totalReadingSeconds: Long = 35L,
    val browserCoachmarkShown: Boolean = false,
    // Reader UX Enhancements
    val autoCropBorders: Boolean = false,
    val preloadNextChapter: Boolean = true,
    val tapZoneMode: String = "افتراضي (يمين/يسار/منتصف)", // "افتراضي (يمين/يسار/منتصف)", "معكوس (يسار للتالي)", "منتصف فقط للقائمة", "شبيه كيندل"
    val readerColorFilter: String = "طبيعي (افتراضي)", // "طبيعي (افتراضي)" | "دافئ لحماية العين (Sepia)" | "عكس الألوان الذكي (ليلي)" | "تدرج رمادي مريح (Grayscale)"
    val splitLongStripPages: Boolean = true,
    // Downloads & Offline Storage
    val preferredDownloadFormat: String = "تلقائي", // "تلقائي", "ملف CBZ واحد", "ملفات CBZ متعددة"
    val autoDeleteReadChapters: Boolean = false,
    val keepLastReadChaptersCount: Int = 2,
    val compressDownloadsWebp: Boolean = false,
    val nightlyDownloadScheduleEnabled: Boolean = false,
    val autoWeeklyBackupEnabled: Boolean = true,
    val readingStreakDays: Int = 3,
    val foldableCompactScale: Boolean = true,
    // Network & DNS over HTTPS (DoH)
    val dohProvider: String = "معطل", // "معطل", "Cloudflare (1.1.1.1)", "Google (8.8.8.8)", "AdGuard"
    // Global Tracking (AniList / MyAnimeList)
    val anilistUsername: String = "",
    val anilistToken: String = "",
    val malUsername: String = "",
    val autoSyncTracking: Boolean = true,
    val anilistTrackingEnabled: Boolean = false,
    val malTrackingEnabled: Boolean = false,
    // Security & Privacy
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "1234",
    val secureScreenEnabled: Boolean = false
)

class UserSettingsDataStore(private val context: Context) {

    private object Keys {
        val START_SCREEN = stringPreferencesKey("start_screen")
        val REC_NOTIFICATIONS = booleanPreferencesKey("rec_notifications")
        val APP_THEME = stringPreferencesKey("app_theme")
        val NIGHT_MODE = stringPreferencesKey("night_mode")
        val PURE_DARK_MODE = booleanPreferencesKey("pure_dark_mode")
        val RTL_MENUS = booleanPreferencesKey("rtl_menus")
        val HIDE_TOOLBAR_SCROLL = booleanPreferencesKey("hide_toolbar_scroll")
        val SHOW_CONTINUE_BTN = booleanPreferencesKey("show_continue_btn")
        val SHOW_SITE_ICONS = booleanPreferencesKey("show_site_icons")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val PAGE_FIT_MODE = stringPreferencesKey("page_fit_mode")
        val SCROLL_DIRECTION = stringPreferencesKey("scroll_direction")
        val VOLUME_KEYS_SCROLL = booleanPreferencesKey("volume_keys_scroll")
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        val AUTO_SCROLL_SPEED = floatPreferencesKey("auto_scroll_speed")
        val WEBTOON_ALWAYS_ON = booleanPreferencesKey("webtoon_always_on")
        val WEBTOON_AUTO_DETECT = booleanPreferencesKey("webtoon_auto_detect")
        val WEBTOON_ZOOM = booleanPreferencesKey("webtoon_zoom")
        val TAP_TO_OPEN_MENU = booleanPreferencesKey("tap_to_open_menu")
        val SHOW_BATTERY_TIME = booleanPreferencesKey("show_battery_time")
        val SHOW_PAGE_NUMBER = booleanPreferencesKey("show_page_number")
        val SHOW_COMMENTS_END = booleanPreferencesKey("show_comments_end")
        val LOAD_PAGES_WIFI_ONLY = booleanPreferencesKey("load_pages_wifi_only")
        val LOAD_PAGES_MODE = stringPreferencesKey("load_pages_mode")
        val HIDE_MENU_BUTTON = booleanPreferencesKey("hide_menu_button")
        val CUSTOM_BRIGHTNESS_ENABLED = booleanPreferencesKey("custom_brightness_enabled")
        val CUSTOM_BRIGHTNESS_LEVEL = intPreferencesKey("custom_brightness_level")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val CHECK_NEW_CHAPTERS = booleanPreferencesKey("check_new_chapters")
        val CHECK_NEW_CHAPTERS_WIFI = booleanPreferencesKey("check_new_chapters_wifi")
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val USER_PROFILE_NAME = stringPreferencesKey("user_profile_name")
        val USER_PROFILE_EMAIL = stringPreferencesKey("user_profile_email")
        val SEARCH_HISTORY_COUNT = intPreferencesKey("search_history_count")
        val TOTAL_READING_SECONDS = longPreferencesKey("total_reading_seconds")
        val BROWSER_COACHMARK_SHOWN = booleanPreferencesKey("browser_coachmark_shown")
        val AUTO_CROP_BORDERS = booleanPreferencesKey("auto_crop_borders")
        val PRELOAD_NEXT_CHAPTER = booleanPreferencesKey("preload_next_chapter")
        val TAP_ZONE_MODE = stringPreferencesKey("tap_zone_mode")
        val READER_COLOR_FILTER = stringPreferencesKey("reader_color_filter")
        val SPLIT_LONG_STRIP_PAGES = booleanPreferencesKey("split_long_strip_pages")
        val PREFERRED_DOWNLOAD_FORMAT = stringPreferencesKey("preferred_download_format")
        val AUTO_DELETE_READ_CHAPTERS = booleanPreferencesKey("auto_delete_read_chapters")
        val KEEP_LAST_READ_CHAPTERS_COUNT = intPreferencesKey("keep_last_read_chapters_count")
        val COMPRESS_DOWNLOADS_WEBP = booleanPreferencesKey("compress_downloads_webp")
        val NIGHTLY_DOWNLOAD_SCHEDULE_ENABLED = booleanPreferencesKey("nightly_download_schedule_enabled")
        val AUTO_WEEKLY_BACKUP_ENABLED = booleanPreferencesKey("auto_weekly_backup_enabled")
        val READING_STREAK_DAYS = intPreferencesKey("reading_streak_days")
        val FOLDABLE_COMPACT_SCALE = booleanPreferencesKey("foldable_compact_scale")
        val DOH_PROVIDER = stringPreferencesKey("doh_provider")
        val ANILIST_USERNAME = stringPreferencesKey("anilist_username")
        val ANILIST_TOKEN = stringPreferencesKey("anilist_token")
        val MAL_USERNAME = stringPreferencesKey("mal_username")
        val AUTO_SYNC_TRACKING = booleanPreferencesKey("auto_sync_tracking")
        val ANILIST_TRACKING_ENABLED = booleanPreferencesKey("anilist_tracking_enabled")
        val MAL_TRACKING_ENABLED = booleanPreferencesKey("mal_tracking_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val SECURE_SCREEN_ENABLED = booleanPreferencesKey("secure_screen_enabled")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            startScreen = prefs[Keys.START_SCREEN] ?: "التخزين",
            recommendationNotifications = prefs[Keys.REC_NOTIFICATIONS] ?: true,
            appTheme = prefs[Keys.APP_THEME] ?: "DYNAMIC",
            nightMode = prefs[Keys.NIGHT_MODE] ?: "اتبع مظهر النظام",
            pureDarkMode = prefs[Keys.PURE_DARK_MODE] ?: false,
            rtlMenus = prefs[Keys.RTL_MENUS] ?: false,
            hideToolbarOnScroll = prefs[Keys.HIDE_TOOLBAR_SCROLL] ?: true,
            showContinueReadingButton = prefs[Keys.SHOW_CONTINUE_BTN] ?: true,
            showSiteIcons = prefs[Keys.SHOW_SITE_ICONS] ?: true,
            readingMode = prefs[Keys.READING_MODE] ?: "الصفحات",
            pageFitMode = prefs[Keys.PAGE_FIT_MODE] ?: "تناسب الشاشة",
            scrollDirection = prefs[Keys.SCROLL_DIRECTION] ?: "من اليسار لليمين",
            volumeKeysScroll = prefs[Keys.VOLUME_KEYS_SCROLL] ?: false,
            autoScroll = prefs[Keys.AUTO_SCROLL] ?: false,
            autoScrollSpeed = prefs[Keys.AUTO_SCROLL_SPEED] ?: 0.25f,
            webtoonAlwaysOn = prefs[Keys.WEBTOON_ALWAYS_ON] ?: true,
            webtoonAutoDetect = prefs[Keys.WEBTOON_AUTO_DETECT] ?: true,
            webtoonZoom = prefs[Keys.WEBTOON_ZOOM] ?: true,
            tapToOpenMenu = prefs[Keys.TAP_TO_OPEN_MENU] ?: false,
            showBatteryAndTime = prefs[Keys.SHOW_BATTERY_TIME] ?: false,
            showPageNumber = prefs[Keys.SHOW_PAGE_NUMBER] ?: true,
            showCommentsAtEnd = prefs[Keys.SHOW_COMMENTS_END] ?: true,
            loadPagesWifiOnly = prefs[Keys.LOAD_PAGES_WIFI_ONLY] ?: true,
            loadPagesMode = prefs[Keys.LOAD_PAGES_MODE] ?: "عبر واي فاي فقط",
            hideMenuButton = prefs[Keys.HIDE_MENU_BUTTON] ?: true,
            customBrightnessEnabled = prefs[Keys.CUSTOM_BRIGHTNESS_ENABLED] ?: false,
            customBrightnessLevel = prefs[Keys.CUSTOM_BRIGHTNESS_LEVEL] ?: 20,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
            checkNewChapters = prefs[Keys.CHECK_NEW_CHAPTERS] ?: true,
            checkNewChaptersWifiOnly = prefs[Keys.CHECK_NEW_CHAPTERS_WIFI] ?: true,
            maxConcurrentDownloads = prefs[Keys.MAX_CONCURRENT_DOWNLOADS] ?: 4,
            downloadWifiOnly = prefs[Keys.DOWNLOAD_WIFI_ONLY] ?: false,
            userProfileName = prefs[Keys.USER_PROFILE_NAME] ?: "",
            userProfileEmail = prefs[Keys.USER_PROFILE_EMAIL] ?: "",
            searchHistoryCount = prefs[Keys.SEARCH_HISTORY_COUNT] ?: 0,
            totalReadingSeconds = prefs[Keys.TOTAL_READING_SECONDS] ?: 35L,
            browserCoachmarkShown = prefs[Keys.BROWSER_COACHMARK_SHOWN] ?: false,
            autoCropBorders = prefs[Keys.AUTO_CROP_BORDERS] ?: false,
            preloadNextChapter = prefs[Keys.PRELOAD_NEXT_CHAPTER] ?: true,
            tapZoneMode = prefs[Keys.TAP_ZONE_MODE] ?: "افتراضي (يمين/يسار/منتصف)",
            readerColorFilter = prefs[Keys.READER_COLOR_FILTER] ?: "طبيعي (افتراضي)",
            splitLongStripPages = prefs[Keys.SPLIT_LONG_STRIP_PAGES] ?: true,
            preferredDownloadFormat = prefs[Keys.PREFERRED_DOWNLOAD_FORMAT] ?: "تلقائي",
            autoDeleteReadChapters = prefs[Keys.AUTO_DELETE_READ_CHAPTERS] ?: false,
            keepLastReadChaptersCount = prefs[Keys.KEEP_LAST_READ_CHAPTERS_COUNT] ?: 2,
            compressDownloadsWebp = prefs[Keys.COMPRESS_DOWNLOADS_WEBP] ?: false,
            nightlyDownloadScheduleEnabled = prefs[Keys.NIGHTLY_DOWNLOAD_SCHEDULE_ENABLED] ?: false,
            autoWeeklyBackupEnabled = prefs[Keys.AUTO_WEEKLY_BACKUP_ENABLED] ?: true,
            readingStreakDays = prefs[Keys.READING_STREAK_DAYS] ?: 3,
            foldableCompactScale = prefs[Keys.FOLDABLE_COMPACT_SCALE] ?: true,
            dohProvider = prefs[Keys.DOH_PROVIDER] ?: "معطل",
            anilistUsername = prefs[Keys.ANILIST_USERNAME] ?: "",
            anilistToken = prefs[Keys.ANILIST_TOKEN] ?: "",
            malUsername = prefs[Keys.MAL_USERNAME] ?: "",
            autoSyncTracking = prefs[Keys.AUTO_SYNC_TRACKING] ?: true,
            anilistTrackingEnabled = prefs[Keys.ANILIST_TRACKING_ENABLED] ?: false,
            malTrackingEnabled = prefs[Keys.MAL_TRACKING_ENABLED] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appLockPin = prefs[Keys.APP_LOCK_PIN] ?: "1234",
            secureScreenEnabled = prefs[Keys.SECURE_SCREEN_ENABLED] ?: false
        )
    }

    suspend fun updateString(keyName: String, value: String) {
        context.dataStore.edit { prefs ->
            when (keyName) {
                "startScreen" -> prefs[Keys.START_SCREEN] = value
                "appTheme" -> prefs[Keys.APP_THEME] = value
                "nightMode" -> prefs[Keys.NIGHT_MODE] = value
                "readingMode" -> prefs[Keys.READING_MODE] = value
                "pageFitMode" -> prefs[Keys.PAGE_FIT_MODE] = value
                "scrollDirection" -> prefs[Keys.SCROLL_DIRECTION] = value
                "loadPagesMode" -> prefs[Keys.LOAD_PAGES_MODE] = value
                "userProfileName" -> prefs[Keys.USER_PROFILE_NAME] = value
                "userProfileEmail" -> prefs[Keys.USER_PROFILE_EMAIL] = value
                "tapZoneMode" -> prefs[Keys.TAP_ZONE_MODE] = value
                "readerColorFilter" -> prefs[Keys.READER_COLOR_FILTER] = value
                "preferredDownloadFormat" -> prefs[Keys.PREFERRED_DOWNLOAD_FORMAT] = value
                "dohProvider" -> prefs[Keys.DOH_PROVIDER] = value
                "anilistUsername" -> prefs[Keys.ANILIST_USERNAME] = value
                "anilistToken" -> prefs[Keys.ANILIST_TOKEN] = value
                "malUsername" -> prefs[Keys.MAL_USERNAME] = value
                "appLockPin" -> prefs[Keys.APP_LOCK_PIN] = value
            }
        }
    }

    suspend fun updateBoolean(keyName: String, value: Boolean) {
        context.dataStore.edit { prefs ->
            when (keyName) {
                "recommendationNotifications" -> prefs[Keys.REC_NOTIFICATIONS] = value
                "pureDarkMode" -> prefs[Keys.PURE_DARK_MODE] = value
                "rtlMenus" -> prefs[Keys.RTL_MENUS] = value
                "hideToolbarOnScroll" -> prefs[Keys.HIDE_TOOLBAR_SCROLL] = value
                "showContinueReadingButton" -> prefs[Keys.SHOW_CONTINUE_BTN] = value
                "showSiteIcons" -> prefs[Keys.SHOW_SITE_ICONS] = value
                "volumeKeysScroll" -> prefs[Keys.VOLUME_KEYS_SCROLL] = value
                "autoScroll" -> prefs[Keys.AUTO_SCROLL] = value
                "webtoonAlwaysOn" -> prefs[Keys.WEBTOON_ALWAYS_ON] = value
                "webtoonAutoDetect" -> prefs[Keys.WEBTOON_AUTO_DETECT] = value
                "webtoonZoom" -> prefs[Keys.WEBTOON_ZOOM] = value
                "tapToOpenMenu" -> prefs[Keys.TAP_TO_OPEN_MENU] = value
                "showBatteryAndTime" -> prefs[Keys.SHOW_BATTERY_TIME] = value
                "showPageNumber" -> prefs[Keys.SHOW_PAGE_NUMBER] = value
                "showCommentsAtEnd" -> prefs[Keys.SHOW_COMMENTS_END] = value
                "loadPagesWifiOnly" -> prefs[Keys.LOAD_PAGES_WIFI_ONLY] = value
                "hideMenuButton" -> prefs[Keys.HIDE_MENU_BUTTON] = value
                "customBrightnessEnabled" -> prefs[Keys.CUSTOM_BRIGHTNESS_ENABLED] = value
                "keepScreenOn" -> prefs[Keys.KEEP_SCREEN_ON] = value
                "checkNewChapters" -> prefs[Keys.CHECK_NEW_CHAPTERS] = value
                "checkNewChaptersWifiOnly" -> prefs[Keys.CHECK_NEW_CHAPTERS_WIFI] = value
                "downloadWifiOnly" -> prefs[Keys.DOWNLOAD_WIFI_ONLY] = value
                "browserCoachmarkShown" -> prefs[Keys.BROWSER_COACHMARK_SHOWN] = value
                "autoCropBorders" -> prefs[Keys.AUTO_CROP_BORDERS] = value
                "preloadNextChapter" -> prefs[Keys.PRELOAD_NEXT_CHAPTER] = value
                "splitLongStripPages" -> prefs[Keys.SPLIT_LONG_STRIP_PAGES] = value
                "autoDeleteReadChapters" -> prefs[Keys.AUTO_DELETE_READ_CHAPTERS] = value
                "compressDownloadsWebp" -> prefs[Keys.COMPRESS_DOWNLOADS_WEBP] = value
                "nightlyDownloadScheduleEnabled" -> prefs[Keys.NIGHTLY_DOWNLOAD_SCHEDULE_ENABLED] = value
                "autoWeeklyBackupEnabled" -> prefs[Keys.AUTO_WEEKLY_BACKUP_ENABLED] = value
                "foldableCompactScale" -> prefs[Keys.FOLDABLE_COMPACT_SCALE] = value
                "autoSyncTracking" -> prefs[Keys.AUTO_SYNC_TRACKING] = value
                "anilistTrackingEnabled" -> prefs[Keys.ANILIST_TRACKING_ENABLED] = value
                "malTrackingEnabled" -> prefs[Keys.MAL_TRACKING_ENABLED] = value
                "appLockEnabled" -> prefs[Keys.APP_LOCK_ENABLED] = value
                "secureScreenEnabled" -> prefs[Keys.SECURE_SCREEN_ENABLED] = value
            }
        }
    }

    suspend fun updateInt(keyName: String, value: Int) {
        context.dataStore.edit { prefs ->
            when (keyName) {
                "customBrightnessLevel" -> prefs[Keys.CUSTOM_BRIGHTNESS_LEVEL] = value
                "maxConcurrentDownloads" -> prefs[Keys.MAX_CONCURRENT_DOWNLOADS] = value
                "searchHistoryCount" -> prefs[Keys.SEARCH_HISTORY_COUNT] = value
                "keepLastReadChaptersCount" -> prefs[Keys.KEEP_LAST_READ_CHAPTERS_COUNT] = value
                "readingStreakDays" -> prefs[Keys.READING_STREAK_DAYS] = value
            }
        }
    }

    suspend fun updateFloat(keyName: String, value: Float) {
        context.dataStore.edit { prefs ->
            if (keyName == "autoScrollSpeed") {
                prefs[Keys.AUTO_SCROLL_SPEED] = value
            }
        }
    }

    suspend fun incrementSearchCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SEARCH_HISTORY_COUNT] ?: 0
            prefs[Keys.SEARCH_HISTORY_COUNT] = current + 1
        }
    }

    suspend fun clearSearchCount() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SEARCH_HISTORY_COUNT] = 0
        }
    }

    suspend fun addReadingSeconds(seconds: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TOTAL_READING_SECONDS] ?: 0L
            prefs[Keys.TOTAL_READING_SECONDS] = current + seconds
        }
    }

    suspend fun resetStatistics() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOTAL_READING_SECONDS] = 0L
        }
    }

    suspend fun getSnapshot(): UserSettings {
        return settingsFlow.first()
    }
}
