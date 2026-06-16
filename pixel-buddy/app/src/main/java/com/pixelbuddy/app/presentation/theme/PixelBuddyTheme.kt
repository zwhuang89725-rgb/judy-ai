package com.pixelbuddy.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * 可用主题枚举。
 * 命名与 preview-*.html 文件对应。
 */
enum class AppTheme(val displayName: String, val emoji: String) {
    PIXEL_BUDDY("像素伙伴", "👾"),
    CANDY_LAND("糖果乐园", "🍭"),
    STARRY_FAIRYTALE("星空童话", "🌙")
}

// ═══════════════════════════════════════
//  Theme 1: Pixel Buddy — 霓虹像素风
// ═══════════════════════════════════════

val PixelBackground = Color(0xFF1A1A2E)
val PixelSurface = Color(0xFF16213E)
val PixelSurfaceLight = Color(0xFF0F3460)
val PixelNeonGreen = Color(0xFF00FF88)
val PixelPink = Color(0xFFFF6B9D)
val PixelBlue = Color(0xFF00D4FF)
val PixelYellow = Color(0xFFFFE600)
val PixelOnBackground = Color(0xFFE0FFE0)
val PixelTextDim = Color(0xFF4ECCA3)
val PixelError = Color(0xFFFF4444)

// ═══════════════════════════════════════
//  Theme 2: Candy Land — 软萌糖果色
// ═══════════════════════════════════════

val CandyBackground = Color(0xFFFFF5F5)
val CandySurface = Color(0xFFFFF0F0)
val CandySurfaceLight = Color(0xFFFFE4E4)
val CandyPrimary = Color(0xFFFFB5C2)
val CandySecondary = Color(0xFFA8E6CF)
val CandyTertiary = Color(0xFFFFD3B6)
val CandyOnBackground = Color(0xFF5C4A4A)
val CandyTextDim = Color(0xFFB5A0A0)
val CandyTextOnPrimary = Color(0xFF5C4A4A)

// ═══════════════════════════════════════
//  Theme 3: Starry Fairytale — 星空童话
// ═══════════════════════════════════════

val StarryBackground = Color(0xFF1B2A4A)
val StarrySurface = Color(0xFF223355)
val StarrySurfaceLight = Color(0xFF2A4066)
val StarryPrimary = Color(0xFFF9D56E)    // 月光金
val StarrySecondary = Color(0xFFC3AED6)  // 星空紫
val StarryTertiary = Color(0xFF88CCFF)   // 星光蓝
val StarryOnBackground = Color(0xFFE8E0F0)
val StarryTextDim = Color(0xFF9A8CB4)
val StarryError = Color(0xFFFF6B6B)

// ═══════════════════════════════════════
//  Color Schemes
// ═══════════════════════════════════════

private val PixelDarkColorScheme = darkColorScheme(
    primary = PixelNeonGreen,
    onPrimary = PixelBackground,
    primaryContainer = PixelSurfaceLight,
    onPrimaryContainer = PixelNeonGreen,
    secondary = PixelPink,
    onSecondary = PixelBackground,
    secondaryContainer = PixelSurfaceLight,
    onSecondaryContainer = PixelPink,
    tertiary = PixelBlue,
    onTertiary = PixelBackground,
    background = PixelBackground,
    onBackground = PixelOnBackground,
    surface = PixelSurface,
    onSurface = PixelOnBackground,
    surfaceVariant = PixelSurfaceLight,
    onSurfaceVariant = PixelTextDim,
    error = PixelError,
    outline = PixelNeonGreen.copy(alpha = 0.4f),
    outlineVariant = PixelNeonGreen.copy(alpha = 0.15f)
)

private val CandyLightColorScheme = lightColorScheme(
    primary = CandyPrimary,
    onPrimary = CandyTextOnPrimary,
    primaryContainer = CandySurfaceLight,
    onPrimaryContainer = CandyPrimary,
    secondary = CandySecondary,
    onSecondary = CandyTextOnPrimary,
    secondaryContainer = CandySurfaceLight,
    onSecondaryContainer = CandySecondary,
    tertiary = CandyTertiary,
    onTertiary = CandyTextOnPrimary,
    background = CandyBackground,
    onBackground = CandyOnBackground,
    surface = CandySurface,
    onSurface = CandyOnBackground,
    surfaceVariant = CandySurfaceLight,
    onSurfaceVariant = CandyTextDim,
    error = PixelError,
    outline = CandyPrimary.copy(alpha = 0.4f),
    outlineVariant = CandyPrimary.copy(alpha = 0.15f)
)

private val StarryDarkColorScheme = darkColorScheme(
    primary = StarryPrimary,
    onPrimary = StarryBackground,
    primaryContainer = StarrySurfaceLight,
    onPrimaryContainer = StarryPrimary,
    secondary = StarrySecondary,
    onSecondary = StarryBackground,
    secondaryContainer = StarrySurfaceLight,
    onSecondaryContainer = StarrySecondary,
    tertiary = StarryTertiary,
    onTertiary = StarryBackground,
    background = StarryBackground,
    onBackground = StarryOnBackground,
    surface = StarrySurface,
    onSurface = StarryOnBackground,
    surfaceVariant = StarrySurfaceLight,
    onSurfaceVariant = StarryTextDim,
    error = StarryError,
    outline = StarryPrimary.copy(alpha = 0.4f),
    outlineVariant = StarryPrimary.copy(alpha = 0.15f)
)

// Pixel typography
val PixelTypography = Typography(
    // 像素标题 — 用等宽字体模拟
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 2.sp,
        color = PixelNeonGreen
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 1.5.sp,
        color = PixelNeonGreen
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 1.sp,
        color = PixelNeonGreen
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.sp,
        color = PixelOnBackground
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp,
        color = PixelOnBackground
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp,
        color = PixelOnBackground
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 1.5.sp,
        color = PixelNeonGreen
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        color = PixelTextDim
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = PixelTextDim
    )
)

@Composable
fun PixelBuddyTheme(
    theme: AppTheme = AppTheme.PIXEL_BUDDY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.PIXEL_BUDDY -> PixelDarkColorScheme
        AppTheme.CANDY_LAND -> CandyLightColorScheme
        AppTheme.STARRY_FAIRYTALE -> StarryDarkColorScheme
    }
    val view = LocalView.current

    val statusBarColor = when (theme) {
        AppTheme.PIXEL_BUDDY -> PixelBackground
        AppTheme.CANDY_LAND -> CandyBackground
        AppTheme.STARRY_FAIRYTALE -> StarryBackground
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = statusBarColor.toArgb()
            window.navigationBarColor = statusBarColor.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = theme == AppTheme.CANDY_LAND
                isAppearanceLightNavigationBars = theme == AppTheme.CANDY_LAND
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelTypography,
        content = content
    )
}
