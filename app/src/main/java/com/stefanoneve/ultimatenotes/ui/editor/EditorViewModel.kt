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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.hypot

enum class EditorTool { SELECT, PEN, HIGHLIGHTER, ERASER, TEXT, CONNECT, LASSO, FRAME }

/** Result of a lasso gesture: elements and strokes captured by the loop. */
data class LassoSelection(
    val elementIds: Set<String> = emptySet(),
    val strokeIds: Set<String> = emptySet(),
) {
    val isEmpty: Boolean get() = elementIds.isEmpty() && strokeIds.isEmpty()
}

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

    /** Id of the frame currently selected (style bar + handles shown). */
    val selectedFrameId = MutableStateFlow<String?>(null)

    /** Current lasso multi-selection. */
    val lassoSelection = MutableStateFlow(LassoSelection())

    /** All notes, for the note-link picker. */
    val allNotes = repo.observeNotes()
        .stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            emptyList<NoteEntity>(),
        )

    private val notePreviewCache = mutableMapOf<String, NoteEntity?>()

    suspend fun notePreview(noteId: String): NoteEntity? =
        notePreviewCache.getOrPut(noteId) { repo.getNote(noteId) }

    fun clearSelections() {
        selectedElementId.value = null
        selectedConnectorId.value = null
        selectedFrameId.value = null
        lassoSelection.value = LassoSelection()
    }

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
        // color/fontId = null → the text follows the active theme.
        val element = TextElement(x = x, y = y)
        commit { it.copy(elements = it.elements + element) }
        selectedElementId.value = element.id
        editingTextId.value = element.id
        return element
    }

    fun addNoteLink(targetNoteId: String, x: Float, y: Float) {
        val element = com.stefanoneve.ultimatenotes.data.model.NoteLinkElement(
            x = x, y = y, targetNoteId = targetNoteId,
        )
        commit { it.copy(elements = it.elements + element) }
        selectedElementId.value = element.id
    }

    /** Adds a web link card and fetches the page title in the background. */
    fun addWebLink(url: String, x: Float, y: Float) {
        val normalized =
            if (url.startsWith("http://") || url.startsWith("https://")) url
            else "https://$url"
        val element = com.stefanoneve.ultimatenotes.data.model.WebLinkElement(
            x = x, y = y, url = normalized,
        )
        commit { it.copy(elements = it.elements + element) }
        selectedElementId.value = element.id
        viewModelScope.launch(Dispatchers.IO) {
            val title = runCatching {
                val conn = java.net.URL(normalized).openConnection()
                    as java.net.HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) UltimateNotes")
                val head = conn.inputStream.bufferedReader()
                    .use { r -> CharArray(65536).let { buf -> String(buf, 0, maxOf(r.read(buf), 0)) } }
                conn.disconnect()
                Regex("""<title[^>]*>(.*?)</title>""", RegexOption.DOT_MATCHES_ALL)
                    .find(head)?.groupValues?.get(1)?.trim()
                    ?.replace(Regex("\\s+"), " ")
                    ?.take(120)
            }.getOrNull()
            if (!title.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    updateElement(element.id, live = true) {
                        (it as com.stefanoneve.ultimatenotes.data.model.WebLinkElement)
                            .copy(title = title)
                    }
                }
            }
        }
    }

    /** Copies any file into the note and embeds it as an openable card. */
    fun importFile(uri: Uri, atX: Float, atY: Float) {
        val noteId = note.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val resolver = app.contentResolver
                val doc = androidx.documentfile.provider.DocumentFile
                    .fromSingleUri(app, uri)
                val displayName = doc?.name ?: "file_${System.currentTimeMillis()}"
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val ext = displayName.substringAfterLast('.', "bin")
                val fileName = "file_${UUID.randomUUID()}.$ext"
                val dest = repo.assetFile(noteId, fileName)
                resolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                } ?: error("File non leggibile")
                com.stefanoneve.ultimatenotes.data.model.FileElement(
                    x = atX, y = atY,
                    fileName = fileName,
                    displayName = displayName,
                    mimeType = mime,
                    sizeBytes = dest.length(),
                )
            }.onSuccess { element ->
                withContext(Dispatchers.Main) {
                    commit { it.copy(elements = it.elements + element) }
                }
            }
        }
    }

    /** Duplicates an element slightly offset from the original. */
    fun duplicateElement(id: String) {
        val element = content.value.elements.firstOrNull { it.id == id } ?: return
        val copyId = UUID.randomUUID().toString()
        val copy: NoteElement = when (element) {
            is TextElement ->
                element.copy(id = copyId, x = element.x + 36f, y = element.y + 36f, groupId = null)
            is ImageElement ->
                element.copy(id = copyId, x = element.x + 36f, y = element.y + 36f, groupId = null)
            is com.stefanoneve.ultimatenotes.data.model.NoteLinkElement ->
                element.copy(id = copyId, x = element.x + 36f, y = element.y + 36f, groupId = null)
            is com.stefanoneve.ultimatenotes.data.model.WebLinkElement ->
                element.copy(id = copyId, x = element.x + 36f, y = element.y + 36f, groupId = null)
            is com.stefanoneve.ultimatenotes.data.model.FileElement ->
                element.copy(id = copyId, x = element.x + 36f, y = element.y + 36f, groupId = null)
        }
        commit { it.copy(elements = it.elements + copy) }
        selectedElementId.value = copy.id
    }

    /** Render order follows list order: last = on top. */
    fun bringToFront(id: String) {
        commit { c ->
            val e = c.elements.firstOrNull { it.id == id } ?: return@commit c
            c.copy(elements = c.elements.filterNot { it.id == id } + e)
        }
    }

    fun sendToBack(id: String) {
        commit { c ->
            val e = c.elements.firstOrNull { it.id == id } ?: return@commit c
            c.copy(elements = listOf(e) + c.elements.filterNot { it.id == id })
        }
    }

    /** Moves an element; if it belongs to a group, the whole group follows. */
    fun moveElementBy(id: String, dx: Float, dy: Float) {
        val element = content.value.elements.firstOrNull { it.id == id } ?: return
        val group = element.groupId
        applyLive { c ->
            c.copy(
                elements = c.elements.map { e ->
                    if (e.id == id || (group != null && e.groupId == group)) {
                        moveElement(e, dx, dy)
                    } else e
                },
            )
        }
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

    // ---- Lasso ----

    /** Computes which elements/strokes fall inside the lasso polygon. */
    fun applyLasso(polygon: List<Pair<Float, Float>>) {
        if (polygon.size < 3) return
        val c = content.value
        val elements = c.elements.filter { e ->
            val size = elementSizes[e.id]
            val cx = e.x + (size?.width ?: 100f) / 2f
            val cy = e.y + (size?.height ?: 60f) / 2f
            pointInPolygon(cx, cy, polygon)
        }.map { it.id }.toSet()
        val strokes = c.strokes.filter { s ->
            s.points.isNotEmpty() &&
                s.points.count { pointInPolygon(it.x, it.y, polygon) } > s.points.size / 2
        }.map { it.id }.toSet()
        lassoSelection.value = LassoSelection(elements, strokes)
        if (elements.isNotEmpty() || strokes.isNotEmpty()) {
            tool.value = EditorTool.SELECT
        }
    }

    fun moveLassoBy(dx: Float, dy: Float) {
        val sel = lassoSelection.value
        if (sel.isEmpty) return
        applyLive { c ->
            c.copy(
                elements = c.elements.map { e ->
                    if (e.id in sel.elementIds) moveElement(e, dx, dy) else e
                },
                strokes = c.strokes.map { s ->
                    if (s.id in sel.strokeIds) {
                        s.copy(points = s.points.map { p -> p.copy(x = p.x + dx, y = p.y + dy) })
                    } else s
                },
            )
        }
    }

    fun deleteLassoSelection() {
        val sel = lassoSelection.value
        if (sel.isEmpty) return
        commit { c ->
            c.copy(
                elements = c.elements.filterNot { it.id in sel.elementIds },
                strokes = c.strokes.filterNot { it.id in sel.strokeIds },
                connectors = c.connectors.filterNot {
                    it.fromId in sel.elementIds || it.toId in sel.elementIds
                },
            )
        }
        lassoSelection.value = LassoSelection()
    }

    /** Assigns a shared groupId to the lasso-selected elements. */
    fun groupLassoSelection() {
        val ids = lassoSelection.value.elementIds
        if (ids.size < 2) return
        val groupId = UUID.randomUUID().toString()
        commit { c ->
            c.copy(
                elements = c.elements.map { e ->
                    if (e.id in ids) withGroup(e, groupId) else e
                },
            )
        }
    }

    fun ungroupLassoSelection() {
        val ids = lassoSelection.value.elementIds
        if (ids.isEmpty()) return
        commit { c ->
            c.copy(
                elements = c.elements.map { e ->
                    if (e.id in ids) withGroup(e, null) else e
                },
            )
        }
    }

    /** True when any lasso-selected element belongs to a group. */
    fun lassoHasGroup(): Boolean =
        content.value.elements.any {
            it.id in lassoSelection.value.elementIds && it.groupId != null
        }

    // ---- Frames ----

    fun addFrame(x: Float, y: Float, width: Float, height: Float) {
        val frame = com.stefanoneve.ultimatenotes.data.model.FrameElement(
            x = x, y = y,
            width = width.coerceAtLeast(120f),
            height = height.coerceAtLeast(120f),
        )
        commit { it.copy(frames = it.frames + frame) }
        selectedFrameId.value = frame.id
        tool.value = EditorTool.SELECT
    }

    fun updateFrame(
        id: String,
        live: Boolean = false,
        transform: (com.stefanoneve.ultimatenotes.data.model.FrameElement) ->
        com.stefanoneve.ultimatenotes.data.model.FrameElement,
    ) {
        val apply: ((NoteContent) -> NoteContent) -> Unit =
            if (live) ::applyLive else { t -> commit(t) }
        apply { c ->
            c.copy(frames = c.frames.map { if (it.id == id) transform(it) else it })
        }
    }

    fun deleteFrame(id: String) {
        commit { c -> c.copy(frames = c.frames.filterNot { it.id == id }) }
        if (selectedFrameId.value == id) selectedFrameId.value = null
    }

    /** Moves a frame together with everything currently inside it. */
    fun moveFrameBy(id: String, dx: Float, dy: Float) {
        val frame = content.value.frames.firstOrNull { it.id == id } ?: return
        val inside = content.value.elements.filter { e ->
            val size = elementSizes[e.id]
            val cx = e.x + (size?.width ?: 100f) / 2f
            val cy = e.y + (size?.height ?: 60f) / 2f
            cx in frame.x..(frame.x + frame.width) && cy in frame.y..(frame.y + frame.height)
        }.map { it.id }.toSet()
        val insideStrokes = content.value.strokes.filter { s ->
            s.points.isNotEmpty() && s.points.first().let { p ->
                p.x in frame.x..(frame.x + frame.width) &&
                    p.y in frame.y..(frame.y + frame.height)
            }
        }.map { it.id }.toSet()
        applyLive { c ->
            c.copy(
                frames = c.frames.map {
                    if (it.id == id) it.copy(x = it.x + dx, y = it.y + dy) else it
                },
                elements = c.elements.map { e ->
                    if (e.id in inside) moveElement(e, dx, dy) else e
                },
                strokes = c.strokes.map { s ->
                    if (s.id in insideStrokes) {
                        s.copy(points = s.points.map { p -> p.copy(x = p.x + dx, y = p.y + dy) })
                    } else s
                },
            )
        }
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

    /**
     * Copies an image (gallery pick, keyboard sticker, clipboard paste, …)
     * into the note. Bitmaps are re-encoded as WebP (lossless when they have
     * transparency) and downsampled to save space; GIFs are kept as-is.
     */
    fun importImage(uri: Uri, atX: Float, atY: Float) {
        val noteId = note.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val resolver = app.contentResolver
                val mime = resolver.getType(uri).orEmpty()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                val sample = generateSequence(1) { it * 2 }.first {
                    bounds.outWidth / it <= 2048 && bounds.outHeight / it <= 2048
                }
                val bitmap = if (mime == "image/gif") null else {
                    resolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(
                            it, null,
                            BitmapFactory.Options().apply { inSampleSize = sample },
                        )
                    }
                }
                val (fileName, w, h) = if (bitmap != null) {
                    val name = "img_${UUID.randomUUID()}.webp"
                    repo.assetFile(noteId, name).outputStream().use { out ->
                        val format =
                            if (android.os.Build.VERSION.SDK_INT >= 30) {
                                if (bitmap.hasAlpha()) {
                                    Bitmap.CompressFormat.WEBP_LOSSLESS
                                } else Bitmap.CompressFormat.WEBP_LOSSY
                            } else {
                                @Suppress("DEPRECATION")
                                Bitmap.CompressFormat.WEBP
                            }
                        bitmap.compress(format, 90, out)
                    }
                    Triple(name, bitmap.width.toFloat(), bitmap.height.toFloat())
                        .also { bitmap.recycle() }
                } else {
                    val ext = when (mime) {
                        "image/png" -> "png"
                        "image/gif" -> "gif"
                        "image/webp" -> "webp"
                        else -> "jpg"
                    }
                    val name = "img_${UUID.randomUUID()}.$ext"
                    resolver.openInputStream(uri)?.use { input ->
                        repo.assetFile(noteId, name).outputStream()
                            .use { input.copyTo(it) }
                    } ?: error("Immagine non leggibile")
                    Triple(
                        name,
                        bounds.outWidth.coerceAtLeast(1).toFloat(),
                        bounds.outHeight.coerceAtLeast(1).toFloat(),
                    )
                }
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

    /** Imports any image found in the system clipboard. */
    fun pasteImage(atX: Float, atY: Float): Boolean {
        val cm = app.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        val clip = cm.primaryClip ?: return false
        var count = 0
        for (i in 0 until clip.itemCount) {
            clip.getItemAt(i).uri?.let { uri ->
                importImage(uri, atX + count * 40f, atY + count * 40f)
                count++
            }
        }
        return count > 0
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

private fun moveElement(e: NoteElement, dx: Float, dy: Float): NoteElement = when (e) {
    is TextElement -> e.copy(x = e.x + dx, y = e.y + dy)
    is ImageElement -> e.copy(x = e.x + dx, y = e.y + dy)
    is com.stefanoneve.ultimatenotes.data.model.NoteLinkElement ->
        e.copy(x = e.x + dx, y = e.y + dy)
    is com.stefanoneve.ultimatenotes.data.model.WebLinkElement ->
        e.copy(x = e.x + dx, y = e.y + dy)
    is com.stefanoneve.ultimatenotes.data.model.FileElement ->
        e.copy(x = e.x + dx, y = e.y + dy)
}

private fun withGroup(e: NoteElement, groupId: String?): NoteElement = when (e) {
    is TextElement -> e.copy(groupId = groupId)
    is ImageElement -> e.copy(groupId = groupId)
    is com.stefanoneve.ultimatenotes.data.model.NoteLinkElement ->
        e.copy(groupId = groupId)
    is com.stefanoneve.ultimatenotes.data.model.WebLinkElement ->
        e.copy(groupId = groupId)
    is com.stefanoneve.ultimatenotes.data.model.FileElement ->
        e.copy(groupId = groupId)
}

/** Ray-casting point-in-polygon test. */
private fun pointInPolygon(x: Float, y: Float, polygon: List<Pair<Float, Float>>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val (xi, yi) = polygon[i]
        val (xj, yj) = polygon[j]
        if ((yi > y) != (yj > y) &&
            x < (xj - xi) * (y - yi) / (yj - yi + 1e-7f) + xi
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}
