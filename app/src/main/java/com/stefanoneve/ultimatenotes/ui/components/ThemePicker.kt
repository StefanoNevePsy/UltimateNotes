package com.stefanoneve.ultimatenotes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Sun
import com.stefanoneve.ultimatenotes.ui.theme.AllThemes
import com.stefanoneve.ultimatenotes.ui.theme.AppStyle
import com.stefanoneve.ultimatenotes.ui.theme.counterpartOf
import com.stefanoneve.ultimatenotes.ui.theme.themeById

@Composable
fun ThemeSwatch(
    theme: AppStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "swatchScale",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .scale(scale)
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.sweepGradient(theme.swatch + theme.swatch.first()))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            theme.name,
            style = MaterialTheme.typography.labelSmall,
            color =
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ThemeRow(
    selectedThemeId: String,
    onSelect: (String) -> Unit,
) {
    val current = themeById(selectedThemeId)
    Column {
        // Sun/moon quick toggle: jumps to the counterpart variant.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (current.dark) Lucide.Moon else Lucide.Sun,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (current.dark) "Variante scura" else "Variante chiara",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = current.dark,
                onCheckedChange = {
                    counterpartOf(current.id)?.let { onSelect(it.id) }
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AllThemes, key = { it.id }) { theme ->
                ThemeSwatch(
                    theme = theme,
                    selected = selectedThemeId == theme.id,
                    onClick = { onSelect(theme.id) },
                )
            }
        }
    }
}

/** Quick theme switcher used from inside the editor. */
@Composable
fun ThemePickerDialog(
    selectedThemeId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema") },
        text = {
            ThemeRow(selectedThemeId = selectedThemeId, onSelect = onSelect)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        },
    )
}
