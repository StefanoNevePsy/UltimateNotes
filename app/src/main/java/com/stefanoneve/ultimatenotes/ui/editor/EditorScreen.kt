package com.stefanoneve.ultimatenotes.ui.editor

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixNormal
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stefanoneve.ultimatenotes.data.fonts.FontManager
import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.ImageElement
import com.stefanoneve.ultimatenotes.data.model.InkStroke
import com.stefanoneve.ultimatenotes.data.model.NoteElement
import com.stefanoneve.ultimatenotes.data.model.StrokePoint
import com.stefanoneve.ultimatenotes.data.model.TextElement
import com.stefanoneve.ultimatenotes.util.SPenEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private fun NoteElement.movedBy(dx: Float, dy: Float): NoteElement = when (this) {
    is TextElement -> copy(x = x + dx, y = y + dy)
    is ImageElement -> copy(x = x + dx, y = y + dy)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: String,
    onBack: () -> Unit,
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
    val editingTextId by viewModel.editingTextId.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val settings by viewModel.settingsStore.settings.collectAsState()

    val canvasState = remember { CanvasState() }
    val activeStroke = remember { mutableStateOf<InkStroke?>(null) }
    var radialCenter by remember { mutableStateOf<Offset?>(null) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }

    // Editing buffer for the text element currently open in the keyboard.
    val editingElement = content.elements
        .filterIsInstance<TextElement>()
        .firstOrNull { it.id == editingTextId }
    var editingValue by remember(editingTextId) {
        mutableStateOf(TextFieldValue(editingElement?.text.orEmpty()))
    }

    fun applyEditingValue(value: TextFieldValue) {
        editingValue = value
        editingTextId?.let { id ->
            viewModel.updateElement(id, live = true) {
                (it as TextElement).copy(text = value.text)
            }
        }
    }

    fun stopEditingText() {
        editingTextId?.let { id ->
            val element = viewModel.content.value.elements
                .filterIsInstance<TextElement>().firstOrNull { it.id == id }
            // Drop empty leftover blocks.
            if (element != null && element.text.isBlank()) viewModel.deleteElement(id)
        }
        viewModel.editingTextId.value = null
    }

    // S Pen barrel button → toggle the radial wheel under the pen tip.
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
            val at = canvasState.toWorld(Offset(300f, 400f))
            viewModel.importImage(it, at.x, at.y)
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val at = canvasState.toWorld(Offset(100f, 200f))
            viewModel.importPdf(it, at.x, at.y)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.positionInWindow() },
    ) {
        Scaffold(
            topBar = {
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
                    background = content.background,
                    onBackgroundChange = viewModel::setBackground,
                )
            },
            bottomBar = {
                if (editingTextId != null) {
                    TextFormatBar(
                        value = editingValue,
                        onValueChange = ::applyEditingValue,
                        fontManager = viewModel.fontManager,
                        currentFontId = editingElement?.fontId,
                        onFontSelected = { fontId ->
                            editingTextId?.let { id ->
                                viewModel.updateElement(id) {
                                    (it as TextElement).copy(fontId = fontId)
                                }
                            }
                        },
                        onDone = ::stopEditingText,
                    )
                } else {
                    EditorToolBar(
                        tool = tool,
                        onToolSelected = { viewModel.tool.value = it },
                        currentColor =
                        if (tool == EditorTool.HIGHLIGHTER) highlighterColor else penColor,
                        onOpenWheel = { center -> radialCenter = center - rootOrigin },
                    )
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                                when {
                                    editingTextId != null -> stopEditingText()
                                    viewModel.tool.value == EditorTool.TEXT -> {
                                        val world = canvasState.toWorld(position)
                                        viewModel.addTextElement(world.x, world.y)
                                    }
                                    else -> viewModel.selectedElementId.value = null
                                }
                            },
                            activeStroke = activeStroke,
                        ),
                )

                val interactive = tool == EditorTool.SELECT || tool == EditorTool.TEXT
                content.elements.forEach { element ->
                    androidx.compose.runtime.key(element.id) {
                        ElementView(
                            element = element,
                            canvasState = canvasState,
                            interactive = interactive,
                            selected = selectedElementId == element.id,
                            editing = editingTextId == element.id,
                            editingValue = editingValue,
                            onEditingValueChange = ::applyEditingValue,
                            viewModel = viewModel,
                            onReceiveImage = { uri ->
                                viewModel.importImage(
                                    uri,
                                    element.x,
                                    element.y + 80f,
                                )
                            },
                        )
                    }
                }
            }
        }

        radialCenter?.let { center ->
            RadialMenu(
                center = center,
                currentTool = tool,
                currentColor =
                if (tool == EditorTool.HIGHLIGHTER) highlighterColor else penColor,
                currentWidth =
                if (tool == EditorTool.HIGHLIGHTER) highlighterWidth else penWidth,
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
    activeStroke: MutableState<InkStroke?>,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val isEraserTip = down.type == PointerType.Eraser
        val isStylus = down.type == PointerType.Stylus || isEraserTip
        val tool = if (isEraserTip) EditorTool.ERASER else toolProvider()
        val drawingTool =
            tool == EditorTool.PEN || tool == EditorTool.HIGHLIGHTER || tool == EditorTool.ERASER
        val canDraw = drawingTool && (isStylus || !stylusOnlyProvider())

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
                        // A second finger landed: this is a pan/zoom, not a stroke.
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

            else -> {
                var moved = false
                var totalPan = Offset.Zero
                var sawMultiTouch = false
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break
                    if (pressed.size > 1) sawMultiTouch = true
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
                if (!moved && !sawMultiTouch) onTap(down.position, down.type)
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.transformLoop(
    canvasState: CanvasState,
) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ElementView(
    element: NoteElement,
    canvasState: CanvasState,
    interactive: Boolean,
    selected: Boolean,
    editing: Boolean,
    editingValue: TextFieldValue,
    onEditingValueChange: (TextFieldValue) -> Unit,
    viewModel: EditorViewModel,
    onReceiveImage: (android.net.Uri) -> Unit,
) {
    val density = LocalDensity.current
    val widthDp = with(density) { (element as? ImageElement)?.width?.toDp() ?: (element as TextElement).width.toDp() }

    var modifier = Modifier
        .graphicsLayer {
            translationX = element.x * canvasState.scale + canvasState.offset.x
            translationY = element.y * canvasState.scale + canvasState.offset.y
            scaleX = canvasState.scale
            scaleY = canvasState.scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
        .width(widthDp)

    if (interactive && !editing) {
        modifier = modifier
            .pointerInput(element.id) {
                detectTapGestures {
                    if (selected && element is TextElement) {
                        viewModel.editingTextId.value = element.id
                    } else if (element is TextElement &&
                        viewModel.tool.value == EditorTool.TEXT
                    ) {
                        viewModel.selectedElementId.value = element.id
                        viewModel.editingTextId.value = element.id
                    } else {
                        viewModel.selectedElementId.value = element.id
                    }
                }
            }
            .pointerInput(element.id) {
                detectDragGestures(
                    onDragStart = {
                        viewModel.beginGesture()
                        viewModel.selectedElementId.value = element.id
                    },
                ) { change, amount ->
                    change.consume()
                    viewModel.updateElement(element.id, live = true) {
                        it.movedBy(
                            amount.x / canvasState.scale,
                            amount.y / canvasState.scale,
                        )
                    }
                }
            }
    }

    Box(modifier) {
        when (element) {
            is ImageElement -> ImageElementContent(element, viewModel)
            is TextElement -> TextElementContent(
                element = element,
                editing = editing,
                editingValue = editingValue,
                onEditingValueChange = onEditingValueChange,
                viewModel = viewModel,
                onReceiveImage = onReceiveImage,
            )
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
    Box(
        Modifier
            .width(with(density) { element.width.toDp() })
            .height(with(density) { element.height.toDp() })
            .background(
                if (element.isPdfPage) Color.White else Color.Transparent,
            ),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TextElementContent(
    element: TextElement,
    editing: Boolean,
    editingValue: TextFieldValue,
    onEditingValueChange: (TextFieldValue) -> Unit,
    viewModel: EditorViewModel,
    onReceiveImage: (android.net.Uri) -> Unit,
) {
    val settings by viewModel.settingsStore.settings.collectAsState()
    val fonts by viewModel.fontManager.fonts.collectAsState()
    val fontFamily = remember(element.fontId, fonts) {
        viewModel.fontManager.byId(element.fontId).family
    }
    val color = element.color?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val textStyle = baseTextStyle(settings.styleSet, element.styleId, fontFamily, color)

    if (editing) {
        val focusRequester = remember { FocusRequester() }
        BasicTextField(
            value = editingValue,
            onValueChange = onEditingValueChange,
            textStyle = textStyle,
            visualTransformation = MarkdownVisualTransformation(settings.styleSet, color),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .focusRequester(focusRequester)
                .contentReceiver(
                    ReceiveContentListener { transferable ->
                        if (!transferable.hasMediaType(MediaType.Image)) {
                            return@ReceiveContentListener transferable
                        }
                        // Samsung Keyboard stickers / pasted or dropped images.
                        transferable.consume { item ->
                            item.uri?.also(onReceiveImage) != null
                        }
                    },
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                )
                .padding(4.dp),
        )
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    } else {
        Text(
            text = styleMarkdown(element.text.ifEmpty { "Scrivi…" }, settings.styleSet, color),
            style = textStyle,
            color = if (element.text.isEmpty()) color.copy(alpha = 0.4f) else Color.Unspecified,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.SelectionChrome(
    element: NoteElement,
    canvasState: CanvasState,
    viewModel: EditorViewModel,
) {
    Box(
        Modifier
            .matchParentSize()
            .border(2.dp, MaterialTheme.colorScheme.primary),
    )
    // Delete button.
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 16.dp, y = (-16).dp)
            .size(32.dp),
    ) {
        IconButton(onClick = { viewModel.deleteElement(element.id) }) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Elimina elemento",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    // Resize handle.
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 14.dp, y = 14.dp)
            .size(28.dp)
            .pointerInput(element.id) {
                detectDragGestures(
                    onDragStart = { viewModel.beginGesture() },
                ) { change, amount ->
                    change.consume()
                    val dx = amount.x / canvasState.scale
                    val dy = amount.y / canvasState.scale
                    viewModel.updateElement(element.id, live = true) { e ->
                        when (e) {
                            is TextElement ->
                                e.copy(width = (e.width + dx).coerceAtLeast(120f))
                            is ImageElement -> {
                                val ratio = e.height / e.width
                                val newW = (e.width + dx).coerceAtLeast(80f)
                                e.copy(width = newW, height = newW * ratio)
                            }
                        }
                    }
                }
            },
    ) {
        Icon(
            Icons.Outlined.OpenInFull,
            contentDescription = "Ridimensiona",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(6.dp),
        )
    }
}

// ---- Bars ----

@OptIn(ExperimentalMaterial3Api::class)
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
    background: CanvasBackground,
    onBackgroundChange: (CanvasBackground) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Indietro")
            }
        },
        title = {
            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
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
        },
        actions = {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Annulla")
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "Ripeti")
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Altro")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Aggiungi immagine") },
                    leadingIcon = { Icon(Icons.Outlined.Image, null) },
                    onClick = {
                        menuOpen = false
                        onAddImage()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Importa PDF") },
                    leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) },
                    onClick = {
                        menuOpen = false
                        onAddPdf()
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
                                },
                            )
                        },
                        leadingIcon = {
                            if (background == bg) Icon(Icons.Outlined.Check, null)
                        },
                        onClick = {
                            menuOpen = false
                            onBackgroundChange(bg)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun EditorToolBar(
    tool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    currentColor: Long,
    onOpenWheel: (Offset) -> Unit,
) {
    Surface(tonalElevation = 4.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(Icons.Outlined.BackHand, "Selezione", tool == EditorTool.SELECT) {
                onToolSelected(EditorTool.SELECT)
            }
            ToolButton(Icons.Outlined.Draw, "Penna", tool == EditorTool.PEN) {
                onToolSelected(EditorTool.PEN)
            }
            ToolButton(Icons.Outlined.Highlight, "Evidenziatore", tool == EditorTool.HIGHLIGHTER) {
                onToolSelected(EditorTool.HIGHLIGHTER)
            }
            ToolButton(Icons.Outlined.AutoFixNormal, "Gomma", tool == EditorTool.ERASER) {
                onToolSelected(EditorTool.ERASER)
            }
            ToolButton(Icons.Outlined.TextFields, "Testo", tool == EditorTool.TEXT) {
                onToolSelected(EditorTool.TEXT)
            }
            Spacer(Modifier.weight(1f))
            var buttonCenter by remember { mutableStateOf(Offset.Zero) }
            Box(
                Modifier
                    .onGloballyPositioned {
                        val pos = it.positionInWindow()
                        buttonCenter = Offset(
                            pos.x + it.size.width / 2f,
                            pos.y - 240f,
                        )
                    }
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(currentColor))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures { onOpenWheel(buttonCenter) }
                    },
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color =
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                icon,
                contentDescription = label,
                tint =
                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TextFormatBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontManager: FontManager,
    currentFontId: String?,
    onFontSelected: (String) -> Unit,
    onDone: () -> Unit,
) {
    val fonts by fontManager.fonts.collectAsState()
    var fontMenuOpen by remember { mutableStateOf(false) }
    var styleMenuOpen by remember { mutableStateOf(false) }

    Surface(tonalElevation = 4.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IconButton(onClick = { styleMenuOpen = true }) {
                    Text("T", style = MaterialTheme.typography.titleMedium)
                }
                DropdownMenu(
                    expanded = styleMenuOpen,
                    onDismissRequest = { styleMenuOpen = false },
                ) {
                    listOf(
                        "Titolo 1" to "# ",
                        "Titolo 2" to "## ",
                        "Titolo 3" to "### ",
                        "Corpo" to "",
                    ).forEach { (label, prefix) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                styleMenuOpen = false
                                onValueChange(
                                    if (prefix.isEmpty()) value.toggleLinePrefix(" ")
                                    else value.toggleLinePrefix(prefix),
                                )
                            },
                        )
                    }
                }
            }
            IconButton(onClick = { onValueChange(value.wrapSelection("**")) }) {
                Icon(Icons.Outlined.FormatBold, contentDescription = "Grassetto")
            }
            IconButton(onClick = { onValueChange(value.wrapSelection("*")) }) {
                Icon(Icons.Outlined.FormatItalic, contentDescription = "Corsivo")
            }
            IconButton(onClick = { onValueChange(value.wrapSelection("~~")) }) {
                Icon(Icons.Outlined.FormatStrikethrough, contentDescription = "Barrato")
            }
            IconButton(onClick = { onValueChange(value.wrapSelection("`")) }) {
                Icon(Icons.Outlined.Code, contentDescription = "Codice")
            }
            IconButton(onClick = { onValueChange(value.toggleLinePrefix("- ")) }) {
                Icon(Icons.Outlined.FormatListBulleted, contentDescription = "Elenco")
            }
            IconButton(onClick = { onValueChange(value.toggleLinePrefix("- [ ] ")) }) {
                Icon(Icons.Outlined.Checklist, contentDescription = "Checklist")
            }
            IconButton(onClick = { onValueChange(value.toggleLinePrefix("> ")) }) {
                Icon(Icons.Outlined.FormatQuote, contentDescription = "Citazione")
            }
            Box {
                IconButton(onClick = { fontMenuOpen = true }) {
                    Icon(Icons.Outlined.TextFields, contentDescription = "Font")
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
                                    if (font.id == currentFontId) {
                                        androidx.compose.ui.text.font.FontWeight.Bold
                                    } else null,
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
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDone) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "Fine",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
