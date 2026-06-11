package com.stefanoneve.ultimatenotes.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ColorPalette(
    val id: String,
    val name: String,
    val colors: List<Long>,
)

/** Built-in palettes; user-created ones live in AppSettings.customPalettes. */
val BuiltInPalettes: List<ColorPalette> = listOf(
    ColorPalette(
        "classic", "Classica",
        listOf(
            0xFF1A1A1A, 0xFF6B7280, 0xFFEF4444, 0xFFF97316,
            0xFFEAB308, 0xFF22C55E, 0xFF0EA5E9, 0xFF3B82F6,
            0xFF8B5CF6, 0xFFEC4899, 0xFF92400E, 0xFFFFFFFF,
        ),
    ),
    ColorPalette(
        "pastel", "Pastello",
        listOf(
            0xFF44403C, 0xFFA8A29E, 0xFFFCA5A5, 0xFFFDBA74,
            0xFFFDE047, 0xFF86EFAC, 0xFF7DD3FC, 0xFFA5B4FC,
            0xFFD8B4FE, 0xFFF9A8D4, 0xFFD6BC9F, 0xFFFFFFFF,
        ),
    ),
    ColorPalette(
        "neon", "Neon",
        listOf(
            0xFF0F172A, 0xFF64748B, 0xFFFF2D55, 0xFFFF9F0A,
            0xFFFFE600, 0xFF39FF14, 0xFF00F5FF, 0xFF2979FF,
            0xFFBF5AF2, 0xFFFF2DCB, 0xFF00FFA3, 0xFFFFFFFF,
        ),
    ),
    ColorPalette(
        "earth", "Terra",
        listOf(
            0xFF3E2C1C, 0xFF8C7B5D, 0xFFA8552F, 0xFFC97B3D,
            0xFFD8B45A, 0xFF6B8E23, 0xFF4F7942, 0xFF2F6F6F,
            0xFF5E81AC, 0xFF7D6699, 0xFFB36A5E, 0xFFF4EAD8,
        ),
    ),
    ColorPalette(
        "mono", "Mono",
        listOf(
            0xFF000000, 0xFF1F2937, 0xFF374151, 0xFF4B5563,
            0xFF6B7280, 0xFF9CA3AF, 0xFFD1D5DB, 0xFFE5E7EB,
            0xFFF3F4F6, 0xFFFFFFFF, 0xFF111827, 0xFF030712,
        ),
    ),
    ColorPalette(
        "fantasy_ink", "Inchiostri antichi",
        listOf(
            0xFF3B2A1A, 0xFF7A1F1F, 0xFF9C6F1E, 0xFF3F5C3A,
            0xFF34425E, 0xFF6B4A2F, 0xFF8D744E, 0xFF552E5E,
            0xFF1F4548, 0xFFA8552F, 0xFFD8B45A, 0xFFEFDFC2,
        ),
    ),
    ColorPalette(
        "chalk", "Gessetti",
        listOf(
            0xFFF5F1E6, 0xFFF2E37C, 0xFFF2B8C6, 0xFFA8D8EA,
            0xFFB5E3B5, 0xFFF2C879, 0xFFD9B8F2, 0xFFF2A07C,
            0xFFC9E5DC, 0xFFE8E8E8, 0xFFFFD3D3, 0xFFCFE3F5,
        ),
    ),
    ColorPalette(
        "retro16", "VGA 16",
        listOf(
            0xFF000000, 0xFF808080, 0xFFC0C0C0, 0xFFFFFFFF,
            0xFF800000, 0xFFFF0000, 0xFF808000, 0xFFFFFF00,
            0xFF008000, 0xFF00FF00, 0xFF008080, 0xFF00FFFF,
            0xFF000080, 0xFF0000FF, 0xFF800080, 0xFFFF00FF,
        ),
    ),
    ColorPalette(
        "phosphor", "Fosforo",
        listOf(
            0xFF00FF66, 0xFF9CFF57, 0xFF38E8C2, 0xFF00CC52,
            0xFF2A5C3F, 0xFFB8F5CC, 0xFFFFBF00, 0xFFFF8800,
            0xFF7FBF96, 0xFF1C3826, 0xFFE0FFE8, 0xFF55FFAA,
        ),
    ),
    ColorPalette(
        "neonwave", "Neon Wave",
        listOf(
            0xFFFF71CE, 0xFF01CDFE, 0xFF05FFA1, 0xFFB967FF,
            0xFFFFFB96, 0xFFFF2DCB, 0xFF7DF9FF, 0xFFFE4164,
            0xFF8A2BE2, 0xFF00F5D4, 0xFFFFFFFF, 0xFF1A0B2E,
        ),
    ),
)

/** Candidate colors offered when composing a custom palette. */
val PaletteCandidateColors: List<Long> = buildList {
    // Grays.
    addAll(listOf(0xFF000000, 0xFF374151, 0xFF6B7280, 0xFFD1D5DB, 0xFFFFFFFF))
    // Hue ramp, three shades each.
    val hues = listOf(
        0xFFEF4444 to 0xFFFCA5A5, 0xFFF97316 to 0xFFFDBA74,
        0xFFEAB308 to 0xFFFDE047, 0xFF84CC16 to 0xFFBEF264,
        0xFF22C55E to 0xFF86EFAC, 0xFF14B8A6 to 0xFF5EEAD4,
        0xFF0EA5E9 to 0xFF7DD3FC, 0xFF3B82F6 to 0xFF93C5FD,
        0xFF8B5CF6 to 0xFFC4B5FD, 0xFFD946EF to 0xFFF0ABFC,
        0xFFEC4899 to 0xFFF9A8D4, 0xFF92400E to 0xFFD6BC9F,
    )
    hues.forEach { (strong, light) ->
        add(strong)
        add(light)
    }
}
