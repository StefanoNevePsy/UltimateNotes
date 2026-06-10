package com.stefanoneve.ultimatenotes.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stefanoneve.ultimatenotes.data.fonts.FontManager
import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.StyleSet
import com.stefanoneve.ultimatenotes.data.repo.SettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settingsStore: SettingsStore,
    fontManager: FontManager,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onImportFont: () -> Unit,
    onDismiss: () -> Unit,
) {
    val settings by settingsStore.settings.collectAsState()
    val fonts by fontManager.fonts.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Impostazioni", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Disegna solo con la S Pen", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Le dita scorrono e zoomano il canvas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Switch(
                    checked = settings.stylusOnlyDrawing,
                    onCheckedChange = { checked ->
                        settingsStore.update { it.copy(stylusOnlyDrawing = checked) }
                    },
                )
            }
            Spacer(Modifier.height(16.dp))

            Text("Sfondo predefinito", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CanvasBackground.entries.forEach { bg ->
                    FilterChip(
                        selected = settings.defaultBackground == bg,
                        onClick = {
                            settingsStore.update { it.copy(defaultBackground = bg) }
                        },
                        label = {
                            Text(
                                when (bg) {
                                    CanvasBackground.BLANK -> "Vuoto"
                                    CanvasBackground.DOTS -> "Punti"
                                    CanvasBackground.GRID -> "Griglia"
                                    CanvasBackground.LINES -> "Righe"
                                },
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Stili di testo", style = MaterialTheme.typography.titleMedium)
            Text(
                "Personalizza dimensione e peso: le modifiche si applicano a tutte le note",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(8.dp))
            settings.styleSet.styles.forEach { style ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            style.name,
                            modifier = Modifier.weight(1f),
                            fontSize = style.fontSize.coerceAtMost(24f).sp,
                            fontWeight = FontWeight(style.fontWeight),
                        )
                        Text(
                            "${style.fontSize.toInt()} sp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.width(12.dp))
                        FilterChip(
                            selected = style.fontWeight >= 600,
                            onClick = {
                                updateStyle(settingsStore, style.id) {
                                    it.copy(fontWeight = if (it.fontWeight >= 600) 400 else 700)
                                }
                            },
                            label = { Text("Grassetto") },
                        )
                    }
                    Slider(
                        value = style.fontSize,
                        onValueChange = { v ->
                            updateStyle(settingsStore, style.id) { it.copy(fontSize = v) }
                        },
                        valueRange = 10f..48f,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Font", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            fonts.forEach { font ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        font.name,
                        fontFamily = font.family,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (font.id.startsWith("file:")) {
                        IconButton(onClick = { fontManager.removeFont(font.id) }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Rimuovi font",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            FilledTonalButton(onClick = onImportFont, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FontDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importa font (.ttf / .otf)")
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Backup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Salva o ripristina tutte le note come .zip. Nel selettore puoi " +
                    "scegliere Google Drive come destinazione.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onExportBackup, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Esporta")
                }
                FilledTonalButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Importa")
                }
            }
        }
    }
}

private fun updateStyle(
    store: SettingsStore,
    styleId: String,
    transform: (com.stefanoneve.ultimatenotes.data.model.TextStyleDef) ->
    com.stefanoneve.ultimatenotes.data.model.TextStyleDef,
) {
    store.update { settings ->
        settings.copy(
            styleSet = StyleSet(
                settings.styleSet.styles.map {
                    if (it.id == styleId) transform(it) else it
                },
            ),
        )
    }
}
