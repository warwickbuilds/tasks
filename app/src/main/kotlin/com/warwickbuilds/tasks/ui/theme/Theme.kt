package com.warwickbuilds.tasks.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** "Calm": soft sage greens on an off-white ground. */
val CalmLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF2F6B5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E8DF),
    onPrimaryContainer = Color(0xFF14382D),
    secondary = Color(0xFF4F6359),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6EBE5),
    onSecondaryContainer = Color(0xFF1B1F1C),
    background = Color(0xFFF4F6F2),
    onBackground = Color(0xFF1B1F1C),
    surface = Color(0xFFF4F6F2),
    onSurface = Color(0xFF1B1F1C),
    surfaceVariant = Color(0xFFE6EBE5),
    onSurfaceVariant = Color(0xFF5C645E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FAF7),
    surfaceContainer = Color(0xFFEEF1EC),
    surfaceContainerHigh = Color(0xFFE8ECE6),
    surfaceContainerHighest = Color(0xFFE2E7E1),
    outline = Color(0xFF7D8A82),
    outlineVariant = Color(0xFFD3DAD4),
    error = Color(0xFFA0402F),
    onError = Color(0xFFFFFFFF),
)

val CalmDark: ColorScheme = darkColorScheme(
    primary = Color(0xFF8FD1B8),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF1F4C3F),
    onPrimaryContainer = Color(0xFFD5E8DF),
    secondary = Color(0xFFB4CCBF),
    onSecondary = Color(0xFF1F352B),
    secondaryContainer = Color(0xFF2A322D),
    onSecondaryContainer = Color(0xFFE1E3DF),
    background = Color(0xFF111412),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF111412),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF2A322D),
    onSurfaceVariant = Color(0xFFBFC9C2),
    surfaceContainerLowest = Color(0xFF0C0F0D),
    surfaceContainerLow = Color(0xFF191C1A),
    surfaceContainer = Color(0xFF1D211E),
    surfaceContainerHigh = Color(0xFF272B28),
    surfaceContainerHighest = Color(0xFF323633),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF3F4943),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF5F1508),
)

private val base = Typography()

val TasksTypography = Typography(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Bold),
)

val MonoStyle = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp, lineHeight = 22.sp)

@Composable
fun TasksTheme(wallpaperColours: Boolean, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        wallpaperColours && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> CalmDark
        else -> CalmLight
    }
    MaterialTheme(colorScheme = scheme, typography = TasksTypography, content = content)
}

/** Tag colours: a text colour and a soft background for each, in light and dark. */
data class TagTone(val content: Color, val container: Color)

private val lightTones = listOf(
    TagTone(Color(0xFF2F6B5A), Color(0xFFD5E8DF)), // sage
    TagTone(Color(0xFF8A4B14), Color(0xFFF6E2CF)), // amber
    TagTone(Color(0xFF2F5C8A), Color(0xFFDCE7F3)), // blue
    TagTone(Color(0xFF6A4A8C), Color(0xFFEADFF3)), // violet
    TagTone(Color(0xFF8A3D5B), Color(0xFFF5DDE6)), // rose
    TagTone(Color(0xFF3D6B1F), Color(0xFFE1EED3)), // leaf
    TagTone(Color(0xFF1F6670), Color(0xFFD5ECEE)), // teal
    TagTone(Color(0xFF5C5A55), Color(0xFFE7E5E0)), // stone
)

private val darkTones = listOf(
    TagTone(Color(0xFF8FD1B8), Color(0xFF1F4C3F)),
    TagTone(Color(0xFFF2BC8C), Color(0xFF4A2E12)),
    TagTone(Color(0xFFA6C8EC), Color(0xFF1C3550)),
    TagTone(Color(0xFFD3B8EE), Color(0xFF3A2A4F)),
    TagTone(Color(0xFFF0B2C9), Color(0xFF4D2335)),
    TagTone(Color(0xFFB4D99A), Color(0xFF263D17)),
    TagTone(Color(0xFF93D3DA), Color(0xFF153D42)),
    TagTone(Color(0xFFCFCCC5), Color(0xFF3A3935)),
)

@Composable
fun tagTone(color: Int): TagTone {
    val tones = if (isSystemInDarkTheme()) darkTones else lightTones
    return tones[Math.floorMod(color, tones.size)]
}

@Composable
fun allTagTones(): List<TagTone> = if (isSystemInDarkTheme()) darkTones else lightTones
