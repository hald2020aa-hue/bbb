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
    val browserCoachmarkShown: Boolean = false
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
            browserCoachmarkShown = prefs[Keys.BROWSER_COACHMARK_SHOWN] ?: false
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
            }
        }
    }

    suspend fun updateInt(keyName: String, value: Int) {
        context.dataStore.edit { prefs ->
            when (keyName) {
                "customBrightnessLevel" -> prefs[Keys.CUSTOM_BRIGHTNESS_LEVEL] = value
                "maxConcurrentDownloads" -> prefs[Keys.MAX_CONCURRENT_DOWNLOADS] = value
                "searchHistoryCount" -> prefs[Keys.SEARCH_HISTORY_COUNT] = value
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
}
