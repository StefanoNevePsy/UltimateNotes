package com.stefanoneve.ultimatenotes.data.fonts

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import com.stefanoneve.ultimatenotes.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class AppFont(
    val id: String,
    val name: String,
    val family: FontFamily,
)

/**
 * Provides the list of usable fonts: bundled (Nunito/Lora), system families
 * and any .ttf/.otf the user imported (copied into filesDir/fonts).
 * Exposes both Compose [FontFamily] and platform [Typeface] (for the native
 * markdown EditText spans).
 */
class FontManager(private val context: Context) {

    private val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
    private val typefaceCache = mutableMapOf<String, Typeface>()

    private val systemFonts = listOf(
        AppFont("default", "Nunito", com.stefanoneve.ultimatenotes.ui.theme.SansFamily),
        AppFont("lora", "Lora", com.stefanoneve.ultimatenotes.ui.theme.SerifFamily),
        AppFont("cinzel", "Cinzel", com.stefanoneve.ultimatenotes.ui.theme.CinzelFamily),
        AppFont("medieval", "Medieval", com.stefanoneve.ultimatenotes.ui.theme.MedievalFamily),
        AppFont("oldbook", "Libro antico", com.stefanoneve.ultimatenotes.ui.theme.OldBookFamily),
        AppFont("caveat", "Caveat", com.stefanoneve.ultimatenotes.ui.theme.HandFamily),
        AppFont("patrickhand", "Patrick Hand", com.stefanoneve.ultimatenotes.ui.theme.NeatHandFamily),
        AppFont("typewriter", "Macchina da scrivere", com.stefanoneve.ultimatenotes.ui.theme.TypewriterFamily),
        AppFont("vt323", "VT323", com.stefanoneve.ultimatenotes.ui.theme.PixelFamily),
        AppFont("system", "Sistema", FontFamily.Default),
        AppFont("serif", "Serif", FontFamily.Serif),
        AppFont("mono", "Monospace", FontFamily.Monospace),
        AppFont("cursive", "Corsivo", FontFamily.Cursive),
    )

    private val _fonts = MutableStateFlow(load())
    val fonts: StateFlow<List<AppFont>> = _fonts

    fun byId(id: String?): AppFont =
        _fonts.value.firstOrNull { it.id == id } ?: systemFonts.first()

    /** Platform Typeface for EditText spans; falls back to default sans. */
    fun typefaceOf(id: String?): Typeface {
        // Blank / "default" follow the bundled body sans.
        val key = id?.takeIf { it.isNotBlank() } ?: "default"
        typefaceCache[key]?.let { return it }
        val tf = runCatching {
            when (key) {
                "default" -> ResourcesCompat.getFont(context, R.font.nunito_regular)
                "lora" -> ResourcesCompat.getFont(context, R.font.lora_medium)
                "cinzel" -> ResourcesCompat.getFont(context, R.font.cinzel_regular)
                "medieval" -> ResourcesCompat.getFont(context, R.font.medievalsharp_regular)
                "oldbook" -> ResourcesCompat.getFont(context, R.font.imfell_regular)
                "caveat" -> ResourcesCompat.getFont(context, R.font.caveat_regular)
                "patrickhand" -> ResourcesCompat.getFont(context, R.font.patrickhand_regular)
                "typewriter" -> ResourcesCompat.getFont(context, R.font.specialelite_regular)
                "vt323" -> ResourcesCompat.getFont(context, R.font.vt323_regular)
                "system" -> Typeface.SANS_SERIF
                "serif" -> Typeface.SERIF
                "mono" -> Typeface.MONOSPACE
                "cursive" -> Typeface.create("cursive", Typeface.NORMAL)
                else ->
                    if (key.startsWith("file:")) {
                        Typeface.createFromFile(File(fontsDir, key.removePrefix("file:")))
                    } else null
            }
        }.getOrNull()
        // Cache only real loads, so an early failure is retried later instead
        // of being stuck on the system fallback forever.
        if (tf != null) typefaceCache[key] = tf
        return tf ?: Typeface.DEFAULT
    }

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
        typefaceCache.remove(id)
        _fonts.value = load()
    }

    /** Re-scans filesDir/fonts (e.g. after a backup restore drops files in). */
    fun reload() {
        typefaceCache.keys.filter { it.startsWith("file:") }
            .forEach { typefaceCache.remove(it) }
        _fonts.value = load()
    }
}
