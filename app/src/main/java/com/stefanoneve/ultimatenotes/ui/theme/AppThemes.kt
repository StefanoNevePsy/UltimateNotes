package com.stefanoneve.ultimatenotes.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A theme is more than a palette: it carries a graphic "personality" —
 * corner softness, glass translucency, hand-drawn borders — so switching
 * theme changes how the whole app is drawn, not just its colors.
 */
data class AppStyle(
    val id: String,
    val name: String,
    val dark: Boolean,
    val colorScheme: ColorScheme,
    /** Base corner radius for cards, pills, sheets. */
    val corner: Dp,
    /** Alpha of floating glass surfaces (toolbars, search, header). */
    val glassAlpha: Float,
    /** When true, cards and chips get sketchy hand-drawn ink borders. */
    val handDrawn: Boolean,
    /** Accent gradient used for the FAB and highlights. */
    val gradient: List<Color>,
    /** Preview swatch for the theme picker. */
    val swatch: List<Color>,
)

val LatteTheme = AppStyle(
    id = "latte",
    name = "Latte",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF6356E5),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5E1FF),
        onPrimaryContainer = Color(0xFF2A2370),
        secondary = Color(0xFFE5735A),
        onSecondary = Color.White,
        background = Color(0xFFFAF7F2),
        onBackground = Color(0xFF2D2A26),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF2D2A26),
        surfaceVariant = Color(0xFFF1ECE3),
        onSurfaceVariant = Color(0xFF5F5A52),
        outline = Color(0xFF9A938A),
        outlineVariant = Color(0xFFDCD5C9),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
    ),
    corner = 24.dp,
    glassAlpha = 0.78f,
    handDrawn = false,
    gradient = listOf(Color(0xFF6356E5), Color(0xFFB05AE5)),
    swatch = listOf(Color(0xFFFAF7F2), Color(0xFF6356E5), Color(0xFFE5735A)),
)

val SepiaTheme = AppStyle(
    id = "sepia",
    name = "Seppia",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF8A5A2B),
        onPrimary = Color(0xFFFFF6E8),
        primaryContainer = Color(0xFFEDDCC2),
        onPrimaryContainer = Color(0xFF4A2F12),
        secondary = Color(0xFFA8552F),
        onSecondary = Color(0xFFFFF6E8),
        background = Color(0xFFF4EAD8),
        onBackground = Color(0xFF433726),
        surface = Color(0xFFFBF3E4),
        onSurface = Color(0xFF433726),
        surfaceVariant = Color(0xFFEADFC8),
        onSurfaceVariant = Color(0xFF6E5F45),
        outline = Color(0xFF8C7B5D),
        outlineVariant = Color(0xFFD6C8AB),
        errorContainer = Color(0xFFF2D4C4),
        onErrorContainer = Color(0xFF7A2E0E),
    ),
    corner = 14.dp,
    glassAlpha = 0.88f,
    handDrawn = true,
    gradient = listOf(Color(0xFFA8552F), Color(0xFF8A5A2B)),
    swatch = listOf(Color(0xFFF4EAD8), Color(0xFF8A5A2B), Color(0xFFA8552F)),
)

val NordicTheme = AppStyle(
    id = "nordic",
    name = "Nordic",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF5E81AC),
        onPrimary = Color(0xFFECEFF4),
        primaryContainer = Color(0xFFD8E2EE),
        onPrimaryContainer = Color(0xFF2E3440),
        secondary = Color(0xFF88C0D0),
        onSecondary = Color(0xFF2E3440),
        background = Color(0xFFECEFF4),
        onBackground = Color(0xFF2E3440),
        surface = Color(0xFFF8FAFC),
        onSurface = Color(0xFF2E3440),
        surfaceVariant = Color(0xFFE0E5EE),
        onSurfaceVariant = Color(0xFF4C566A),
        outline = Color(0xFF7B88A1),
        outlineVariant = Color(0xFFCDD6E4),
        errorContainer = Color(0xFFF1D4D7),
        onErrorContainer = Color(0xFFBF616A),
    ),
    corner = 16.dp,
    glassAlpha = 0.8f,
    handDrawn = false,
    gradient = listOf(Color(0xFF5E81AC), Color(0xFF88C0D0)),
    swatch = listOf(Color(0xFFECEFF4), Color(0xFF5E81AC), Color(0xFF88C0D0)),
)

val ForestTheme = AppStyle(
    id = "forest",
    name = "Foresta",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFF8FBC8F),
        onPrimary = Color(0xFF15291B),
        primaryContainer = Color(0xFF2C4434),
        onPrimaryContainer = Color(0xFFC9E5C9),
        secondary = Color(0xFFD8B45A),
        onSecondary = Color(0xFF2A2208),
        background = Color(0xFF16241B),
        onBackground = Color(0xFFDCE8DC),
        surface = Color(0xFF1D2F23),
        onSurface = Color(0xFFDCE8DC),
        surfaceVariant = Color(0xFF27392D),
        onSurfaceVariant = Color(0xFFA8BCA8),
        outline = Color(0xFF6F8473),
        outlineVariant = Color(0xFF3A4F40),
        errorContainer = Color(0xFF5C2B23),
        onErrorContainer = Color(0xFFF2B8AC),
    ),
    corner = 18.dp,
    glassAlpha = 0.72f,
    handDrawn = true,
    gradient = listOf(Color(0xFF8FBC8F), Color(0xFFD8B45A)),
    swatch = listOf(Color(0xFF16241B), Color(0xFF8FBC8F), Color(0xFFD8B45A)),
)

val NightTheme = AppStyle(
    id = "dark",
    name = "Notte",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFF8E85FF),
        onPrimary = Color(0xFF1C1840),
        primaryContainer = Color(0xFF37316B),
        onPrimaryContainer = Color(0xFFDDD9FF),
        secondary = Color(0xFFFF9E80),
        onSecondary = Color(0xFF3A1505),
        background = Color(0xFF15151E),
        onBackground = Color(0xFFE6E4EE),
        surface = Color(0xFF1C1C28),
        onSurface = Color(0xFFE6E4EE),
        surfaceVariant = Color(0xFF262635),
        onSurfaceVariant = Color(0xFFAFACC0),
        outline = Color(0xFF6E6B80),
        outlineVariant = Color(0xFF35334A),
        errorContainer = Color(0xFF5C2333),
        onErrorContainer = Color(0xFFFFB3C0),
    ),
    corner = 24.dp,
    glassAlpha = 0.7f,
    handDrawn = false,
    gradient = listOf(Color(0xFF8E85FF), Color(0xFFFF9E80)),
    swatch = listOf(Color(0xFF15151E), Color(0xFF8E85FF), Color(0xFFFF9E80)),
)

val DraculaTheme = AppStyle(
    id = "dracula",
    name = "Dracula",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFFBD93F9),
        onPrimary = Color(0xFF1E1F29),
        primaryContainer = Color(0xFF44475A),
        onPrimaryContainer = Color(0xFFE9DDFF),
        secondary = Color(0xFFFF79C6),
        onSecondary = Color(0xFF2A0E1F),
        tertiary = Color(0xFF8BE9FD),
        background = Color(0xFF282A36),
        onBackground = Color(0xFFF8F8F2),
        surface = Color(0xFF2F3140),
        onSurface = Color(0xFFF8F8F2),
        surfaceVariant = Color(0xFF383B4D),
        onSurfaceVariant = Color(0xFFBFC2D4),
        outline = Color(0xFF6272A4),
        outlineVariant = Color(0xFF44475A),
        errorContainer = Color(0xFF55303A),
        onErrorContainer = Color(0xFFFF5555),
    ),
    corner = 12.dp,
    glassAlpha = 0.75f,
    handDrawn = false,
    gradient = listOf(Color(0xFFBD93F9), Color(0xFFFF79C6)),
    swatch = listOf(Color(0xFF282A36), Color(0xFFBD93F9), Color(0xFFFF79C6)),
)

val OledTheme = AppStyle(
    id = "oled",
    name = "OLED",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFF7DD3FC),
        onPrimary = Color(0xFF062536),
        primaryContainer = Color(0xFF12303F),
        onPrimaryContainer = Color(0xFFC8ECFF),
        secondary = Color(0xFFA5B4FC),
        onSecondary = Color(0xFF10153A),
        background = Color(0xFF000000),
        onBackground = Color(0xFFE7E7EA),
        surface = Color(0xFF0A0A0C),
        onSurface = Color(0xFFE7E7EA),
        surfaceVariant = Color(0xFF131318),
        onSurfaceVariant = Color(0xFF9D9DA8),
        outline = Color(0xFF55555F),
        outlineVariant = Color(0xFF222228),
        errorContainer = Color(0xFF3A1218),
        onErrorContainer = Color(0xFFFB7185),
    ),
    corner = 28.dp,
    glassAlpha = 0.62f,
    handDrawn = false,
    gradient = listOf(Color(0xFF7DD3FC), Color(0xFFA5B4FC)),
    swatch = listOf(Color(0xFF000000), Color(0xFF7DD3FC), Color(0xFFA5B4FC)),
)

val AllThemes: List<AppStyle> = listOf(
    LatteTheme, SepiaTheme, NordicTheme, ForestTheme,
    NightTheme, DraculaTheme, OledTheme,
)

fun themeById(id: String): AppStyle = AllThemes.firstOrNull { it.id == id } ?: LatteTheme

val LocalAppStyle = staticCompositionLocalOf { LatteTheme }
