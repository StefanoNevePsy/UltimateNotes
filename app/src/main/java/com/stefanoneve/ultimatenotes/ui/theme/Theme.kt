package com.stefanoneve.ultimatenotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Indigo = Color(0xFF4F46E5)
private val IndigoLight = Color(0xFF818CF8)

private val LightColors = lightColorScheme(
    primary = Indigo,
    secondary = Color(0xFF7C3AED),
    tertiary = Color(0xFF0EA5E9),
    surfaceVariant = Color(0xFFEEF0FA),
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    secondary = Color(0xFFA78BFA),
    tertiary = Color(0xFF38BDF8),
)

@Composable
fun UltimateNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
