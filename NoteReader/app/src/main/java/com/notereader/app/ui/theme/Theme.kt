package com.notereader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Stitch Palette tokens
val SpruceGreen = Color(0xFF264A42)
val SpruceGreenLight = Color(0xFF3E6259)
val SageGreen = Color(0xFFA8CFC3)

val BgLight = Color(0xFFF9F9F7)
val SideNavLight = Color(0xFFDADAD8)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE8E8E6)
val TextPrimaryLight = Color(0xFF1A1C1B)
val TextSecondaryLight = Color(0xFF414846)
val BorderLight = Color(0xFFC1C8C5)

val BgDark = Color(0xFF1A1C1B)
val SideNavDark = Color(0xFF2F3130)
val SurfaceDark = Color(0xFF242726)
val SurfaceVariantDark = Color(0xFF383C3A)
val TextPrimaryDark = Color(0xFFF1F1EF)
val TextSecondaryDark = Color(0xFFC1C8C5)
val BorderDark = Color(0xFF414846)

// Doc Format Badges
val BadgePdf = Color(0xFFE55B3C)
val BadgeMd = Color(0xFF7C5EB1)
val BadgeIpynb = Color(0xFFD99026)
val BadgeDoc = Color(0xFF31685A)

// Book Cover Palette
val NotePalette = listOf(
    "#3E6259", // Forest Green
    "#7C4D3E", // Rust / Sienna
    "#2F4F4F", // Dark Slate
    "#4A3E7C", // Deep Purple
    "#3E5F7C", // Steel Blue
    "#7C3E5F", // Mulberry
    "#8B5A2B", // Saddle Brown
    "#264A42"  // Deep Spruce
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreen,
    secondary = SpruceGreenLight,
    tertiary = BadgeIpynb,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = BgDark,
    onSecondary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = SpruceGreen,
    secondary = SpruceGreenLight,
    tertiary = BadgeIpynb,
    background = BgLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight.copy(alpha = 0.5f)
)

@Composable
fun NoteReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
