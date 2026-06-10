package com.stefanoneve.ultimatenotes.data.fonts

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class AppFont(
    val id: String,
    val name: String,
    val family: FontFamily,
)

/**
 * Provides the list of usable fonts: system families plus any .ttf/.otf the
 * user imported (copied into filesDir/fonts).
 */
class FontManager(private val context: Context) {

    private val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }

    private val systemFonts = listOf(
        AppFont("default", "Predefinito", FontFamily.Default),
        AppFont("serif", "Serif", FontFamily.Serif),
        AppFont("mono", "Monospace", FontFamily.Monospace),
        AppFont("cursive", "Corsivo", FontFamily.Cursive),
    )

    private val _fonts = MutableStateFlow(load())
    val fonts: StateFlow<List<AppFont>> = _fonts

    fun byId(id: String?): AppFont =
        _fonts.value.firstOrNull { it.id == id } ?: systemFonts.first()

    private fun load(): List<AppFont> {
        val imported = fontsDir.listFiles { f ->
            f.extension.lowercase() in listOf("ttf", "otf")
        }.orEmpty().sortedBy { it.name }.mapNotNull { file ->
            runCatching {
                AppFont(
                    id = "file:${file.name}",
                    name = file.nameWithoutExtension,
                    family = FontFamily(Font(file)),
                )
            }.getOrNull()
        }
        return systemFonts + imported
    }

    /** Copies a user-picked font file into the app and refreshes the list. */
    fun importFont(uri: Uri): AppFont? {
        val doc = DocumentFile.fromSingleUri(context, uri)
        val name = doc?.name ?: "font_${System.currentTimeMillis()}.ttf"
        if (!name.lowercase().endsWith(".ttf") && !name.lowercase().endsWith(".otf")) return null
        val dest = File(fontsDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: return null
        _fonts.value = load()
        return _fonts.value.firstOrNull { it.id == "file:$name" }
    }

    fun removeFont(id: String) {
        if (!id.startsWith("file:")) return
        File(fontsDir, id.removePrefix("file:")).delete()
        _fonts.value = load()
    }
}
