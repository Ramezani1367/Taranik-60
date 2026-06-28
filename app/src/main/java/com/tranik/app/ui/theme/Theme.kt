package com.tranik.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// ========== Colors ==========
val DarkBg = Color(0xFF0D0D12)
val DarkBg2 = Color(0xFF13131A)
val DarkBg3 = Color(0xFF1A1A24)
val DarkBg4 = Color(0xFF22222E)
val DarkBg5 = Color(0xFF2A2A38)

val Accent = Color(0xFF7C6FE0)
val Accent2 = Color(0xFF9D92E8)
val Pink = Color(0xFFE054A0)

val Text1 = Color(0xFFFFFFFF)
val Text2 = Color(0xFFC8C8D8)
val Text3 = Color(0xFF8888A0)
val Text4 = Color(0xFF555568)

val Green = Color(0xFF1DB954)
val Red = Color(0xFFE05555)

// ========== Color Schemes ==========
private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = Accent2,
    tertiary = Pink,
    background = DarkBg,
    surface = DarkBg2,
    surfaceVariant = DarkBg3,
    onPrimary = Color.White,
    onBackground = Text1,
    onSurface = Text1,
    onSurfaceVariant = Text3,
    error = Red,
    outline = Text4,
    outlineVariant = DarkBg4,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5C4FC0),
    secondary = Color(0xFF7D72D8),
    tertiary = Color(0xFFC04480),
    background = Color(0xFFF5F5FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8E8F0),
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFF666680),
    error = Color(0xFFD04040),
    outline = Color(0xFFAAAA88),
)

// ========== Shapes ==========
val TarAnikShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

// ========== Theme ==========
@Composable
fun TarAnikTheme(
    theme: String = "dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        "light" -> LightColorScheme
        "system" -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TarAnikTypography,
        shapes = TarAnikShapes,
        content = content
    )
}
