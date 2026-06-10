package com.stefanoneve.ultimatenotes.data.repo

import android.content.Context
import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.StyleSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppSettings(
    val styleSet: StyleSet = StyleSet(),
    /** When true only the stylus draws; fingers always pan/zoom. */
    val stylusOnlyDrawing: Boolean = true,
    val defaultBackground: CanvasBackground = CanvasBackground.DOTS,
    /** Default font id applied to new text blocks. */
    val defaultFontId: String = "default",
)

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
