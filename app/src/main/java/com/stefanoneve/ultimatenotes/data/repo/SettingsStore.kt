package com.stefanoneve.ultimatenotes.data.repo

import android.content.Context
import com.stefanoneve.ultimatenotes.data.model.BuiltInPalettes
import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.ColorPalette
import com.stefanoneve.ultimatenotes.data.model.StyleSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppSettings(
    val themeId: String = "latte",
    val styleSet: StyleSet = StyleSet(),
    /** When true only the stylus draws; fingers always pan/zoom. */
    val stylusOnlyDrawing: Boolean = true,
    /** When true a nearly straight stroke is cleaned up into a real line. */
    val autoStraightenStrokes: Boolean = true,
    val defaultBackground: CanvasBackground = CanvasBackground.DOTS,
    /** When true new notes take the canvas background suggested by the theme. */
    val followThemeBackground: Boolean = true,
    /** Default font id applied to new text blocks. */
    val defaultFontId: String = "default",
    /** User-created color palettes (built-in ones live in BuiltInPalettes). */
    val customPalettes: List<ColorPalette> = emptyList(),
    /** Active palette; "auto" follows the theme's suggested palette. */
    val activePaletteId: String = "auto",
) {
    fun allPalettes(): List<ColorPalette> = BuiltInPalettes + customPalettes

    fun activePalette(): ColorPalette {
        val id =
            if (activePaletteId == "auto") {
                com.stefanoneve.ultimatenotes.ui.theme.themeById(themeId).defaultPaletteId
            } else activePaletteId
        return allPalettes().firstOrNull { it.id == id } ?: BuiltInPalettes.first()
    }
}

class SettingsStore(context: Context) {

    private val file = File(context.filesDir, "settings.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        runCatching {
            file.writeText(json.encodeToString(AppSettings.serializer(), next))
        }
    }

    private fun load(): AppSettings =
        runCatching {
            json.decodeFromString(AppSettings.serializer(), file.readText())
        }.getOrDefault(AppSettings())
}
