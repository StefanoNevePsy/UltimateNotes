package com.stefanoneve.ultimatenotes.ui.editor

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Bold
import com.composables.icons.lucide.BringToFront
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.File
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.MoveHorizontal
import com.composables.icons.lucide.PaintBucket
import com.composables.icons.lucide.Paperclip
import com.composables.icons.lucide.SendToBack
import com.composables.icons.lucide.Slash
import com.composables.icons.lucide.StickyNote
import com.composables.icons.lucide.FileDown
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Code
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Frame
import com.composables.icons.lucide.Group
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Highlighter
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Italic
import com.composables.icons.lucide.Lasso
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.List
import com.composables.icons.lucide.ListChecks
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.MoveDiagonal
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Pipette
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.Spline
import com.composables.icons.lucide.Strikethrough
import com.composables.icons.lucide.TextQuote
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.Undo2
import com.composables.icons.lucide.Ungroup
import com.composables.icons.lucide.X
import com.composables.icons.lucide.Zap
import com.stefanoneve.ultimatenotes.data.db.NoteEntity
import com.stefanoneve.ultimatenotes.data.fonts.FontManager
import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.CapStyle
import com.stefanoneve.ultimatenotes.data.model.ConnectorElement
import com.stefanoneve.ultimatenotes.data.model.FrameElement
import com.stefanoneve.ultimatenotes.data.model.FrameShape
import com.stefanoneve.ultimatenotes.data.model.ImageElement
import com.stefanoneve.ultimatenotes.data.model.InkStroke
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import com.stefanoneve.ultimatenotes.data.model.FileElement
import com.stefanoneve.ultimatenotes.data.model.NoteContent
import com.stefanoneve.ultimatenotes.data.model.NoteElement
import com.stefanoneve.ultimatenotes.data.model.NoteLinkElement
import com.stefanoneve.ultimatenotes.data.model.StrokePoint
import com.stefanoneve.ultimatenotes.data.model.TapeElement
import com.stefanoneve.ultimatenotes.data.model.TapePattern
import com.stefanoneve.ultimatenotes.data.model.TextElement
import com.stefanoneve.ultimatenotes.data.model.WebLinkElement
import com.stefanoneve.ultimatenotes.ui.components.ThemePickerDialog
import com.stefanoneve.ultimatenotes.ui.components.glass
import com.stefanoneve.ultimatenotes.ui.theme.LocalAppStyle
import com.stefanoneve.ultimatenotes.util.SPenEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    noteId: String,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val content by viewModel.content.collectAsState()
    val title by viewModel.title.collectAsState()
    val tool by viewModel.tool.collectAsState()
    val penColor by viewModel.penColor.collectAsState()
    val penWidth by viewModel.penWidth.collectAsState()
    val highlighterColor by viewModel.highlighterColor.collectAsState()
    val highlighterWidth by viewModel.highlighterWidth.collectAsState()
    val selectedElementId by viewModel.selectedElementId.collectAsState()
    val selectedConnectorId by viewModel.selectedConnectorId.collectAsState()
    val selectedFrameId by viewModel.selectedFrameId.collectAsState()
    val selectedTapeId by viewModel.selectedTapeId.collectAsState()
    val lassoSelection by viewModel.lassoSelection.collectAsState()
    val pendingConnectFrom by viewModel.pendingConnectFrom.collectAsState()
    val editingTextId by viewModel.editingTextId.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val settings by viewModel.settingsStore.settings.collectAsState()

    val canvasState = remember { CanvasState() }
    val activeStroke = remember { mutableStateOf<InkStroke?>(null) }
    val lassoPoints = remember { mutableStateOf<List<Offset>>(emptyList()) }
    val framePreview = remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    val tapePreview = remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var radialCenter by remember { mutableStateOf<Offset?>(null) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNotePicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }

    val editController = remember { MarkdownEditController() }

    // Marching dashes for animated connectors and frames.
    val anyAnimated = content.connectors.any { it.animated } ||
        content.frames.any { it.animated }
    var dashPhase = 0f
    if (anyAnimated) {
        val transition = rememberInfiniteTransition(label = "dash")
        // 0..1 = exactly one dash cycle, so the loop restart is invisible.
        dashPhase = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(900, easing = LinearEasing),
                RepeatMode.Restart,
            ),
            label = "dashPhase",
        ).value
    }

    val editingElement = content.elements
        .filterIsInstance<TextElement>()
        .firstOrNull { it.id == editingTextId }

    fun stopEditingText() {
        editingTextId?.let { id ->
            val element = viewModel.content.value.elements
                .filterIsInstance<TextElement>().firstOrNull { it.id == id }
            if (element != null && element.text.isBlank()) viewModel.deleteElement(id)
        }
        viewModel.editingTextId.value = null
    }

    LaunchedEffect(Unit) {
        SPenEvents.buttonPresses.collect { press ->
            radialCenter =
                if (radialCenter == null) Offset(press.x, press.y) - rootOrigin
                else null
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            val at = canvasState.toWorld(Offset(300f, 500f))
            viewModel.importImage(it, at.x, at.y)
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val at = canvasState.toWorld(Offset(100f, 300f))
            viewModel.importPdf(it, at.x, at.y)
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val at = canvasState.toWorld(Offset(350f, 550f))
            viewModel.importFile(it, at.x, at.y)
        }
    }
    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> uri?.let(viewModel::exportPdf) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned { rootOrigin = it.positionInWindow() },
    ) {
        // ---- Infinite canvas ----
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            InkLayer(
                strokes = content.strokes,
                activeStroke = activeStroke.value,
                background = content.background,
                canvasState = canvasState,
                patternColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .canvasGestures(
                        canvasState = canvasState,
                        toolProvider = { viewModel.tool.value },
                        stylusOnlyProvider = { settings.stylusOnlyDrawing },
                        strokeTemplate = viewModel::currentStrokeTemplate,
                        onStrokeFinished = viewModel::addStroke,
                        onErase = { world ->
                            viewModel.eraseAt(world.x, world.y, 18f / canvasState.scale)
                        },
                        onTap = { position, _ ->
                            val world = canvasState.toWorld(position)
                            when {
                                editingTextId != null -> stopEditingText()
                                viewModel.tool.value == EditorTool.TEXT ->
                                    viewModel.addTextElement(world.x, world.y)
                                viewModel.tool.value == EditorTool.CONNECT ->
                                    viewModel.pendingConnectFrom.value = null
                                else -> {
                                    val c = viewModel.content.value
                                    val frame = c.frames.lastOrNull {
                                        hitTestFrameBorder(
                                            it, world.x, world.y,
                                            24f / canvasState.scale,
                                        )
                                    }
                                    val tape =
                                        if (frame == null) {
                                            hitTestTape(
                                                c.tapes,
                                                world.x,
                                                world.y,
                                                tolerance = 12f / canvasState.scale,
                                            )
                                        } else null
                                    val connector =
                                        if (frame == null && tape == null) {
                                            hitTestConnector(
                                                c,
                                                viewModel.elementSizes,
                                                world,
                                                tolerance = 28f / canvasState.scale,
                                            )
                                        } else null
                                    viewModel.clearSelections()
                                    viewModel.selectedFrameId.value = frame?.id
                                    viewModel.selectedTapeId.value = tape
                                    viewModel.selectedConnectorId.value = connector
                                }
                            }
                        },
                        onLassoFinished = { points ->
                            viewModel.applyLasso(points.map { it.x to it.y })
                        },
                        onFrameFinished = { start, end ->
                            viewModel.addFrame(
                                x = minOf(start.x, end.x),
                                y = minOf(start.y, end.y),
                                width = kotlin.math.abs(end.x - start.x),
                                height = kotlin.math.abs(end.y - start.y),
                            )
                        },
                        onTapeFinished = { start, end ->
                            viewModel.addTape(start.x, start.y, end.x, end.y)
                        },
                        activeStroke = activeStroke,
                        lassoPoints = lassoPoints,
                        framePreview = framePreview,
                        tapePreview = tapePreview,
                    ),
            )

            FrameLayer(
                frames = content.frames,
                canvasState = canvasState,
                selectedFrameId = selectedFrameId,
                dashPhase = dashPhase,
                modifier = Modifier.fillMaxSize(),
            )

            ConnectorLayer(
                content = content,
                canvasState = canvasState,
                selectedConnectorId = selectedConnectorId,
                elementSizes = viewModel.elementSizes,
                dashPhase = dashPhase,
                modifier = Modifier.fillMaxSize(),
            )

            // Frame labels.
            content.frames.forEach { frame ->
                if (frame.label.isNotBlank()) {
                    key("label_${frame.id}") {
                        Text(
                            frame.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(frame.color),
                            modifier = Modifier.graphicsLayer {
                                translationX =
                                    frame.x * canvasState.scale + canvasState.offset.x
                                translationY =
                                    (frame.y - 34f) * canvasState.scale + canvasState.offset.y
                                scaleX = canvasState.scale
                                scaleY = canvasState.scale
                                transformOrigin = TransformOrigin(0f, 0f)
                            },
                        )
                    }
                }
            }

            val interactive =
                tool == EditorTool.SELECT || tool == EditorTool.TEXT ||
                    tool == EditorTool.CONNECT
            content.elements.forEach { element ->
                key(element.id) {
                    ElementView(
                        element = element,
                        canvasState = canvasState,
                        interactive = interactive,
                        selected = selectedElementId == element.id,
                        inLasso = element.id in lassoSelection.elementIds,
                        pendingConnect = pendingConnectFrom == element.id,
                        editing = editingTextId == element.id,
                        editController = editController,
                        viewModel = viewModel,
                        onOpenNote = onOpenNote,
                        onReceiveImage = { uri ->
                            viewModel.importImage(uri, element.x, element.y + 80f)
                        },
                    )
                }
            }

            // Washi tape sits above every element, like real tape.
            TapeLayer(
                tapes = content.tapes,
                canvasState = canvasState,
                selectedTapeId = selectedTapeId,
                modifier = Modifier.fillMaxSize(),
            )
            tapePreview.value?.let { (start, end) ->
                Canvas(Modifier.fillMaxSize()) {
                    withTransform({
                        translate(canvasState.offset.x, canvasState.offset.y)
                        scale(canvasState.scale, canvasState.scale, pivot = Offset.Zero)
                    }) {
                        drawTape(
                            com.stefanoneve.ultimatenotes.data.model.TapeElement(
                                x1 = start.x, y1 = start.y, x2 = end.x, y2 = end.y,
                                thickness = viewModel.tapeThickness.value,
                                color = viewModel.currentTheme().resolvedTapeColors().first(),
                                pattern = viewModel.currentTheme().tapePattern,
                                alpha = 0.6f,
                            ),
                            selected = false,
                        )
                    }
                }
            }

            // Lasso loop while drawing + frame creation preview.
            LassoOverlay(
                points = lassoPoints.value,
                canvasState = canvasState,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(),
            )
            framePreview.value?.let { (start, end) ->
                Canvas(Modifier.fillMaxSize()) {
                    val topLeft = Offset(
                        minOf(start.x, end.x) * canvasState.scale + canvasState.offset.x,
                        minOf(start.y, end.y) * canvasState.scale + canvasState.offset.y,
                    )
                    val size = Size(
                        kotlin.math.abs(end.x - start.x) * canvasState.scale,
                        kotlin.math.abs(end.y - start.y) * canvasState.scale,
                    )
                    drawRect(
                        Color(0xFF9A8FE5),
                        topLeft = topLeft,
                        size = size,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                        ),
                    )
                }
            }

            // Lasso selection chrome: bounds + drag to move.
            if (!lassoSelection.isEmpty) {
                lassoBounds(lassoSelection, content, viewModel.elementSizes)?.let { bounds ->
                    LassoSelectionBox(
                        bounds = bounds,
                        canvasState = canvasState,
                        viewModel = viewModel,
                    )
                }
            }

            // Selected connector: bezier handle.
            content.connectors.firstOrNull { it.id == selectedConnectorId }?.let {
                ConnectorHandle(
                    connector = it,
                    content = content,
                    canvasState = canvasState,
                    viewModel = viewModel,
                )
            }

            // Selected frame: move/resize/delete handles.
            content.frames.firstOrNull { it.id == selectedFrameId }?.let {
                FrameHandles(frame = it, canvasState = canvasState, viewModel = viewModel)
            }

            // Selected tape: endpoint + move handles.
            content.tapes.firstOrNull { it.id == selectedTapeId }?.let {
                TapeHandles(tape = it, canvasState = canvasState, viewModel = viewModel)
            }
        }

        // ---- Floating glass top bar ----
        EditorTopBar(
            title = title,
            onTitleChange = viewModel::setTitle,
            canUndo = canUndo,
            canRedo = canRedo,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onBack = {
                viewModel.saveNow()
                onBack()
            },
            onAddImage = { imageLauncher.launch("image/*") },
            onAddPdf = { pdfLauncher.launch(arrayOf("application/pdf")) },
            onPaste = {
                val at = canvasState.toWorld(Offset(350f, 550f))
                viewModel.pasteImage(at.x, at.y)
            },
            onAddNoteLink = { showNotePicker = true },
            onAddWebLink = { showLinkDialog = true },
            onAttachFile = { fileLauncher.launch(arrayOf("*/*")) },
            onAddSticky = {
                val at = canvasState.toWorld(Offset(350f, 550f))
                viewModel.addStickyNote(at.x, at.y)
            },
            onExportPdf = {
                pdfExportLauncher.launch("UltimateNotes-export.pdf")
            },
            onPickTheme = { showThemeDialog = true },
            background = content.background,
            onBackgroundChange = viewModel::setBackground,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        // ---- Floating glass bottom bar ----
        val selectedConnector = content.connectors
            .firstOrNull { it.id == selectedConnectorId }
        val selectedFrame = content.frames.firstOrNull { it.id == selectedFrameId }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            when {
                editingTextId != null -> TextFormatBar(
                    controller = editController,
                    fontManager = viewModel.fontManager,
                    paletteColors = settings.activePalette().colors,
                    currentFontId = editingElement?.fontId,
                    onFontSelected = { fontId ->
                        if (editController.hasSelection()) {
                            editController.wrap("{f:$fontId}", "{/f}")
                        } else {
                            editingTextId?.let { id ->
                                viewModel.updateElement(id) {
                                    (it as TextElement).copy(fontId = fontId)
                                }
                            }
                        }
                    },
                    onColorSelected = { color ->
                        if (editController.hasSelection()) {
                            val hex = String.format("#%06X", color and 0xFFFFFF)
                            editController.wrap("{c:$hex}", "{/c}")
                        } else {
                            editingTextId?.let { id ->
                                viewModel.updateElement(id) {
                                    (it as TextElement).copy(color = color)
                                }
                            }
                        }
                    },
                    onColorAuto = {
                        editingTextId?.let { id ->
                            viewModel.updateElement(id) {
                                (it as TextElement).copy(color = null)
                            }
                        }
                    },
                    onDone = ::stopEditingText,
                )
                selectedElementId != null -> {
                    val selectedElement =
                        content.elements.firstOrNull { it.id == selectedElementId }
                    if (selectedElement != null) {
                        ElementActionBar(
                            element = selectedElement,
                            paletteColors = settings.activePalette().colors,
                            viewModel = viewModel,
                        )
                    }
                }
                !lassoSelection.isEmpty -> LassoActionBar(
                    selection = lassoSelection,
                    hasGroup = viewModel.lassoHasGroup(),
                    onGroup = viewModel::groupLassoSelection,
                    onUngroup = viewModel::ungroupLassoSelection,
                    onDelete = viewModel::deleteLassoSelection,
                    onClose = {
                        viewModel.lassoSelection.value = LassoSelection()
                    },
                )
                selectedConnector != null -> ConnectorStyleBar(
                    connector = selectedConnector,
                    paletteColors = settings.activePalette().colors,
                    onUpdate = { transform ->
                        viewModel.updateConnector(selectedConnector.id, transform = transform)
                    },
                    onDelete = { viewModel.deleteConnector(selectedConnector.id) },
                )
                content.tapes.any { it.id == selectedTapeId } -> {
                    val tape = content.tapes.first { it.id == selectedTapeId }
                    TapeStyleBar(
                        tape = tape,
                        tapeColors = viewModel.currentTheme().resolvedTapeColors() +
                            settings.activePalette().colors.take(4),
                        onUpdate = { transform ->
                            viewModel.updateTape(tape.id, transform = transform)
                        },
                        onDelete = { viewModel.deleteTape(tape.id) },
                    )
                }
                selectedFrame != null -> FrameStyleBar(
                    frame = selectedFrame,
                    paletteColors = settings.activePalette().colors,
                    onUpdate = { transform ->
                        viewModel.updateFrame(selectedFrame.id, transform = transform)
                    },
                    onDelete = { viewModel.deleteFrame(selectedFrame.id) },
                )
                else -> EditorToolBar(
                    tool = tool,
                    onToolSelected = {
                        viewModel.tool.value = it
                        if (it != EditorTool.CONNECT) {
                            viewModel.pendingConnectFrom.value = null
                        }
                    },
                    currentColor =
                    if (tool == EditorTool.HIGHLIGHTER) highlighterColor else penColor,
                    onOpenWheel = { center -> radialCenter = center - rootOrigin },
                )
            }
        }

        if (pendingConnectFrom != null) {
            Text(
                "Tocca un altro elemento per collegarlo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 86.dp)
                    .glass(corner = 20.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        radialCenter?.let { center ->
            RadialMenu(
                center = center,
                currentTool = tool,
                currentColor =
                if (tool == EditorTool.HIGHLIGHTER) highlighterColor else penColor,
                currentWidth =
                if (tool == EditorTool.HIGHLIGHTER) highlighterWidth else penWidth,
                paletteColors = settings.activePalette().colors,
                onToolSelected = { viewModel.tool.value = it },
                onColorSelected = { color ->
                    if (viewModel.tool.value == EditorTool.HIGHLIGHTER) {
                        viewModel.highlighterColor.value = 0xCC000000 or (color and 0xFFFFFF)
                    } else {
                        viewModel.penColor.value = color
                    }
                },
                onWidthSelected = { width ->
                    if (viewModel.tool.value == EditorTool.HIGHLIGHTER) {
                        viewModel.highlighterWidth.value = width
                    } else {
                        viewModel.penWidth.value = width
                    }
                },
                onDismiss = { radialCenter = null },
            )
        }
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            selectedThemeId = settings.themeId,
            onSelect = { id ->
                viewModel.settingsStore.update { it.copy(themeId = id) }
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showNotePicker) {
        val notes by viewModel.allNotes.collectAsState()
        NotePickerDialog(
            notes = notes.filter { it.id != noteId },
            onPick = { picked ->
                val at = canvasState.toWorld(Offset(350f, 550f))
                viewModel.addNoteLink(picked.id, at.x, at.y)
                showNotePicker = false
            },
            onDismiss = { showNotePicker = false },
        )
    }

    if (showLinkDialog) {
        WebLinkDialog(
            onConfirm = { url ->
                val at = canvasState.toWorld(Offset(350f, 550f))
                viewModel.addWebLink(url, at.x, at.y)
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false },
        )
    }
}

@Composable
private fun WebLinkDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi link web") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = { Text("https://…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank(),
                onClick = { onConfirm(url.trim()) },
            ) { Text("Aggiungi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

// ---- Gestures ----

private fun Offset.toWorldPoint(state: CanvasState, pressure: Float): StrokePoint {
    val world = state.toWorld(this)
    return StrokePoint(world.x, world.y, pressure.coerceIn(0.05f, 1.5f))
}

private fun Modifier.canvasGestures(
    canvasState: CanvasState,
    toolProvider: () -> EditorTool,
    stylusOnlyProvider: () -> Boolean,
    strokeTemplate: () -> InkStroke,
    onStrokeFinished: (InkStroke) -> Unit,
    onErase: (Offset) -> Unit,
    onTap: (Offset, PointerType) -> Unit,
    onLassoFinished: (List<Offset>) -> Unit,
    onFrameFinished: (Offset, Offset) -> Unit,
    onTapeFinished: (Offset, Offset) -> Unit,
    activeStroke: MutableState<InkStroke?>,
    lassoPoints: MutableState<List<Offset>>,
    framePreview: MutableState<Pair<Offset, Offset>?>,
    tapePreview: MutableState<Pair<Offset, Offset>?>,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val isEraserTip = down.type == PointerType.Eraser
        val isStylus = down.type == PointerType.Stylus || isEraserTip
        val tool = if (isEraserTip) EditorTool.ERASER else toolProvider()
        val drawingTool =
            tool == EditorTool.PEN || tool == EditorTool.HIGHLIGHTER || tool == EditorTool.ERASER
        // A down already consumed above (open text editor, handles…) must not
        // start a stroke underneath.
        val canDraw = drawingTool && !down.isConsumed &&
            (isStylus || !stylusOnlyProvider())

        when {
            canDraw && tool != EditorTool.ERASER -> {
                var stroke = strokeTemplate().copy(
                    points = listOf(down.position.toWorldPoint(canvasState, down.pressure)),
                )
                activeStroke.value = stroke
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.size > 1) {
                        activeStroke.value = null
                        transformLoop(canvasState)
                        return@awaitEachGesture
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.positionChanged()) {
                        stroke = stroke.copy(
                            points = stroke.points +
                                change.position.toWorldPoint(canvasState, change.pressure),
                        )
                        activeStroke.value = stroke
                        change.consume()
                    }
                    if (!change.pressed) break
                }
                activeStroke.value = null
                onStrokeFinished(stroke)
            }

            canDraw -> {
                onErase(canvasState.toWorld(down.position))
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.positionChanged()) {
                        onErase(canvasState.toWorld(change.position))
                        change.consume()
                    }
                    if (!change.pressed) break
                }
            }

            tool == EditorTool.LASSO -> {
                var points = listOf(canvasState.toWorld(down.position))
                lassoPoints.value = points
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.size > 1) {
                        lassoPoints.value = emptyList()
                        transformLoop(canvasState)
                        return@awaitEachGesture
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.positionChanged()) {
                        points = points + canvasState.toWorld(change.position)
                        lassoPoints.value = points
                        change.consume()
                    }
                    if (!change.pressed) break
                }
                lassoPoints.value = emptyList()
                onLassoFinished(points)
            }

            tool == EditorTool.FRAME -> {
                val start = canvasState.toWorld(down.position)
                var end = start
                framePreview.value = start to end
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.size > 1) {
                        framePreview.value = null
                        transformLoop(canvasState)
                        return@awaitEachGesture
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.positionChanged()) {
                        end = canvasState.toWorld(change.position)
                        framePreview.value = start to end
                        change.consume()
                    }
                    if (!change.pressed) break
                }
                framePreview.value = null
                if (kotlin.math.abs(end.x - start.x) > 40f &&
                    kotlin.math.abs(end.y - start.y) > 40f
                ) {
                    onFrameFinished(start, end)
                }
            }

            tool == EditorTool.TAPE -> {
                val start = canvasState.toWorld(down.position)
                var end = start
                tapePreview.value = start to end
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.size > 1) {
                        tapePreview.value = null
                        transformLoop(canvasState)
                        return@awaitEachGesture
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.positionChanged()) {
                        end = snapTapeAngle(start, canvasState.toWorld(change.position))
                        tapePreview.value = start to end
                        change.consume()
                    }
                    if (!change.pressed) break
                }
                tapePreview.value = null
                if (kotlin.math.hypot(
                        (end.x - start.x).toDouble(),
                        (end.y - start.y).toDouble(),
                    ) > 40.0
                ) {
                    onTapeFinished(start, end)
                }
            }

            else -> {
                var moved = false
                var totalPan = Offset.Zero
                var sawMultiTouch = false
                // Touches already handled by an element above (text editor,
                // buttons, drag handles…) must not count as canvas taps.
                var consumedAbove = down.isConsumed
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.any { it.isConsumed }) consumedAbove = true
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break
                    if (pressed.size > 1) sawMultiTouch = true
                    if (consumedAbove) continue
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    totalPan += pan
                    if (!moved &&
                        (totalPan.getDistance() > viewConfiguration.touchSlop || zoom != 1f)
                    ) {
                        moved = true
                    }
                    if (moved) {
                        val centroid = event.calculateCentroid()
                        if (centroid.isSpecified) {
                            canvasState.applyZoom(centroid, zoom, pan)
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
                if (!moved && !sawMultiTouch && !consumedAbove) {
                    onTap(down.position, down.type)
                }
            }
        }
    }
}

/** Snaps the tape direction to multiples of 15° when close enough. */
private fun snapTapeAngle(start: Offset, end: Offset): Offset {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (len < 1f) return end
    val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble()))
    val snapped = Math.round(angle / 15.0) * 15.0
    return if (kotlin.math.abs(angle - snapped) < 5.0) {
        val rad = Math.toRadians(snapped)
        Offset(
            start.x + len * kotlin.math.cos(rad).toFloat(),
            start.y + len * kotlin.math.sin(rad).toFloat(),
        )
    } else end
}

private suspend fun AwaitPointerEventScope.transformLoop(canvasState: CanvasState) {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.none { it.pressed }) break
        val centroid = event.calculateCentroid()
        if (centroid.isSpecified) {
            canvasState.applyZoom(centroid, event.calculateZoom(), event.calculatePan())
        }
        event.changes.forEach { it.consume() }
    }
}

// ---- Elements ----

@Composable
private fun ElementView(
    element: NoteElement,
    canvasState: CanvasState,
    interactive: Boolean,
    selected: Boolean,
    inLasso: Boolean,
    pendingConnect: Boolean,
    editing: Boolean,
    editController: MarkdownEditController,
    viewModel: EditorViewModel,
    onOpenNote: (String) -> Unit,
    onReceiveImage: (android.net.Uri) -> Unit,
) {
    val density = LocalDensity.current
    val elementWidth = when (element) {
        is ImageElement -> element.width
        is TextElement -> element.width
        is NoteLinkElement -> element.width
        is WebLinkElement -> element.width
        is FileElement -> element.width
    }
    val vectorScale = element.vectorScale()
    val widthDp = with(density) { elementWidth.toDp() }

    var modifier = Modifier
        .graphicsLayer {
            translationX = element.x * canvasState.scale + canvasState.offset.x
            translationY = element.y * canvasState.scale + canvasState.offset.y
            // Element scale stacks on top of the canvas zoom: vector content
            // (text, cards) is re-rendered through the transform, so it stays
            // crisp at any size.
            scaleX = canvasState.scale * vectorScale
            scaleY = canvasState.scale * vectorScale
            transformOrigin = TransformOrigin(0f, 0f)
        }
        .width(widthDp)
        .onGloballyPositioned {
            viewModel.elementSizes[element.id] =
                Size(it.size.width.toFloat(), it.size.height.toFloat())
        }

    if (interactive && !editing) {
        modifier = modifier
            .pointerInput(element.id) {
                detectTapGestures {
                    when {
                        viewModel.tool.value == EditorTool.CONNECT ->
                            viewModel.handleConnectTap(element.id)
                        selected && element is TextElement ->
                            viewModel.editingTextId.value = element.id
                        element is TextElement &&
                            viewModel.tool.value == EditorTool.TEXT -> {
                            viewModel.selectedElementId.value = element.id
                            viewModel.editingTextId.value = element.id
                        }
                        else -> {
                            viewModel.clearSelections()
                            viewModel.selectedElementId.value = element.id
                        }
                    }
                }
            }
            .pointerInput(element.id) {
                detectDragGestures(
                    onDragStart = {
                        if (viewModel.tool.value != EditorTool.CONNECT) {
                            viewModel.beginGesture()
                            viewModel.selectedElementId.value = element.id
                        }
                    },
                ) { change, amount ->
                    if (viewModel.tool.value == EditorTool.CONNECT) return@detectDragGestures
                    change.consume()
                    // Drag amounts arrive in the element's local (layout)
                    // space, already divided by every layer transform.
                    viewModel.moveElementBy(
                        element.id,
                        amount.x * vectorScale,
                        amount.y * vectorScale,
                    )
                }
            }
    }

    if (!interactive && !editing) {
        // Finger long-press grabs the element even while a drawing tool is
        // active, so things can be rearranged without switching tool.
        modifier = modifier.pointerInput(element.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                if (down.type != PointerType.Touch) return@awaitEachGesture
                val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                press.consume()
                viewModel.beginGesture()
                viewModel.selectedElementId.value = element.id
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (change.positionChanged()) {
                        val delta = change.positionChange()
                        viewModel.moveElementBy(
                            element.id,
                            delta.x * vectorScale,
                            delta.y * vectorScale,
                        )
                        change.consume()
                    }
                }
            }
        }
    }

    if (pendingConnect) {
        val pulse = rememberInfiniteTransition(label = "pendingPulse")
        val alpha by pulse.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
            label = "pendingAlpha",
        )
        modifier = modifier.border(
            3.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        )
    }
    if (inLasso) {
        modifier = modifier.border(
            2.dp,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
        )
    }

    Box(modifier) {
        when (element) {
            is ImageElement -> ImageElementContent(element, viewModel)
            is TextElement -> TextElementContent(
                element = element,
                editing = editing,
                editController = editController,
                viewModel = viewModel,
                onReceiveImage = onReceiveImage,
            )
            is NoteLinkElement -> NoteLinkContent(element, viewModel, onOpenNote)
            is WebLinkElement -> WebLinkContent(element)
            is FileElement -> FileContent(element, viewModel)
        }
        if (selected) {
            SelectionChrome(
                element = element,
                canvasState = canvasState,
                viewModel = viewModel,
            )
        }
    }
}

private fun NoteElement.vectorScale(): Float = when (this) {
    is TextElement -> scale
    is NoteLinkElement -> scale
    is WebLinkElement -> scale
    is FileElement -> scale
    is ImageElement -> 1f
}

@Composable
private fun ImageElementContent(element: ImageElement, viewModel: EditorViewModel) {
    val noteId = viewModel.note.collectAsState().value?.id
    val density = LocalDensity.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, element.fileName, noteId) {
        if (noteId == null) return@produceState
        value = withContext(Dispatchers.IO) {
            val file = viewModel.assetFile(element.fileName)
            if (file == null || !file.exists()) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val sample = generateSequence(1) { it * 2 }
                .first { bounds.outWidth / it <= 2048 && bounds.outHeight / it <= 2048 }
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )
        }
    }
    val appear by animateFloatAsState(
        targetValue = if (bitmap != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "imageAppear",
    )
    Box(
        Modifier
            .width(with(density) { element.width.toDp() })
            .height(with(density) { element.height.toDp() })
            .scale(0.85f + 0.15f * appear)
            .graphicsLayer { alpha = appear }
            .background(if (element.isPdfPage) Color.White else Color.Transparent),
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (element.isPdfPage) {
            Text(
                "p. ${element.pdfPage}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
            )
        }
    }
}

@Composable
private fun TextElementContent(
    element: TextElement,
    editing: Boolean,
    editController: MarkdownEditController,
    viewModel: EditorViewModel,
    onReceiveImage: (android.net.Uri) -> Unit,
) {
    val settings by viewModel.settingsStore.settings.collectAsState()
    val fonts by viewModel.fontManager.fonts.collectAsState()
    val appStyle = LocalAppStyle.current
    // No explicit font → the block follows the theme's body font.
    val fontFamily = remember(element.fontId, fonts, appStyle) {
        element.fontId?.let { viewModel.fontManager.byId(it).family } ?: appStyle.bodyFont
    }
    val themeColor = MaterialTheme.colorScheme.onSurface
    val color = element.color?.let { Color(it) } ?: themeColor
    val textStyle = baseTextStyle(settings.styleSet, element.styleId, fontFamily, color)
    val fontResolver: (String) -> androidx.compose.ui.text.font.FontFamily? = { id ->
        viewModel.fontManager.byId(id).family
    }

    val bgModifier =
        if (element.bgColor != null) {
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(element.bgColor))
        } else Modifier

    if (editing) {
        MarkdownTextEditor(
            text = element.text,
            onTextChanged = { newText ->
                viewModel.updateElement(element.id, live = true) {
                    (it as TextElement).copy(text = newText)
                }
            },
            styleSet = settings.styleSet,
            styleId = element.styleId,
            baseColor = color.toArgb(),
            baseTypeface = viewModel.fontManager.typefaceOf(
                element.fontId ?: appStyle.bodyFontId,
            ),
            displayTypeface = viewModel.fontManager.typefaceOf(appStyle.displayFontId),
            fontManager = viewModel.fontManager,
            controller = editController,
            onReceiveImage = onReceiveImage,
            modifier = Modifier
                .fillMaxWidth()
                .then(bgModifier)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
        )
    } else {
        Text(
            text = styleMarkdown(
                element.text.ifEmpty { "Scrivi…" },
                settings.styleSet,
                color,
                fontResolver,
                displayFont = appStyle.displayFont,
            ),
            style = textStyle,
            color = if (element.text.isEmpty()) color.copy(alpha = 0.4f) else Color.Unspecified,
            modifier = Modifier
                .fillMaxWidth()
                .then(bgModifier)
                .padding(if (element.bgColor != null) 10.dp else 4.dp),
        )
    }
}

@Composable
private fun WebLinkContent(element: WebLinkElement) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val host = remember(element.url) {
        runCatching { java.net.URI(element.url).host.orEmpty() }
            .getOrDefault("")
            .removePrefix("www.")
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(
                1.5.dp,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Lucide.Globe,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                element.title.ifBlank { host.ifBlank { element.url } },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(element.url),
                            ),
                        )
                    }
                },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Lucide.ArrowUpRight,
                    contentDescription = "Apri link",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (element.title.isNotBlank() && host.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                host,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun FileContent(element: FileElement, viewModel: EditorViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sizeLabel = remember(element.sizeBytes) {
        val kb = element.sizeBytes / 1024.0
        when {
            kb < 1 -> "${element.sizeBytes} B"
            kb < 1024 -> "%.0f KB".format(kb)
            else -> "%.1f MB".format(kb / 1024.0)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            when {
                element.mimeType.startsWith("audio") -> Lucide.File
                element.mimeType.startsWith("video") -> Lucide.File
                element.mimeType == "application/pdf" -> Lucide.FileText
                element.mimeType.startsWith("text") -> Lucide.FileText
                else -> Lucide.Paperclip
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                element.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                sizeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        IconButton(
            onClick = {
                runCatching {
                    val file = viewModel.assetFile(element.fileName) ?: return@runCatching
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileprovider",
                        file,
                    )
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, element.mimeType)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                    )
                }
            },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                Lucide.ArrowUpRight,
                contentDescription = "Apri file",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NoteLinkContent(
    element: NoteLinkElement,
    viewModel: EditorViewModel,
    onOpenNote: (String) -> Unit,
) {
    val preview by produceState<NoteEntity?>(null, element.targetNoteId) {
        value = viewModel.notePreview(element.targetNoteId)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(
                1.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Lucide.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                preview?.title?.ifBlank { "Senza titolo" } ?: "Nota collegata",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onOpenNote(element.targetNoteId) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Lucide.ArrowUpRight,
                    contentDescription = "Apri nota",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        preview?.plainText?.takeIf { it.isNotBlank() }?.let { snippet ->
            Spacer(Modifier.height(4.dp))
            Text(
                snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BoxScope.SelectionChrome(
    element: NoteElement,
    canvasState: CanvasState,
    viewModel: EditorViewModel,
) {
    Box(
        Modifier
            .matchParentSize()
            .border(2.dp, MaterialTheme.colorScheme.primary),
    )
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 16.dp, y = (-16).dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = { viewModel.deleteElement(element.id) }) {
            Icon(
                Lucide.Trash2,
                contentDescription = "Elimina elemento",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp),
            )
        }
    }
    // Bottom-right: vector zoom (text/cards stay crisp); images scale w/h.
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 14.dp, y = 14.dp)
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(element.id) {
                detectDragGestures(
                    onDragStart = { viewModel.beginGesture() },
                ) { change, amount ->
                    change.consume()
                    viewModel.updateElement(element.id, live = true) { e ->
                        // amount is in the element's layout units.
                        val factor = 1f + amount.x / 360f
                        when (e) {
                            is TextElement ->
                                e.copy(scale = (e.scale * factor).coerceIn(0.3f, 6f))
                            is NoteLinkElement ->
                                e.copy(scale = (e.scale * factor).coerceIn(0.3f, 6f))
                            is WebLinkElement ->
                                e.copy(scale = (e.scale * factor).coerceIn(0.3f, 6f))
                            is FileElement ->
                                e.copy(scale = (e.scale * factor).coerceIn(0.3f, 6f))
                            is ImageElement -> {
                                val ratio = e.height / e.width
                                val newW = (e.width + amount.x).coerceAtLeast(80f)
                                e.copy(width = newW, height = newW * ratio)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Lucide.MoveDiagonal,
            contentDescription = "Ridimensiona",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
    // Right edge: reflow width for text blocks.
    if (element is TextElement) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 14.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
                .pointerInput(element.id) {
                    detectDragGestures(
                        onDragStart = { viewModel.beginGesture() },
                    ) { change, amount ->
                        change.consume()
                        viewModel.updateElement(element.id, live = true) { e ->
                            (e as TextElement)
                                .copy(width = (e.width + amount.x).coerceAtLeast(120f))
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Lucide.MoveHorizontal,
                contentDescription = "Larghezza",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ---- Lasso chrome ----

@Composable
private fun LassoSelectionBox(
    bounds: Rect,
    canvasState: CanvasState,
    viewModel: EditorViewModel,
) {
    val density = LocalDensity.current
    val pad = 14f
    val topLeft = Offset(
        (bounds.left - pad) * canvasState.scale + canvasState.offset.x,
        (bounds.top - pad) * canvasState.scale + canvasState.offset.y,
    )
    val sizePx = Size(
        (bounds.width + pad * 2) * canvasState.scale,
        (bounds.height + pad * 2) * canvasState.scale,
    )
    Box(
        Modifier
            .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            .size(
                with(density) { sizePx.width.toDp() },
                with(density) { sizePx.height.toDp() },
            )
            .border(
                2.dp,
                MaterialTheme.colorScheme.tertiary,
                RoundedCornerShape(8.dp),
            )
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { viewModel.beginGesture() },
                ) { change, amount ->
                    change.consume()
                    viewModel.moveLassoBy(
                        amount.x / canvasState.scale,
                        amount.y / canvasState.scale,
                    )
                }
            },
    ) {
        Icon(
            Lucide.Move,
            contentDescription = "Sposta selezione",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(18.dp),
        )
    }
}

/** Actions for the selected element: duplicate, z-order, bg color, delete. */
@Composable
private fun ElementActionBar(
    element: NoteElement,
    paletteColors: kotlin.collections.List<Long>,
    viewModel: EditorViewModel,
) {
    var bgMenuOpen by remember { mutableStateOf(false) }
    val stickyColors = viewModel.currentTheme().resolvedStickyColors()
    Row(
        Modifier
            .glass(corner = 32.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { viewModel.duplicateElement(element.id) }) {
            Icon(Lucide.Copy, contentDescription = "Duplica")
        }
        IconButton(onClick = { viewModel.bringToFront(element.id) }) {
            Icon(Lucide.BringToFront, contentDescription = "Porta avanti")
        }
        IconButton(onClick = { viewModel.sendToBack(element.id) }) {
            Icon(Lucide.SendToBack, contentDescription = "Porta dietro")
        }
        if (element is TextElement) {
            Box {
                IconButton(onClick = { bgMenuOpen = true }) {
                    Icon(Lucide.PaintBucket, contentDescription = "Colore sfondo")
                }
                DropdownMenu(
                    expanded = bgMenuOpen,
                    onDismissRequest = { bgMenuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Nessuno") },
                        onClick = {
                            bgMenuOpen = false
                            viewModel.updateElement(element.id) {
                                (it as TextElement).copy(bgColor = null)
                            }
                        },
                    )
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        (stickyColors + paletteColors.take(3).map(::softBackground))
                            .distinct().take(8).forEach { soft ->
                            Box(
                                Modifier
                                    .padding(3.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(soft))
                                    .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                                    .pointerInput(soft) {
                                        detectTapGestures {
                                            bgMenuOpen = false
                                            viewModel.updateElement(element.id) {
                                                (it as TextElement).copy(bgColor = soft)
                                            }
                                        }
                                    },
                            )
                        }
                    }
                }
            }
        }
        IconButton(onClick = { viewModel.deleteElement(element.id) }) {
            Icon(
                Lucide.Trash2,
                contentDescription = "Elimina",
                tint = MaterialTheme.colorScheme.error,
            )
        }
        IconButton(onClick = { viewModel.selectedElementId.value = null }) {
            Icon(Lucide.X, contentDescription = "Chiudi")
        }
    }
}

/** Blends a palette color toward white for a readable sticky-note tint. */
private fun softBackground(color: Long): Long {
    val r = ((color shr 16) and 0xFF).toInt()
    val g = ((color shr 8) and 0xFF).toInt()
    val b = (color and 0xFF).toInt()
    fun soften(v: Int) = (v + (255 - v) * 0.65f).toInt().coerceIn(0, 255)
    return 0xFF000000 or
        (soften(r).toLong() shl 16) or (soften(g).toLong() shl 8) or soften(b).toLong()
}

@Composable
private fun LassoActionBar(
    selection: LassoSelection,
    hasGroup: Boolean,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        Modifier
            .glass(corner = 32.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${selection.elementIds.size + selection.strokeIds.size} selezionati",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(8.dp))
        if (selection.elementIds.size >= 2) {
            IconButton(onClick = onGroup) {
                Icon(Lucide.Group, contentDescription = "Raggruppa")
            }
        }
        if (hasGroup) {
            IconButton(onClick = onUngroup) {
                Icon(Lucide.Ungroup, contentDescription = "Separa gruppo")
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Lucide.Trash2,
                contentDescription = "Elimina selezione",
                tint = MaterialTheme.colorScheme.error,
            )
        }
        IconButton(onClick = onClose) {
            Icon(Lucide.X, contentDescription = "Chiudi")
        }
    }
}

// ---- Connector handle ----

@Composable
private fun ConnectorHandle(
    connector: ConnectorElement,
    content: NoteContent,
    canvasState: CanvasState,
    viewModel: EditorViewModel,
) {
    val geo = connectorGeometry(connector, content, viewModel.elementSizes) ?: return
    val mid = geo.midpoint
    val screen = Offset(
        mid.x * canvasState.scale + canvasState.offset.x,
        mid.y * canvasState.scale + canvasState.offset.y,
    )
    Box(
        Modifier
            .offset {
                IntOffset(
                    (screen.x - 14.dp.toPx()).roundToInt(),
                    (screen.y - 14.dp.toPx()).roundToInt(),
                )
            }
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .pointerInput(connector.id) {
                detectDragGestures(
                    onDragStart = { viewModel.beginGesture() },
                ) { change, amount ->
                    change.consume()
                    // The visible midpoint moves at half the control-point speed.
                    val dx = amount.x / canvasState.scale * 2f
                    val dy = amount.y / canvasState.scale * 2f
                    viewModel.updateConnector(connector.id, live = true) {
                        it.copy(curveDx = it.curveDx + dx, curveDy = it.curveDy + dy)
                    }
                }
            },
    )
}

// ---- Frame handles ----

@Composable
private fun FrameHandles(
    frame: FrameElement,
    canvasState: CanvasState,
    viewModel: EditorViewModel,
) {
    fun screenOf(x: Float, y: Float) = Offset(
        x * canvasState.scale + canvasState.offset.x,
        y * canvasState.scale + canvasState.offset.y,
    )

    // Move grip (top-left).
    val tl = screenOf(frame.x, frame.y)
    Box(
        Modifier
            .offset { IntOffset((tl.x - 16.dp.toPx()).roundToInt(), (tl.y - 16.dp.toPx()).roundToInt()) }
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(frame.color))
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .pointerInput(frame.id) {
                detectDragGestures(
                    onDragStart = { viewModel.beginGesture() },
                ) { change, amount ->
                    change.consume()
                    viewModel.moveFrameBy(
                        frame.id,
                        amount.x / canvasState.scale,
                        amount.y / canvasState.scale,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Lucide.Move,
            contentDescription = "Sposta cornice",
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }

    // Resize handle (bottom-right).
    val br = screenOf(frame.x + frame.width, frame.y + frame.height)
    Box(
        Modifier
            .offset { IntOffset((br.x - 14.dp.toPx()).roundToInt(), (br.y - 14.dp.toPx()).roundToInt()) }
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(frame.color))
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .pointerInput(frame.id) {
                detectDragGestures(
                    onDragStart = { viewModel.beginGesture() },
                ) { change, amount ->
                    change.consume()
                    viewModel.updateFrame(frame.id, live = true) {
                        it.copy(
                            width = (it.width + amount.x / canvasState.scale)
                                .coerceAtLeast(120f),
                            height = (it.height + amount.y / canvasState.scale)
                                .coerceAtLeast(120f),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Lucide.MoveDiagonal,
            contentDescription = "Ridimensiona cornice",
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}

// ---- Tape chrome ----

@Composable
private fun TapeHandles(
    tape: TapeElement,
    canvasState: CanvasState,
    viewModel: EditorViewModel,
) {
    fun screenOf(x: Float, y: Float) = Offset(
        x * canvasState.scale + canvasState.offset.x,
        y * canvasState.scale + canvasState.offset.y,
    )

    @Composable
    fun handle(
        cx: Float,
        cy: Float,
        onDrag: (Float, Float) -> Unit,
        key: Any,
    ) {
        val pos = screenOf(cx, cy)
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (pos.x - 13.dp.toPx()).roundToInt(),
                        (pos.y - 13.dp.toPx()).roundToInt(),
                    )
                }
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(tape.color))
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .pointerInput(key) {
                    detectDragGestures(
                        onDragStart = { viewModel.beginGesture() },
                    ) { change, amount ->
                        change.consume()
                        onDrag(amount.x / canvasState.scale, amount.y / canvasState.scale)
                    }
                },
        )
    }

    handle(tape.x1, tape.y1, { dx, dy ->
        viewModel.updateTape(tape.id, live = true) {
            it.copy(x1 = it.x1 + dx, y1 = it.y1 + dy)
        }
    }, "start_" + tape.id)
    handle(tape.x2, tape.y2, { dx, dy ->
        viewModel.updateTape(tape.id, live = true) {
            it.copy(x2 = it.x2 + dx, y2 = it.y2 + dy)
        }
    }, "end_" + tape.id)
    handle((tape.x1 + tape.x2) / 2f, (tape.y1 + tape.y2) / 2f, { dx, dy ->
        viewModel.updateTape(tape.id, live = true) {
            it.copy(
                x1 = it.x1 + dx, y1 = it.y1 + dy,
                x2 = it.x2 + dx, y2 = it.y2 + dy,
            )
        }
    }, "mid_" + tape.id)
}

@Composable
private fun TapeStyleBar(
    tape: TapeElement,
    tapeColors: kotlin.collections.List<Long>,
    onUpdate: ((TapeElement) -> TapeElement) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .glass(corner = 32.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TapePattern.entries.forEach { pattern ->
            TapePatternPreviewButton(
                pattern = pattern,
                color = tape.color,
                selected = tape.pattern == pattern,
                onClick = { onUpdate { it.copy(pattern = pattern) } },
            )
        }
        Spacer(Modifier.width(6.dp))
        ColorDots(
            colors = tapeColors.distinct().take(9),
            selected = tape.color,
            onPick = { c -> onUpdate { it.copy(color = c) } },
        )
        Spacer(Modifier.width(6.dp))
        Slider(
            value = tape.thickness,
            onValueChange = { t -> onUpdate { it.copy(thickness = t) } },
            valueRange = 14f..90f,
            modifier = Modifier.width(110.dp),
        )
        IconButton(onClick = onDelete) {
            Icon(
                Lucide.Trash2,
                contentDescription = "Elimina nastro",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TapePatternPreviewButton(
    pattern: TapePattern,
    color: Long,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(width = 48.dp, height = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
            )
            .pointerInput(pattern) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 40.dp, height = 18.dp)) {
            drawTape(
                TapeElement(
                    x1 = 0f, y1 = size.height / 2f,
                    x2 = size.width, y2 = size.height / 2f,
                    thickness = size.height,
                    color = color,
                    pattern = pattern,
                ),
                selected = false,
            )
        }
    }
}

// ---- Bars ----

@Composable
private fun EditorTopBar(
    title: String,
    onTitleChange: (String) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit,
    onAddImage: () -> Unit,
    onAddPdf: () -> Unit,
    onPaste: () -> Unit,
    onAddNoteLink: () -> Unit,
    onAddWebLink: () -> Unit,
    onAttachFile: () -> Unit,
    onAddSticky: () -> Unit,
    onExportPdf: () -> Unit,
    onPickTheme: () -> Unit,
    background: CanvasBackground,
    onBackgroundChange: (CanvasBackground) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glass()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Lucide.ArrowLeft, contentDescription = "Indietro")
        }
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            singleLine = true,
            modifier = Modifier.weight(1f),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box {
                    if (title.isEmpty()) {
                        Text(
                            "Titolo nota",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    inner()
                }
            },
        )
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Lucide.Undo2, contentDescription = "Annulla")
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(Lucide.Redo2, contentDescription = "Ripeti")
        }
        IconButton(onClick = onPickTheme) {
            Icon(Lucide.Palette, contentDescription = "Tema")
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Lucide.Plus, contentDescription = "Aggiungi")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Sticky note") },
                    leadingIcon = { Icon(Lucide.StickyNote, null) },
                    onClick = {
                        menuOpen = false
                        onAddSticky()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Aggiungi immagine") },
                    leadingIcon = { Icon(Lucide.Image, null) },
                    onClick = {
                        menuOpen = false
                        onAddImage()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Incolla immagine") },
                    leadingIcon = { Icon(Lucide.ClipboardPaste, null) },
                    onClick = {
                        menuOpen = false
                        onPaste()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Collega nota") },
                    leadingIcon = { Icon(Lucide.Link, null) },
                    onClick = {
                        menuOpen = false
                        onAddNoteLink()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Aggiungi link web") },
                    leadingIcon = { Icon(Lucide.Globe, null) },
                    onClick = {
                        menuOpen = false
                        onAddWebLink()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Allega file") },
                    leadingIcon = { Icon(Lucide.Paperclip, null) },
                    onClick = {
                        menuOpen = false
                        onAttachFile()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Importa PDF") },
                    leadingIcon = { Icon(Lucide.FileText, null) },
                    onClick = {
                        menuOpen = false
                        onAddPdf()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Esporta in PDF") },
                    leadingIcon = { Icon(Lucide.FileDown, null) },
                    onClick = {
                        menuOpen = false
                        onExportPdf()
                    },
                )
                CanvasBackground.entries.forEach { bg ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Sfondo: " + when (bg) {
                                    CanvasBackground.BLANK -> "vuoto"
                                    CanvasBackground.DOTS -> "punti"
                                    CanvasBackground.GRID -> "griglia"
                                    CanvasBackground.LINES -> "righe"
                                    CanvasBackground.PAPER -> "carta"
                                    CanvasBackground.SCANLINES -> "scanline"
                                },
                            )
                        },
                        leadingIcon = {
                            if (background == bg) Icon(Lucide.Check, null)
                        },
                        onClick = {
                            menuOpen = false
                            onBackgroundChange(bg)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorToolBar(
    tool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    currentColor: Long,
    onOpenWheel: (Offset) -> Unit,
) {
    Row(
        Modifier
            .glass(corner = 32.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(Lucide.Hand, "Selezione", tool == EditorTool.SELECT) {
            onToolSelected(EditorTool.SELECT)
        }
        ToolButton(Lucide.Pen, "Penna", tool == EditorTool.PEN) {
            onToolSelected(EditorTool.PEN)
        }
        ToolButton(Lucide.Highlighter, "Evidenziatore", tool == EditorTool.HIGHLIGHTER) {
            onToolSelected(EditorTool.HIGHLIGHTER)
        }
        ToolButton(Lucide.Eraser, "Gomma", tool == EditorTool.ERASER) {
            onToolSelected(EditorTool.ERASER)
        }
        ToolButton(Lucide.Type, "Testo", tool == EditorTool.TEXT) {
            onToolSelected(EditorTool.TEXT)
        }
        ToolButton(Lucide.Spline, "Collega", tool == EditorTool.CONNECT) {
            onToolSelected(EditorTool.CONNECT)
        }
        ToolButton(Lucide.Lasso, "Lazo", tool == EditorTool.LASSO) {
            onToolSelected(EditorTool.LASSO)
        }
        ToolButton(Lucide.Frame, "Cornice", tool == EditorTool.FRAME) {
            onToolSelected(EditorTool.FRAME)
        }
        ToolButton(Lucide.Slash, "Nastro", tool == EditorTool.TAPE) {
            onToolSelected(EditorTool.TAPE)
        }
        Spacer(Modifier.width(8.dp))
        var buttonCenter by remember { mutableStateOf(Offset.Zero) }
        Box(
            Modifier
                .onGloballyPositioned {
                    val pos = it.positionInWindow()
                    buttonCenter = Offset(
                        pos.x + it.size.width / 2f,
                        pos.y - 280f,
                    )
                }
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(currentColor))
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    CircleShape,
                )
                .pointerInput(Unit) {
                    detectTapGestures { onOpenWheel(buttonCenter) }
                },
        )
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue =
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        animationSpec = tween(200),
        label = "toolBg",
    )
    val motion = LocalAppStyle.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = motion.motionDamping,
            stiffness = motion.motionStiffness,
        ),
        label = "toolScale",
    )
    Box(
        Modifier
            .scale(scale)
            .clip(CircleShape)
            .background(bg),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint =
                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Connector style bar ----

@Composable
private fun ConnectorStyleBar(
    connector: ConnectorElement,
    paletteColors: kotlin.collections.List<Long>,
    onUpdate: ((ConnectorElement) -> ConnectorElement) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .glass(corner = 32.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineStyle.entries.forEach { style ->
            LinePreviewButton(
                lineStyle = style,
                selected = connector.lineStyle == style,
                onClick = { onUpdate { it.copy(lineStyle = style) } },
            )
        }
        Spacer(Modifier.width(4.dp))
        ToolButton(Lucide.Zap, "Animata", connector.animated) {
            onUpdate { it.copy(animated = !it.animated) }
        }
        Spacer(Modifier.width(4.dp))
        CapPreviewButton(
            cap = connector.startCap,
            mirrored = true,
            onClick = { onUpdate { it.copy(startCap = it.startCap.next()) } },
        )
        CapPreviewButton(
            cap = connector.endCap,
            mirrored = false,
            onClick = { onUpdate { it.copy(endCap = it.endCap.next()) } },
        )
        Spacer(Modifier.width(6.dp))
        ColorDots(
            colors = paletteColors.take(8),
            selected = connector.color,
            onPick = { c -> onUpdate { it.copy(color = c) } },
        )
        Spacer(Modifier.width(6.dp))
        Slider(
            value = connector.width,
            onValueChange = { w -> onUpdate { it.copy(width = w) } },
            valueRange = 1.5f..12f,
            modifier = Modifier.width(110.dp),
        )
        IconButton(onClick = onDelete) {
            Icon(
                Lucide.Trash2,
                contentDescription = "Elimina collegamento",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ---- Frame style bar ----

@Composable
private fun FrameStyleBar(
    frame: FrameElement,
    paletteColors: kotlin.collections.List<Long>,
    onUpdate: ((FrameElement) -> FrameElement) -> Unit,
    onDelete: () -> Unit,
) {
    var label by remember(frame.id) { mutableStateOf(frame.label) }
    Row(
        Modifier
            .glass(corner = 32.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FrameShape.entries.forEach { shape ->
            FrameShapePreviewButton(
                shape = shape,
                selected = frame.shape == shape,
                onClick = { onUpdate { it.copy(shape = shape) } },
            )
        }
        Spacer(Modifier.width(4.dp))
        LineStyle.entries.forEach { style ->
            LinePreviewButton(
                lineStyle = style,
                selected = frame.lineStyle == style,
                onClick = { onUpdate { it.copy(lineStyle = style) } },
            )
        }
        ToolButton(Lucide.Zap, "Animata", frame.animated) {
            onUpdate { it.copy(animated = !it.animated) }
        }
        ToolButton(Lucide.Pipette, "Riempimento", frame.filled) {
            onUpdate { it.copy(filled = !it.filled) }
        }
        Spacer(Modifier.width(6.dp))
        ColorDots(
            colors = paletteColors.take(8),
            selected = frame.color,
            onPick = { c -> onUpdate { it.copy(color = c) } },
        )
        Spacer(Modifier.width(6.dp))
        Slider(
            value = frame.strokeWidth,
            onValueChange = { w -> onUpdate { it.copy(strokeWidth = w) } },
            valueRange = 1.5f..12f,
            modifier = Modifier.width(90.dp),
        )
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = label,
            onValueChange = {
                label = it
                onUpdate { f -> f.copy(label = it) }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .width(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    if (label.isEmpty()) {
                        Text(
                            "Etichetta",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    inner()
                }
            },
        )
        IconButton(onClick = onDelete) {
            Icon(
                Lucide.Trash2,
                contentDescription = "Elimina cornice",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ColorDots(
    colors: kotlin.collections.List<Long>,
    selected: Long,
    onPick: (Long) -> Unit,
) {
    colors.forEach { c ->
        Box(
            Modifier
                .padding(horizontal = 3.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(c))
                .border(
                    width = if (selected == c) 3.dp else 1.dp,
                    color =
                    if (selected == c) MaterialTheme.colorScheme.primary
                    else Color.Black.copy(alpha = 0.15f),
                    shape = CircleShape,
                )
                .pointerInput(c) {
                    detectTapGestures { onPick(c) }
                },
        )
    }
}

private fun CapStyle.next(): CapStyle = when (this) {
    CapStyle.NONE -> CapStyle.ARROW
    CapStyle.ARROW -> CapStyle.DOT
    CapStyle.DOT -> CapStyle.NONE
}

@Composable
private fun FrameShapePreviewButton(
    shape: FrameShape,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(width = 40.dp, height = 36.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
            )
            .pointerInput(shape) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 24.dp, height = 18.dp)) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
            when (shape) {
                FrameShape.RECT -> drawRect(color, style = stroke)
                FrameShape.ROUNDED -> drawRoundRect(
                    color,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    style = stroke,
                )
                FrameShape.ELLIPSE -> drawOval(color, style = stroke)
                FrameShape.SKETCHY -> {
                    drawRoundRect(
                        color,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 9f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 3f)),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinePreviewButton(
    lineStyle: LineStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(width = 44.dp, height = 36.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
            )
            .pointerInput(lineStyle) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 28.dp, height = 12.dp)) {
            val effect = when (lineStyle) {
                LineStyle.SOLID -> null
                LineStyle.DASHED ->
                    PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                LineStyle.DOTTED ->
                    PathEffect.dashPathEffect(floatArrayOf(0.5f, 9f))
            }
            drawLine(
                color,
                Offset(0f, size.height / 2),
                Offset(size.width, size.height / 2),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
                pathEffect = effect,
            )
        }
    }
}

@Composable
private fun CapPreviewButton(
    cap: CapStyle,
    mirrored: Boolean,
    onClick: () -> Unit,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(width = 40.dp, height = 36.dp)
            .clip(CircleShape)
            .pointerInput(cap, mirrored) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .size(width = 26.dp, height = 14.dp)
                .graphicsLayer { if (mirrored) scaleX = -1f },
        ) {
            val midY = size.height / 2
            drawLine(
                color,
                Offset(0f, midY),
                Offset(size.width - 4f, midY),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
            when (cap) {
                CapStyle.NONE -> Unit
                CapStyle.DOT -> drawCircle(color, 6f, Offset(size.width - 6f, midY))
                CapStyle.ARROW -> {
                    drawLine(
                        color,
                        Offset(size.width - 12f, midY - 7f),
                        Offset(size.width - 2f, midY),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color,
                        Offset(size.width - 12f, midY + 7f),
                        Offset(size.width - 2f, midY),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

// ---- Text format bar ----

@Composable
private fun TextFormatBar(
    controller: MarkdownEditController,
    fontManager: FontManager,
    paletteColors: kotlin.collections.List<Long>,
    currentFontId: String?,
    onFontSelected: (String) -> Unit,
    onColorSelected: (Long) -> Unit,
    onColorAuto: () -> Unit,
    onDone: () -> Unit,
) {
    val fonts by fontManager.fonts.collectAsState()
    var fontMenuOpen by remember { mutableStateOf(false) }
    var styleMenuOpen by remember { mutableStateOf(false) }
    var colorMenuOpen by remember { mutableStateOf(false) }

    Row(
        Modifier
            .glass(corner = 32.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            IconButton(onClick = { styleMenuOpen = true }) {
                Text(
                    "T",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(
                expanded = styleMenuOpen,
                onDismissRequest = { styleMenuOpen = false },
            ) {
                listOf(
                    "Titolo 1" to "# ",
                    "Titolo 2" to "## ",
                    "Titolo 3" to "### ",
                    "Corpo" to " ",
                ).forEach { (label, prefix) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            styleMenuOpen = false
                            controller.toggleLinePrefix(prefix)
                        },
                    )
                }
            }
        }
        IconButton(onClick = { controller.wrap("**") }) {
            Icon(Lucide.Bold, contentDescription = "Grassetto")
        }
        IconButton(onClick = { controller.wrap("*") }) {
            Icon(Lucide.Italic, contentDescription = "Corsivo")
        }
        IconButton(onClick = { controller.wrap("~~") }) {
            Icon(Lucide.Strikethrough, contentDescription = "Barrato")
        }
        IconButton(onClick = { controller.wrap("`") }) {
            Icon(Lucide.Code, contentDescription = "Codice")
        }
        IconButton(onClick = { controller.toggleLinePrefix("- ") }) {
            Icon(Lucide.List, contentDescription = "Elenco")
        }
        IconButton(onClick = { controller.toggleLinePrefix("- [ ] ") }) {
            Icon(Lucide.ListChecks, contentDescription = "Checklist")
        }
        IconButton(onClick = { controller.toggleLinePrefix("> ") }) {
            Icon(Lucide.TextQuote, contentDescription = "Citazione")
        }
        // Text color: applies to the selection (inline tag) or to the block.
        Box {
            IconButton(onClick = { colorMenuOpen = true }) {
                Icon(Lucide.Palette, contentDescription = "Colore testo")
            }
            DropdownMenu(
                expanded = colorMenuOpen,
                onDismissRequest = { colorMenuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Automatico (tema)") },
                    onClick = {
                        colorMenuOpen = false
                        onColorAuto()
                    },
                )
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    paletteColors.take(6).forEach { c ->
                        Box(
                            Modifier
                                .padding(3.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    1.dp,
                                    Color.Black.copy(alpha = 0.2f),
                                    CircleShape,
                                )
                                .pointerInput(c) {
                                    detectTapGestures {
                                        colorMenuOpen = false
                                        onColorSelected(c)
                                    }
                                },
                        )
                    }
                }
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    paletteColors.drop(6).take(6).forEach { c ->
                        Box(
                            Modifier
                                .padding(3.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    1.dp,
                                    Color.Black.copy(alpha = 0.2f),
                                    CircleShape,
                                )
                                .pointerInput(c) {
                                    detectTapGestures {
                                        colorMenuOpen = false
                                        onColorSelected(c)
                                    }
                                },
                        )
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { fontMenuOpen = true }) {
                Icon(Lucide.Type, contentDescription = "Font")
            }
            DropdownMenu(
                expanded = fontMenuOpen,
                onDismissRequest = { fontMenuOpen = false },
            ) {
                fonts.forEach { font ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                font.name,
                                fontFamily = font.family,
                                fontWeight =
                                if (font.id == currentFontId) FontWeight.Bold else null,
                            )
                        },
                        onClick = {
                            fontMenuOpen = false
                            onFontSelected(font.id)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDone) {
            Icon(
                Lucide.Check,
                contentDescription = "Fine",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---- Note picker ----

@Composable
private fun NotePickerDialog(
    notes: kotlin.collections.List<NoteEntity>,
    onPick: (NoteEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = notes.filter {
        query.isBlank() || it.title.contains(query, ignoreCase = true) ||
            it.plainText.contains(query, ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collega una nota") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cerca…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(280.dp)) {
                    items(filtered, key = { it.id }) { note ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .pointerInput(note.id) {
                                    detectTapGestures { onPick(note) }
                                }
                                .padding(10.dp),
                        ) {
                            Text(
                                note.title.ifBlank { "Senza titolo" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (note.plainText.isNotBlank()) {
                                Text(
                                    note.plainText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}
