package com.stefanoneve.ultimatenotes.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue

@Composable
fun UltimateNotesTheme(
    themeId: String,
    content: @Composable () -> Unit,
) {
    val style = themeById(themeId)
    CompositionLocalProvider(LocalAppStyle provides style) {
        MaterialTheme(
            colorScheme = style.colorScheme.animated(),
            typography = themeTypography(style.displayFont, style.bodyFont),
            content = content,
        )
    }
}

/** Cross-fades the main colors when switching theme. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 400)
    val primary by animateColorAsState(primary, spec, label = "primary")
    val background by animateColorAsState(background, spec, label = "background")
    val surface by animateColorAsState(surface, spec, label = "surface")
    val surfaceVariant by animateColorAsState(surfaceVariant, spec, label = "surfaceVariant")
    val onBackground by animateColorAsState(onBackground, spec, label = "onBackground")
    val onSurface by animateColorAsState(onSurface, spec, label = "onSurface")
    return copy(
        primary = primary,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        onBackground = onBackground,
        onSurface = onSurface,
    )
}
