package com.stefanoneve.ultimatenotes.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.FrameShape
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import com.stefanoneve.ultimatenotes.data.model.TapePattern

/** How floating bars/cards are rendered: each theme picks its own world. */
enum class BarStyle {
    /** Translucent modern glass. */
    GLASS,

    /** Solid paper with hand-drawn ink borders (analog themes). */
    PAPER,

    /** Win95-style raised 3D bevel panels. */
    BEVEL,
}

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
    /** Font pairing: display drives headers/titles, body drives text/labels. */
    val displayFont: androidx.compose.ui.text.font.FontFamily = SerifFamily,
    val bodyFont: androidx.compose.ui.text.font.FontFamily = SansFamily,
    /** FontManager ids of the pairing (for note text and EditText spans). */
    val displayFontId: String = "lora",
    val bodyFontId: String = "default",
    /** Rendering style of toolbars, search pill and floating panels. */
    val barStyle: BarStyle = BarStyle.GLASS,
    /** Defaults applied to newly created canvas objects. */
    val frameShape: FrameShape = FrameShape.ROUNDED,
    val frameLineStyle: LineStyle = LineStyle.SOLID,
    val connectorLineStyle: LineStyle = LineStyle.SOLID,
    val canvasBackground: CanvasBackground = CanvasBackground.DOTS,
    /** Built-in palette that feels at home in this theme. */
    val defaultPaletteId: String = "classic",
    /**
     * Element color slots (connectors, frames, text accents): picking a slot
     * stores its index, so the element re-colors when the theme changes.
     * null derives a 6-slot palette from the scheme.
     */
    val elementColors: List<Long>? = null,
    /** Sticky-note colors; null derives soft tints from the scheme. */
    val stickyColors: List<Long>? = null,
    /** Washi-tape colors; null derives from the scheme. */
    val tapeColors: List<Long>? = null,
    /** Default washi-tape pattern for this theme. */
    val tapePattern: TapePattern = TapePattern.STRIPES,
    /** Signature block decor for "auto" text-block skins. */
    val blockDecor: String = "glass",
    /** Motion personality: retro = instant, fantasy = gentle, modern = springy. */
    val motionStiffness: Float = 700f,
    val motionDamping: Float = 0.55f,
) {
    /** Theme accent as packed ARGB, for new connectors/frames/tape. */
    fun accentArgb(): Long = gradient.first().toArgb().toLong() and 0xFFFFFFFFL

    fun resolvedElementColors(): List<Long> = elementColors ?: listOf(
        gradient.first().toArgb().toLong() and 0xFFFFFFFFL,
        gradient.last().toArgb().toLong() and 0xFFFFFFFFL,
        colorScheme.secondary.toArgb().toLong() and 0xFFFFFFFFL,
        colorScheme.tertiary.toArgb().toLong() and 0xFFFFFFFFL,
        colorScheme.onSurfaceVariant.toArgb().toLong() and 0xFFFFFFFFL,
        colorScheme.outline.toArgb().toLong() and 0xFFFFFFFFL,
    )

    fun resolvedStickyColors(): List<Long> = stickyColors ?: listOf(
        0xFFFFF3A8, // classic post-it yellow
        softTint(colorScheme.primary.toArgb().toLong() and 0xFFFFFFFFL),
        softTint(colorScheme.secondary.toArgb().toLong() and 0xFFFFFFFFL),
        0xFFD9F2D9,
        0xFFFCE0EC,
    )

    fun resolvedTapeColors(): List<Long> = tapeColors ?: listOf(
        gradient.first().toArgb().toLong() and 0xFFFFFFFFL,
        gradient.last().toArgb().toLong() and 0xFFFFFFFFL,
        0xFFF2C879,
        0xFF8FBC8F,
        0xFFEC4899,
        0xFF6B7280,
    )
}

/** Blends a color toward white for readable sticky/pastel tints. */
fun softTint(color: Long): Long {
    val r = ((color shr 16) and 0xFF).toInt()
    val g = ((color shr 8) and 0xFF).toInt()
    val b = (color and 0xFF).toInt()
    fun soften(v: Int) = (v + (255 - v) * 0.65f).toInt().coerceIn(0, 255)
    return 0xFF000000 or
        (soften(r).toLong() shl 16) or (soften(g).toLong() shl 8) or soften(b).toLong()
}

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

/** Fantasy parchment: epic Cinzel headers, warm inks, hand-drawn borders. */
val FantasyTheme = AppStyle(
    id = "fantasy",
    name = "Pergamena",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF7A1F1F),
        onPrimary = Color(0xFFF7ECD4),
        primaryContainer = Color(0xFFE9D5AC),
        onPrimaryContainer = Color(0xFF4A1212),
        secondary = Color(0xFF9C6F1E),
        onSecondary = Color(0xFFFFF6E0),
        background = Color(0xFFF0E2C4),
        onBackground = Color(0xFF3B2A1A),
        surface = Color(0xFFF7ECD4),
        onSurface = Color(0xFF3B2A1A),
        surfaceVariant = Color(0xFFE7D7B4),
        onSurfaceVariant = Color(0xFF6C5638),
        outline = Color(0xFF8D744E),
        outlineVariant = Color(0xFFD2BE96),
        errorContainer = Color(0xFFEFD0BC),
        onErrorContainer = Color(0xFF7A1F1F),
    ),
    corner = 10.dp,
    glassAlpha = 0.9f,
    handDrawn = true,
    gradient = listOf(Color(0xFF7A1F1F), Color(0xFF9C6F1E)),
    swatch = listOf(Color(0xFFF0E2C4), Color(0xFF7A1F1F), Color(0xFF9C6F1E)),
    displayFont = CinzelFamily,
    bodyFont = SerifFamily,
    displayFontId = "cinzel",
    bodyFontId = "lora",
)

/** Windows 95 nostalgia: teal desktop, gray panels, zero rounding. */
val RetroTheme = AppStyle(
    id = "win95",
    name = "Retro 95",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF000080),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB0B0CF),
        onPrimaryContainer = Color(0xFF000050),
        secondary = Color(0xFF008080),
        onSecondary = Color(0xFFFFFFFF),
        background = Color(0xFF008080),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFFC0C0C0),
        onSurface = Color(0xFF111111),
        surfaceVariant = Color(0xFFAFAFAF),
        onSurfaceVariant = Color(0xFF333333),
        outline = Color(0xFF555555),
        outlineVariant = Color(0xFF8E8E8E),
        errorContainer = Color(0xFFE0B0B0),
        onErrorContainer = Color(0xFF800000),
    ),
    corner = 0.dp,
    glassAlpha = 1f,
    handDrawn = false,
    gradient = listOf(Color(0xFF000080), Color(0xFF008080)),
    swatch = listOf(Color(0xFF008080), Color(0xFFC0C0C0), Color(0xFF000080)),
    displayFont = PixelFamily,
    bodyFont = androidx.compose.ui.text.font.FontFamily.Default,
    displayFontId = "vt323",
    bodyFontId = "system",
)

/** Green phosphor terminal, all pixel type. */
val TerminalTheme = AppStyle(
    id = "terminal",
    name = "Terminal",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFF00FF66),
        onPrimary = Color(0xFF002211),
        primaryContainer = Color(0xFF043A1F),
        onPrimaryContainer = Color(0xFFA4FFC8),
        secondary = Color(0xFF38E8C2),
        onSecondary = Color(0xFF00231C),
        background = Color(0xFF060E08),
        onBackground = Color(0xFFB8F5CC),
        surface = Color(0xFF0B1810),
        onSurface = Color(0xFFB8F5CC),
        surfaceVariant = Color(0xFF12251A),
        onSurfaceVariant = Color(0xFF7FBF96),
        outline = Color(0xFF3E7A55),
        outlineVariant = Color(0xFF1C3826),
        errorContainer = Color(0xFF3A1212),
        onErrorContainer = Color(0xFFFF6B6B),
    ),
    corner = 6.dp,
    glassAlpha = 0.82f,
    handDrawn = false,
    gradient = listOf(Color(0xFF00FF66), Color(0xFF38E8C2)),
    swatch = listOf(Color(0xFF060E08), Color(0xFF00FF66), Color(0xFF38E8C2)),
    displayFont = PixelFamily,
    bodyFont = PixelFamily,
    displayFontId = "vt323",
    bodyFontId = "vt323",
)

/** Notebook & handwriting: blue ink, Caveat script, sketchy borders. */
val SketchTheme = AppStyle(
    id = "sketch",
    name = "Quaderno",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF2C4FD8),
        onPrimary = Color(0xFFF6F8FF),
        primaryContainer = Color(0xFFD9E0FB),
        onPrimaryContainer = Color(0xFF15205A),
        secondary = Color(0xFFD83A3A),
        onSecondary = Color(0xFFFFF6F6),
        background = Color(0xFFFCFAF4),
        onBackground = Color(0xFF26303E),
        surface = Color(0xFFFFFFFC),
        onSurface = Color(0xFF26303E),
        surfaceVariant = Color(0xFFF0EDE2),
        onSurfaceVariant = Color(0xFF565F6E),
        outline = Color(0xFF8893A3),
        outlineVariant = Color(0xFFD9DCE2),
        errorContainer = Color(0xFFF8D7D7),
        onErrorContainer = Color(0xFFB42318),
    ),
    corner = 12.dp,
    glassAlpha = 0.9f,
    handDrawn = true,
    gradient = listOf(Color(0xFF2C4FD8), Color(0xFFD83A3A)),
    swatch = listOf(Color(0xFFFCFAF4), Color(0xFF2C4FD8), Color(0xFFD83A3A)),
    displayFont = HandFamily,
    bodyFont = HandFamily,
    displayFontId = "caveat",
    bodyFontId = "caveat",
)

/** Vaporwave: deep purple, neon pink/cyan, pixel display type. */
val VaporwaveTheme = AppStyle(
    id = "vaporwave",
    name = "Vaporwave",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFFFF71CE),
        onPrimary = Color(0xFF3A0822),
        primaryContainer = Color(0xFF551640),
        onPrimaryContainer = Color(0xFFFFD2EC),
        secondary = Color(0xFF01CDFE),
        onSecondary = Color(0xFF002B36),
        tertiary = Color(0xFF05FFA1),
        background = Color(0xFF1A0B2E),
        onBackground = Color(0xFFEFE3FF),
        surface = Color(0xFF241240),
        onSurface = Color(0xFFEFE3FF),
        surfaceVariant = Color(0xFF301A52),
        onSurfaceVariant = Color(0xFFC2AEE0),
        outline = Color(0xFF7C63A8),
        outlineVariant = Color(0xFF3E2A63),
        errorContainer = Color(0xFF551429),
        onErrorContainer = Color(0xFFFF8AB3),
    ),
    corner = 4.dp,
    glassAlpha = 0.72f,
    handDrawn = false,
    gradient = listOf(Color(0xFFFF71CE), Color(0xFF01CDFE)),
    swatch = listOf(Color(0xFF1A0B2E), Color(0xFFFF71CE), Color(0xFF01CDFE)),
    displayFont = PixelFamily,
    bodyFont = SansFamily,
    displayFontId = "vt323",
    bodyFontId = "default",
)

/** Dark coffee counterpart of Seppia. */
val SepiaDarkTheme = AppStyle(
    id = "sepia_dark",
    name = "Caffè",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFFD8B45A),
        onPrimary = Color(0xFF332408),
        primaryContainer = Color(0xFF54431C),
        onPrimaryContainer = Color(0xFFF2E0B5),
        secondary = Color(0xFFCE8B5C),
        onSecondary = Color(0xFF341B0A),
        background = Color(0xFF221A12),
        onBackground = Color(0xFFEADFC8),
        surface = Color(0xFF2C2218),
        onSurface = Color(0xFFEADFC8),
        surfaceVariant = Color(0xFF3A2E20),
        onSurfaceVariant = Color(0xFFC4B295),
        outline = Color(0xFF8C7B5D),
        outlineVariant = Color(0xFF4E4128),
        errorContainer = Color(0xFF5C2E1C),
        onErrorContainer = Color(0xFFF2C0A6),
    ),
    corner = 14.dp,
    glassAlpha = 0.8f,
    handDrawn = true,
    gradient = listOf(Color(0xFFD8B45A), Color(0xFFCE8B5C)),
    swatch = listOf(Color(0xFF221A12), Color(0xFFD8B45A), Color(0xFFCE8B5C)),
)

/** Nord "polar night" counterpart of Nordic. */
val NordicDarkTheme = AppStyle(
    id = "nordic_dark",
    name = "Nordic Notte",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFF88C0D0),
        onPrimary = Color(0xFF15232B),
        primaryContainer = Color(0xFF3B4252),
        onPrimaryContainer = Color(0xFFD8E2EE),
        secondary = Color(0xFF81A1C1),
        onSecondary = Color(0xFF14202E),
        background = Color(0xFF2E3440),
        onBackground = Color(0xFFE5E9F0),
        surface = Color(0xFF3B4252),
        onSurface = Color(0xFFE5E9F0),
        surfaceVariant = Color(0xFF434C5E),
        onSurfaceVariant = Color(0xFFC0C8D8),
        outline = Color(0xFF7B88A1),
        outlineVariant = Color(0xFF4C566A),
        errorContainer = Color(0xFF5A3138),
        onErrorContainer = Color(0xFFEBA5AC),
    ),
    corner = 16.dp,
    glassAlpha = 0.78f,
    handDrawn = false,
    gradient = listOf(Color(0xFF88C0D0), Color(0xFF81A1C1)),
    swatch = listOf(Color(0xFF2E3440), Color(0xFF88C0D0), Color(0xFF81A1C1)),
)

/** Chalkboard counterpart of Quaderno: chalk on dark slate, still Caveat. */
val SketchDarkTheme = AppStyle(
    id = "sketch_dark",
    name = "Lavagna",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFFF5F1E6),
        onPrimary = Color(0xFF24302B),
        primaryContainer = Color(0xFF3A4A43),
        onPrimaryContainer = Color(0xFFF5F1E6),
        secondary = Color(0xFFF2C879),
        onSecondary = Color(0xFF332508),
        background = Color(0xFF273430),
        onBackground = Color(0xFFEFEAD9),
        surface = Color(0xFF2F3E39),
        onSurface = Color(0xFFEFEAD9),
        surfaceVariant = Color(0xFF3A4A44),
        onSurfaceVariant = Color(0xFFC2C9BB),
        outline = Color(0xFF84948B),
        outlineVariant = Color(0xFF4A5A53),
        errorContainer = Color(0xFF5C3030),
        onErrorContainer = Color(0xFFF2B6B0),
    ),
    corner = 12.dp,
    glassAlpha = 0.82f,
    handDrawn = true,
    gradient = listOf(Color(0xFFF5F1E6), Color(0xFFF2C879)),
    swatch = listOf(Color(0xFF273430), Color(0xFFF5F1E6), Color(0xFFF2C879)),
    displayFont = HandFamily,
    bodyFont = HandFamily,
    displayFontId = "caveat",
    bodyFontId = "caveat",
)

/** Tavern-at-night counterpart of Pergamena: leather, embers, candlelight. */
val FantasyDarkTheme = AppStyle(
    id = "fantasy_dark",
    name = "Taverna",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFFE0A93E),
        onPrimary = Color(0xFF3A2A05),
        primaryContainer = Color(0xFF5A431A),
        onPrimaryContainer = Color(0xFFF6E2B3),
        secondary = Color(0xFFCC5F45),
        onSecondary = Color(0xFF3A1208),
        background = Color(0xFF221511),
        onBackground = Color(0xFFEFDFC2),
        surface = Color(0xFF2E1E16),
        onSurface = Color(0xFFEFDFC2),
        surfaceVariant = Color(0xFF3D2A1E),
        onSurfaceVariant = Color(0xFFC9B190),
        outline = Color(0xFF927450),
        outlineVariant = Color(0xFF52392A),
        errorContainer = Color(0xFF5E241C),
        onErrorContainer = Color(0xFFF3B5A8),
    ),
    corner = 10.dp,
    glassAlpha = 0.8f,
    handDrawn = true,
    gradient = listOf(Color(0xFFE0A93E), Color(0xFFCC5F45)),
    swatch = listOf(Color(0xFF221511), Color(0xFFE0A93E), Color(0xFFCC5F45)),
    displayFont = CinzelFamily,
    bodyFont = SerifFamily,
    displayFontId = "cinzel",
    bodyFontId = "lora",
)

/** Dark-mode counterpart of Retro 95: charcoal panels, cyan accents. */
val RetroDarkTheme = AppStyle(
    id = "win95_dark",
    name = "Retro Notte",
    dark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFF55FFFF),
        onPrimary = Color(0xFF003535),
        primaryContainer = Color(0xFF1F4A4A),
        onPrimaryContainer = Color(0xFFB8FFFF),
        secondary = Color(0xFF55FF55),
        onSecondary = Color(0xFF0A3A0A),
        background = Color(0xFF1B1B1F),
        onBackground = Color(0xFFE5E5E5),
        surface = Color(0xFF3A3A40),
        onSurface = Color(0xFFEDEDED),
        surfaceVariant = Color(0xFF4A4A52),
        onSurfaceVariant = Color(0xFFBBBBC4),
        outline = Color(0xFF8A8A94),
        outlineVariant = Color(0xFF55555E),
        errorContainer = Color(0xFF5A2222),
        onErrorContainer = Color(0xFFFF8888),
    ),
    corner = 0.dp,
    glassAlpha = 1f,
    handDrawn = false,
    gradient = listOf(Color(0xFF55FFFF), Color(0xFF55FF55)),
    swatch = listOf(Color(0xFF1B1B1F), Color(0xFF55FFFF), Color(0xFF55FF55)),
    displayFont = PixelFamily,
    bodyFont = androidx.compose.ui.text.font.FontFamily.Default,
    displayFontId = "vt323",
    bodyFontId = "system",
)

/** Daylight counterpart of Foresta: sage and cream. */
val ForestLightTheme = AppStyle(
    id = "forest_light",
    name = "Prato",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF3F7048),
        onPrimary = Color(0xFFF2F8EE),
        primaryContainer = Color(0xFFD3E8CE),
        onPrimaryContainer = Color(0xFF1C3A22),
        secondary = Color(0xFFA8842B),
        onSecondary = Color(0xFFFFF8E4),
        background = Color(0xFFF1F5EA),
        onBackground = Color(0xFF273228),
        surface = Color(0xFFFAFCF5),
        onSurface = Color(0xFF273228),
        surfaceVariant = Color(0xFFE2EBD9),
        onSurfaceVariant = Color(0xFF55675A),
        outline = Color(0xFF7F947F),
        outlineVariant = Color(0xFFCBDAC6),
        errorContainer = Color(0xFFF4D5CC),
        onErrorContainer = Color(0xFF8C2F1D),
    ),
    corner = 18.dp,
    glassAlpha = 0.82f,
    handDrawn = true,
    gradient = listOf(Color(0xFF3F7048), Color(0xFFA8842B)),
    swatch = listOf(Color(0xFFF1F5EA), Color(0xFF3F7048), Color(0xFFA8842B)),
)

/** Light counterpart of Dracula (the classic "Alucard" flavor). */
val AlucardTheme = AppStyle(
    id = "alucard",
    name = "Alucard",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF7C4DCC),
        onPrimary = Color(0xFFFCF7FF),
        primaryContainer = Color(0xFFE6D9F7),
        onPrimaryContainer = Color(0xFF38215F),
        secondary = Color(0xFFD23E8E),
        onSecondary = Color(0xFFFFF5FA),
        background = Color(0xFFFFFBEB),
        onBackground = Color(0xFF1F2228),
        surface = Color(0xFFFFFEF7),
        onSurface = Color(0xFF1F2228),
        surfaceVariant = Color(0xFFF2EDDC),
        onSurfaceVariant = Color(0xFF565143),
        outline = Color(0xFF8E8875),
        outlineVariant = Color(0xFFDCD6C0),
        errorContainer = Color(0xFFF8D3D3),
        onErrorContainer = Color(0xFFB02A2A),
    ),
    corner = 12.dp,
    glassAlpha = 0.85f,
    handDrawn = false,
    gradient = listOf(Color(0xFF7C4DCC), Color(0xFFD23E8E)),
    swatch = listOf(Color(0xFFFFFBEB), Color(0xFF7C4DCC), Color(0xFFD23E8E)),
)

/** Pastel daylight counterpart of Vaporwave. */
val VaporLightTheme = AppStyle(
    id = "vaporwave_light",
    name = "Vapor Sole",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFFE0379F),
        onPrimary = Color(0xFFFFF4FA),
        primaryContainer = Color(0xFFFAD3EA),
        onPrimaryContainer = Color(0xFF5C0F3F),
        secondary = Color(0xFF0099C7),
        onSecondary = Color(0xFFF0FBFF),
        background = Color(0xFFFDF3FA),
        onBackground = Color(0xFF31203B),
        surface = Color(0xFFFFFAFE),
        onSurface = Color(0xFF31203B),
        surfaceVariant = Color(0xFFF4E2F0),
        onSurfaceVariant = Color(0xFF6D5470),
        outline = Color(0xFFA288A8),
        outlineVariant = Color(0xFFE3CCE0),
        errorContainer = Color(0xFFFAD2D9),
        onErrorContainer = Color(0xFFA8203F),
    ),
    corner = 4.dp,
    glassAlpha = 0.8f,
    handDrawn = false,
    gradient = listOf(Color(0xFFE0379F), Color(0xFF0099C7)),
    swatch = listOf(Color(0xFFFDF3FA), Color(0xFFE0379F), Color(0xFF0099C7)),
    displayFont = PixelFamily,
    bodyFont = SansFamily,
    displayFontId = "vt323",
    bodyFontId = "default",
)

/** Paper counterpart of Terminal: typewriter ink on warm paper. */
val TerminalLightTheme = AppStyle(
    id = "terminal_light",
    name = "Macchina da scrivere",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF1F5E38),
        onPrimary = Color(0xFFF0F7EE),
        primaryContainer = Color(0xFFD2E6D2),
        onPrimaryContainer = Color(0xFF0E3220),
        secondary = Color(0xFF7A1F1F),
        onSecondary = Color(0xFFFBF1F1),
        background = Color(0xFFF5F1E3),
        onBackground = Color(0xFF26301F),
        surface = Color(0xFFFBF8EC),
        onSurface = Color(0xFF26301F),
        surfaceVariant = Color(0xFFE9E3CD),
        onSurfaceVariant = Color(0xFF5C6450),
        outline = Color(0xFF8B927C),
        outlineVariant = Color(0xFFD4CFB6),
        errorContainer = Color(0xFFF0D2C8),
        onErrorContainer = Color(0xFF7A1F1F),
    ),
    corner = 6.dp,
    glassAlpha = 0.88f,
    handDrawn = false,
    gradient = listOf(Color(0xFF1F5E38), Color(0xFF7A1F1F)),
    swatch = listOf(Color(0xFFF5F1E3), Color(0xFF1F5E38), Color(0xFF7A1F1F)),
    displayFont = PixelFamily,
    bodyFont = PixelFamily,
    displayFontId = "vt323",
    bodyFontId = "vt323",
)

/** Pure-white minimal counterpart of OLED. */
val PaperWhiteTheme = AppStyle(
    id = "paper",
    name = "Bianco",
    dark = false,
    colorScheme = lightColorScheme(
        primary = Color(0xFF0284C7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD8EEFB),
        onPrimaryContainer = Color(0xFF073B57),
        secondary = Color(0xFF6366F1),
        onSecondary = Color(0xFFFFFFFF),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF17181C),
        surface = Color(0xFFFAFAFB),
        onSurface = Color(0xFF17181C),
        surfaceVariant = Color(0xFFF0F0F2),
        onSurfaceVariant = Color(0xFF5A5B63),
        outline = Color(0xFF94959E),
        outlineVariant = Color(0xFFE2E2E7),
        errorContainer = Color(0xFFFBDADA),
        onErrorContainer = Color(0xFFB42318),
    ),
    corner = 28.dp,
    glassAlpha = 0.85f,
    handDrawn = false,
    gradient = listOf(Color(0xFF0284C7), Color(0xFF6366F1)),
    swatch = listOf(Color(0xFFFFFFFF), Color(0xFF0284C7), Color(0xFF6366F1)),
)

/**
 * Per-family personality: every graphic default (bars, frames, connectors,
 * canvas background, palettes, sticky/tape colors, fonts, motion) is tuned so
 * each theme feels like its own little world.
 */
private fun styled(t: AppStyle): AppStyle = when (t.id) {
    "sepia", "sepia_dark" -> t.copy(
        barStyle = BarStyle.PAPER,
        frameShape = FrameShape.SKETCHY,
        blockDecor = "parchment",
        canvasBackground = CanvasBackground.PAPER,
        defaultPaletteId = "earth",
        motionStiffness = 350f, motionDamping = 0.8f,
        stickyColors = listOf(0xFFF2E0B5, 0xFFE8D3A0, 0xFFE0C39B, 0xFFD9C8AC, 0xFFF0D8C8),
    )
    "fantasy", "fantasy_dark" -> t.copy(
        barStyle = BarStyle.PAPER,
        frameShape = FrameShape.SKETCHY,
        connectorLineStyle = LineStyle.DASHED,
        blockDecor = "parchment",
        canvasBackground = CanvasBackground.PAPER,
        defaultPaletteId = "fantasy_ink",
        tapePattern = TapePattern.SOLID,
        displayFont = MedievalFamily,
        bodyFont = OldBookFamily,
        displayFontId = "medieval",
        bodyFontId = "oldbook",
        motionStiffness = 300f, motionDamping = 0.85f,
        elementColors = listOf(0xFF7A1F1F, 0xFF9C6F1E, 0xFF3F5C3A, 0xFF34425E, 0xFF6B4A2F, 0xFF552E5E),
        stickyColors = listOf(0xFFEFDFB9, 0xFFE6CE9E, 0xFFD9BC85, 0xFFE8D5C0, 0xFFD7C5A8),
        tapeColors = listOf(0xFF7A1F1F, 0xFF9C6F1E, 0xFF3F5C3A, 0xFF34425E, 0xFF6B4A2F, 0xFF8D744E),
    )
    "win95", "win95_dark" -> t.copy(
        barStyle = BarStyle.BEVEL,
        frameShape = FrameShape.RECT,
        blockDecor = "window",
        canvasBackground = CanvasBackground.GRID,
        defaultPaletteId = "retro16",
        tapePattern = TapePattern.GRID,
        motionStiffness = 20000f, motionDamping = 1f,
        elementColors = listOf(0xFF000080, 0xFF008080, 0xFF800080, 0xFF800000, 0xFF008000, 0xFF000000),
        stickyColors = listOf(0xFFFFFFCC, 0xFFCCFFFF, 0xFFFFCCCC, 0xFFCCFFCC, 0xFFE0E0E0),
        tapeColors = listOf(0xFF000080, 0xFF008080, 0xFF800080, 0xFF808000, 0xFFC0C0C0, 0xFF000000),
    )
    "terminal" -> t.copy(
        barStyle = BarStyle.PAPER,
        frameShape = FrameShape.RECT,
        blockDecor = "terminal",
        canvasBackground = CanvasBackground.SCANLINES,
        defaultPaletteId = "phosphor",
        tapePattern = TapePattern.GRID,
        motionStiffness = 20000f, motionDamping = 1f,
        elementColors = listOf(0xFF00FF66, 0xFF38E8C2, 0xFF9CFF57, 0xFFFFBF00, 0xFF55FFAA, 0xFF7FBF96),
        stickyColors = listOf(0xFF12251A, 0xFF1C3826, 0xFF26402E, 0xFF143020, 0xFF0E2418),
        tapeColors = listOf(0xFF00FF66, 0xFF38E8C2, 0xFF9CFF57, 0xFFFFBF00, 0xFF2A5C3F, 0xFF1C3826),
    )
    "terminal_light" -> t.copy(
        barStyle = BarStyle.PAPER,
        frameShape = FrameShape.RECT,
        blockDecor = "parchment",
        canvasBackground = CanvasBackground.LINES,
        defaultPaletteId = "earth",
        displayFont = TypewriterFamily,
        bodyFont = TypewriterFamily,
        displayFontId = "typewriter",
        bodyFontId = "typewriter",
        motionStiffness = 350f, motionDamping = 0.85f,
        stickyColors = listOf(0xFFEFE8CF, 0xFFE2D9BC, 0xFFD8E4D0, 0xFFE8D8C8, 0xFFE5E0CB),
    )
    "sketch", "sketch_dark" -> t.copy(
        barStyle = BarStyle.PAPER,
        frameShape = FrameShape.SKETCHY,
        connectorLineStyle = LineStyle.DASHED,
        blockDecor = "sketch",
        canvasBackground = CanvasBackground.LINES,
        defaultPaletteId = if (t.id == "sketch_dark") "chalk" else "classic",
        tapePattern = TapePattern.DOTS,
        bodyFont = if (t.id == "sketch") NeatHandFamily else t.bodyFont,
        bodyFontId = if (t.id == "sketch") "patrickhand" else t.bodyFontId,
        motionStiffness = 400f, motionDamping = 0.7f,
    )
    "forest", "forest_light" -> t.copy(
        barStyle = BarStyle.PAPER,
        frameShape = FrameShape.SKETCHY,
        blockDecor = "sketch",
        defaultPaletteId = "earth",
        motionStiffness = 350f, motionDamping = 0.8f,
    )
    "vaporwave", "vaporwave_light" -> t.copy(
        frameShape = FrameShape.RECT,
        canvasBackground = CanvasBackground.GRID,
        defaultPaletteId = "neonwave",
        tapePattern = TapePattern.ZIGZAG,
        motionStiffness = 420f, motionDamping = 0.4f,
        elementColors = listOf(0xFFFF71CE, 0xFF01CDFE, 0xFF05FFA1, 0xFFB967FF, 0xFFFFFB96, 0xFFFE4164),
        tapeColors = listOf(0xFFFF71CE, 0xFF01CDFE, 0xFF05FFA1, 0xFFB967FF, 0xFFFFFB96, 0xFF7C63A8),
    )
    "dracula", "alucard" -> t.copy(
        defaultPaletteId = "neon",
        frameShape = FrameShape.ROUNDED,
    )
    "oled", "paper" -> t.copy(
        canvasBackground = CanvasBackground.BLANK,
    )
    else -> t
}

val AllThemes: List<AppStyle> = listOf(
    LatteTheme, NightTheme,
    PaperWhiteTheme, OledTheme,
    SepiaTheme, SepiaDarkTheme,
    NordicTheme, NordicDarkTheme,
    SketchTheme, SketchDarkTheme,
    FantasyTheme, FantasyDarkTheme,
    RetroTheme, RetroDarkTheme,
    ForestLightTheme, ForestTheme,
    AlucardTheme, DraculaTheme,
    VaporLightTheme, VaporwaveTheme,
    TerminalLightTheme, TerminalTheme,
).map(::styled)

/** Light ↔ dark counterpart of each theme, for the sun/moon quick toggle. */
private val CounterpartIds: Map<String, String> = buildMap {
    listOf(
        "latte" to "dark",
        "paper" to "oled",
        "sepia" to "sepia_dark",
        "nordic" to "nordic_dark",
        "sketch" to "sketch_dark",
        "fantasy" to "fantasy_dark",
        "win95" to "win95_dark",
        "forest_light" to "forest",
        "alucard" to "dracula",
        "vaporwave_light" to "vaporwave",
        "terminal_light" to "terminal",
    ).forEach { (light, dark) ->
        put(light, dark)
        put(dark, light)
    }
}

fun counterpartOf(id: String): AppStyle? = CounterpartIds[id]?.let { themeById(it) }

fun themeById(id: String): AppStyle = AllThemes.firstOrNull { it.id == id } ?: LatteTheme

val LocalAppStyle = staticCompositionLocalOf { LatteTheme }
