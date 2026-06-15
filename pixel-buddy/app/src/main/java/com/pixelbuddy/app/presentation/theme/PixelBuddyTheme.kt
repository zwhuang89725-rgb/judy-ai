package com.pixelbuddy.app.presentation.theme

import android.app.Activity
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

// ═══════════════════════════════════════
//  Pixel Buddy — 霓虹像素风调色板
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

// Material3 dark color scheme
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
fun PixelBuddyTheme(content: @Composable () -> Unit) {
    val colorScheme = PixelDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PixelBackground.toArgb()
            window.navigationBarColor = PixelBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelTypography,
        content = content
    )
}
