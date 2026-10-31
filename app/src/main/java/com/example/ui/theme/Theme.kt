package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun MyApplicationTheme(
    appThemeKey: String = "DYNAMIC",
    pureDarkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val bg = if (pureDarkMode) Color(0xFF000000) else AppBackgroundDark
    val surfaceColor = if (pureDarkMode) Color(0xFF000000) else AppDrawerSurface
    val cardVariantColor = if (pureDarkMode) Color(0xFF07090E) else AppCardSurface
    val dividerColor = if (pureDarkMode) Color(0xFF1D2230) else AppDividerColor

    val (primaryColor, primaryContainerColor) = when (appThemeKey) {
        "GREEN" -> GreenThemePrimary to GreenThemeContainer
        "TIDAL" -> TidalThemePrimary to TidalThemeContainer
        "YIN_YANG" -> YinYangThemePrimary to YinYangThemeContainer
        "YOTSUBA" -> YotsubaThemePrimary to YotsubaThemeContainer
        "DEFAULT" -> Color(0xFFA89AF0) to Color(0xFF2D264C)
        else -> PeriwinklePrimary to PeriwinkleContainer // DYNAMIC matches the periwinkle blue in video
    }

    val baseScheme = if (appThemeKey == "DYNAMIC_SYSTEM" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context).let { sys ->
            if (pureDarkMode) {
                sys.copy(
                    background = Color(0xFF000000),
                    surface = Color(0xFF000000),
                    surfaceVariant = Color(0xFF07090E)
                )
            } else {
                sys
            }
        }
    } else {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = PeriwinkleOnPrimary,
            primaryContainer = primaryContainerColor,
            onPrimaryContainer = PeriwinkleOnContainer,
            secondary = primaryColor,
            onSecondary = PeriwinkleOnPrimary,
            secondaryContainer = AppDrawerSelectedPill,
            onSecondaryContainer = TextPrimaryWhite,
            background = bg,
            onBackground = TextPrimaryWhite,
            surface = surfaceColor,
            onSurface = TextPrimaryWhite,
            surfaceVariant = cardVariantColor,
            onSurfaceVariant = TextSecondaryGrey,
            outline = dividerColor
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = baseScheme,
            typography = Typography,
            content = content
        )
    }
}
