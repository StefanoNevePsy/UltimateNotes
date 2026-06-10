package com.stefanoneve.ultimatenotes.ui.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stefanoneve.ultimatenotes.UltimateNotesApp
import com.stefanoneve.ultimatenotes.data.db.NoteEntity
import com.stefanoneve.ultimatenotes.data.model.ConnectorElement
import com.stefanoneve.ultimatenotes.data.model.ImageElement
import com.stefanoneve.ultimatenotes.data.model.InkStroke
import com.stefanoneve.ultimatenotes.data.model.NoteContent
import com.stefanoneve.ultimatenotes.data.model.NoteElement
import com.stefanoneve.ultimatenotes.data.model.StrokePoint
import com.stefanoneve.ultimatenotes.data.model.TextElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.hypot

enum class EditorTool { SELECT, PEN, HIGHLIGHTER, ERASER, TEXT, CONNECT }

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as UltimateNotesApp
    private val repo = app.repository
    val settingsStore = app.settingsStore
    val fontManager = app.fontManager

    val note = MutableStateFlow<NoteEntity?>(null)
    val content = MutableStateFlow(NoteContent())
    val title = MutableStateFlow("")

    val tool = MutableStateFlow(EditorTool.PEN)
    val penColor = MutableStateFlow(0xFF1A1A1A)
    val penWidth = MutableStateFlow(4f)
    val highlighterColor = MutableStateFlow(0xCCFFE066)
    val highlighterWidth = MutableStateFlow(24f)

    /** Id of the element currently selected (move/resize handles shown). */
    val selectedElementId = MutableStateFlow<String?>(null)

    /** Id of the text element currently being edited with the keyboard. */
    val editingTextId = MutableStateFlow<String?>(null)

    /** Id of the connector currently selected (handles + style bar shown). */
    val selectedConnectorId = MutableStateFlow<String?>(null)

    /** First endpoint chosen while creating a connector with the CONNECT tool. */
    val pendingConnectFrom = MutableStateFlow<String?>(null)

    /** Measured size (world units) of each element, for connector anchoring. */
    val elementSizes = androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.ui.geometry.Size>()

    val canUndo = MutableStateFlow(false)
    val canRedo = MutableStateFlow(false)

    private val undoStack = ArrayDeque<NoteContent>()
    private val redoStack = ArrayDeque<NoteContent>()
    private var saveJob: Job? = null
    private var loadedId: String? = null

    fun load(noteId: String) {
        if (loadedId == noteId) return
        loadedId = noteId
        viewModelScope.launch {
            val entity = repo.getNote(noteId) ?: return@launch
            note.value = entity
            title.value = entity.title
            content.value = repo.decodeContent(entity).let {
                if (entity.contentJson.isBlank()) {
                    it.copy(background = settingsStore.settings.value.defaultBackground)
                } else it
            }
        }
    }

    fun setTitle(value: String) {
        title.value = value
        scheduleSave()
    }

    /** Applies a content change, recording the previous state for undo. */
    fun commit(transform: (NoteContent) -> NoteContent) {
        val current = content.value
        val next = transform(current)
        if (next == current) return
        undoStack.addLast(current)
        if (undoStack.size > 64) undoStack.removeFirst()
        redoStack.clear()
        content.value = next
        updateUndoFlags()
        scheduleSave()
    }

    /** Applies a transient change (drag in progress) without an undo entry. */
    fun applyLive(transform: (NoteContent) -> NoteContent) {
        content.value = transform(content.value)
        scheduleSave()
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(content.value)
        content.value = prev
        updateUndoFlags()
        scheduleSave()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(content.value)
        content.value = next
        updateUndoFlags()
        scheduleSave()
    }

    private fun updateUndoFlags() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    // ---- Ink ----

    fun currentStrokeTemplate(): InkStroke =
        if (tool.value == EditorTool.HIGHLIGHTER) {
            InkStroke(
                type = com.stefanoneve.ultimatenotes.data.model.StrokeType.HIGHLIGHTER,
                color = highlighterColor.value,
                width = highlighterWidth.value,
            )
        } else {
            InkStroke(color = penColor.value, width = penWidth.value)
        }

    fun addStroke(stroke: InkStroke) {
        if (stroke.points.isEmpty()) return
        commit { it.copy(strokes = it.strokes + stroke) }
    }

    fun assetFile(fileName: String): java.io.File? =
        note.value?.let { repo.assetFile(it.id, fileName) }

    /** Removes strokes passing within [radius] canvas units of (x, y). */
    fun eraseAt(x: Float, y: Float, radius: Float) {
        val hit = content.value.strokes.filter { stroke ->
            stroke.points.any { hypot(it.x - x, it.y - y) <= radius + stroke.width }
        }
        if (hit.isNotEmpty()) {
            commit { c -> c.copy(strokes = c.strokes - hit.toSet()) }
        }
    }

    // ---- Elements ----

    fun addTextElement(x: Float, y: Float): TextElement {
        val settings = settingsStore.settings.value
        val element = TextElement(
            x = x,
            y = y,
            fontId = settings.defaultFontId,
            color = penColor.value,
        )
        commit { it.copy(elements = it.elements + element) }
        selectedElementId.value = element.id
        editingTextId.value = element.id
        return element
    }

    fun updateElement(id: String, live: Boolean = false, transform: (NoteElement) -> NoteElement) {
        val apply: ((NoteContent) -> NoteContent) -> Unit =
            if (live) ::applyLive else { t -> commit(t) }
        apply { c ->
            c.copy(elements = c.elements.map { if (it.id == id) transform(it) else it })
        }
    }

    /** Records an undo snapshot before a live drag/resize sequence starts. */
    fun beginGesture() {
        undoStack.addLast(content.value)
        if (undoStack.size > 64) undoStack.removeFirst()
        redoStack.clear()
        updateUndoFlags()
    }

    // ---- Connectors ----

    /** Handles a tap on an element while the CONNECT tool is active. */
    fun handleConnectTap(elementId: String) {
        val from = pendingConnectFrom.value
        when {
            from == null -> pendingConnectFrom.value = elementId
            from == elementId -> pendingConnectFrom.value = null
            else -> {
                val connector = ConnectorElement(fromId = from, toId = elementId)
                commit { it.copy(connectors = it.connectors + connector) }
                pendingConnectFrom.value = null
                selectedConnectorId.value = connector.id
            }
        }
    }

    fun updateConnector(
        id: String,
        live: Boolean = false,
        transform: (ConnectorElement) -> ConnectorElement,
    ) {
        val apply: ((NoteContent) -> NoteContent) -> Unit =
            if (live) ::applyLive else { t -> commit(t) }
        apply { c ->
            c.copy(connectors = c.connectors.map { if (it.id == id) transform(it) else it })
        }
    }

    fun deleteConnector(id: String) {
        commit { c -> c.copy(connectors = c.connectors.filterNot { it.id == id }) }
        if (selectedConnectorId.value == id) selectedConnectorId.value = null
    }

    fun deleteElement(id: String) {
        val element = content.value.elements.firstOrNull { it.id == id }
        commit { c ->
            c.copy(
                elements = c.elements.filterNot { it.id == id },
                connectors = c.connectors.filterNot { it.fromId == id || it.toId == id },
            )
        }
        elementSizes.remove(id)
        if (element is ImageElement && element.fileName.isNotBlank()) {
            note.value?.let { repo.assetFile(it.id, element.fileName).delete() }
        }
        if (selectedElementId.value == id) selectedElementId.value = null
        if (editingTextId.value == id) editingTextId.value = null
    }

    fun setBackground(bg: com.stefanoneve.ultimatenotes.data.model.CanvasBackground) {
        commit { it.copy(background = bg) }
    }

    // ---- Media import ----

    /** Copies an image (gallery pick, keyboard sticker, …) into the note. */
    fun importImage(uri: Uri, atX: Float, atY: Float) {
        val noteId = note.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val resolver = app.contentResolver
                val ext = when (resolver.getType(uri)) {
                    "image/png" -> "png"
                    "image/gif" -> "gif"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val fileName = "img_${UUID.randomUUID()}.$ext"
                val dest = repo.assetFile(noteId, fileName)
                resolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                } ?: error("stream nullo")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(dest.absolutePath, bounds)
                val w = bounds.outWidth.coerceAtLeast(1).toFloat()
                val h = bounds.outHeight.coerceAtLeast(1).toFloat()
                val displayW = w.coerceAtMost(500f)
                ImageElement(
                    x = atX,
                    y = atY,
                    width = displayW,
                    height = displayW * h / w,
                    fileName = fileName,
                )
            }.onSuccess { element ->
                withContext(Dispatchers.Main) {
                    commit { it.copy(elements = it.elements + element) }
                }
            }
        }
    }

    /**
     * Renders every page of a PDF into the note as annotatable page images,
     * stacked vertically starting at (atX, atY).
     */
    fun importPdf(uri: Uri, atX: Float, atY: Float) {
        val noteId = note.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val pfd = app.contentResolver.openFileDescriptor(uri, "r")
                    ?: error("PDF non leggibile")
                val elements = mutableListOf<ImageElement>()
                pfd.use {
                    PdfRenderer(it).use { renderer ->
                        var top = atY
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { page ->
                                val scale = 2f
                                val bmp = Bitmap.createBitmap(
                                    (page.width * scale).toInt(),
                                    (page.height * scale).toInt(),
                                    Bitmap.Config.ARGB_8888,
                                )
                                bmp.eraseColor(android.graphics.Color.WHITE)
                                page.render(
                                    bmp, null, null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                                )
                                val fileName = "pdf_${UUID.randomUUID()}_p$i.png"
                                repo.assetFile(noteId, fileName).outputStream().use { out ->
                                    bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                                }
                                bmp.recycle()
                                val pageW = 800f
                                val pageH = pageW * page.height / page.width
                                elements += ImageElement(
                                    x = atX,
                                    y = top,
                                    width = pageW,
                                    height = pageH,
                                    fileName = fileName,
                                    isPdfPage = true,
                                    pdfPage = i + 1,
                                )
                                top += pageH + 24f
                            }
                        }
                    }
                }
                elements.toList()
            }.onSuccess { pages ->
                withContext(Dispatchers.Main) {
                    commit { it.copy(elements = it.elements + pages) }
                }
            }
        }
    }

    // ---- Persistence ----

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            persist()
        }
    }

    suspend fun persist() {
        val entity = note.value ?: return
        val derivedTitle = title.value.ifBlank {
            content.value.plainText().lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim('#', ' ', '-', '*', '>')
                ?.take(60)
                .orEmpty()
        }
        repo.saveNote(entity.copy(title = derivedTitle), content.value)
    }

    fun saveNow() {
        viewModelScope.launch { persist() }
    }

    override fun onCleared() {
        // Best-effort flush; viewModelScope is gone, so write synchronously.
        val entity = note.value ?: return
        kotlinx.coroutines.runBlocking {
            repo.saveNote(entity.copy(title = title.value), content.value)
        }
    }
}
