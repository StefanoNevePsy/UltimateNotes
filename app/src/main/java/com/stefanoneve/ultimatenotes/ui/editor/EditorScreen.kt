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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Bold
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Code
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Highlighter
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Italic
import com.composables.icons.lucide.List
import com.composables.icons.lucide.ListChecks
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MoveDiagonal
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.Spline
import com.composables.icons.lucide.Strikethrough
import com.composables.icons.lucide.TextQuote
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.Undo2
import com.composables.icons.lucide.Zap
import com.stefanoneve.ultimatenotes.data.fonts.FontManager
import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.CapStyle
import com.stefanoneve.ultimatenotes.data.model.ConnectorElement
import com.stefanoneve.ultimatenotes.data.model.ImageElement
import com.stefanoneve.ultimatenotes.data.model.InkStroke
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import com.stefanoneve.ultimatenotes.data.model.NoteElement
import com.stefanoneve.ultimatenotes.data.model.StrokePoint
import com.stefanoneve.ultimatenotes.data.model.TextElement
import com.stefanoneve.ultimatenotes.ui.components.glass
import com.stefanoneve.ultimatenotes.ui.theme.LocalAppStyle
import com.stefanoneve.ultimatenotes.util.SPenEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private fun NoteElement.movedBy(dx: Float, dy: Float): NoteElement = when (this) {
    is TextElement -> copy(x = x + dx, y = y + dy)
    is ImageElement -> copy(x = x + dx, y = y + dy)
}

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
    val selectedConnectorId by viewModel.selectedConnectorId.collectAsState()
    val pendingConnectFrom by viewModel.pendingConnectFrom.collectAsState()
    val editingTextId by viewModel.editingTextId.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val settings by viewModel.settingsStore.settings.collectAsState()

    val canvasState = remember { CanvasState() }
    val activeStroke = remember { mutableStateOf<InkStroke?>(null) }
    var radialCenter by remember { mutableStateOf<Offset?>(null) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }

    // Marching dashes for animated connectors.
    val anyAnimatedConnector = content.connectors.any { it.animated }
    var dashPhase = 0f
    if (anyAnimatedConnector) {
        val transition = rememberInfiniteTransition(label = "dash")
        dashPhase = transition.animateFloat(
            initialValue = 0f,
            targetValue = 64f,
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
                                    val hit = hitTestConnector(
                                        viewModel.content.value,
                                        viewModel.elementSizes,
                                        world,
                                        tolerance = 28f / canvasState.scale,
                                    )
                                    viewModel.selectedConnectorId.value = hit
                                    viewModel.selectedElementId.value = null
                                }
                            }
                        },
                        activeStroke = activeStroke,
                    ),
            )

            ConnectorLayer(
                content = content,
                canvasState = canvasState,
                selectedConnectorId = selectedConnectorId,
                elementSizes = viewModel.elementSizes,
                dashPhase = dashPhase,
                modifier = Modifier.fillMaxSize(),
            )

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
                        pendingConnect = pendingConnectFrom == element.id,
                        editing = editingTextId == element.id,
                        editingValue = editingValue,
                        onEditingValueChange = ::applyEditingValue,
                        viewModel = viewModel,
                        onReceiveImage = { uri ->
                            viewModel.importImage(uri, element.x, element.y + 80f)
                        },
                    )
                }
            }

            // Bezier handle of the selected connector.
            val selectedConnector = content.connectors
                .firstOrNull { it.id == selectedConnectorId }
            if (selectedConnector != null) {
                ConnectorHandle(
                    connector = selectedConnector,
                    content = content,
                    canvasState = canvasState,
                    viewModel = viewModel,
                )
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
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            when {
                editingTextId != null -> TextFormatBar(
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
                selectedConnector != null -> ConnectorStyleBar(
                    connector = selectedConnector,
                    onUpdate = { transform ->
                        viewModel.updateConnector(selectedConnector.id, transform = transform)
                    },
                    onDelete = { viewModel.deleteConnector(selectedConnector.id) },
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
    pendingConnect: Boolean,
    editing: Boolean,
    editingValue: TextFieldValue,
    onEditingValueChange: (TextFieldValue) -> Unit,
    viewModel: EditorViewModel,
    onReceiveImage: (android.net.Uri) -> Unit,
) {
    val density = LocalDensity.current
    val elementWidth = when (element) {
        is ImageElement -> element.width
        is TextElement -> element.width
    }
    val widthDp = with(density) { elementWidth.toDp() }

    var modifier = Modifier
        .graphicsLayer {
            translationX = element.x * canvasState.scale + canvasState.offset.x
            translationY = element.y * canvasState.scale + canvasState.offset.y
            scaleX = canvasState.scale
            scaleY = canvasState.scale
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
                            viewModel.selectedElementId.value = element.id
                            viewModel.selectedConnectorId.value = null
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
                    viewModel.updateElement(element.id, live = true) {
                        it.movedBy(
                            amount.x / canvasState.scale,
                            amount.y / canvasState.scale,
                        )
                    }
                }
            }
    }

    // Pulsing accent border while this element waits for its connection pair.
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
            cursorBrush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .focusRequester(focusRequester)
                .contentReceiver(
                    ReceiveContentListener { transferable ->
                        if (!transferable.hasMediaType(MediaType.Image)) {
                            return@ReceiveContentListener transferable
                        }
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
                    val dx = amount.x / canvasState.scale
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
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Lucide.MoveDiagonal,
            contentDescription = "Ridimensiona",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ---- Connector handle ----

@Composable
private fun ConnectorHandle(
    connector: ConnectorElement,
    content: com.stefanoneve.ultimatenotes.data.model.NoteContent,
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
            .offset { IntOffset((screen.x - 14.dp.toPx()).roundToInt(), (screen.y - 14.dp.toPx()).roundToInt()) }
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
            cursorBrush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.primary,
            ),
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
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Lucide.Plus, contentDescription = "Aggiungi")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Aggiungi immagine") },
                    leadingIcon = { Icon(Lucide.Image, null) },
                    onClick = {
                        menuOpen = false
                        onAddImage()
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
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
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
        listOf(0xFF9A8FE5, 0xFFEF4444, 0xFFF59E0B, 0xFF22C55E, 0xFF0EA5E9, 0xFF6B7280)
            .forEach { c ->
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (connector.color == c) 3.dp else 1.dp,
                            color =
                            if (connector.color == c) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.15f),
                            shape = CircleShape,
                        )
                        .pointerInput(c) {
                            detectTapGestures { onUpdate { it.copy(color = c) } }
                        },
                )
            }
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

private fun CapStyle.next(): CapStyle = when (this) {
    CapStyle.NONE -> CapStyle.ARROW
    CapStyle.ARROW -> CapStyle.DOT
    CapStyle.DOT -> CapStyle.NONE
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
                    "Corpo" to "",
                ).forEach { (label, prefix) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            styleMenuOpen = false
                            onValueChange(
                                if (prefix.isEmpty()) value.toggleLinePrefix(" ")
                                else value.toggleLinePrefix(prefix),
                            )
                        },
                    )
                }
            }
        }
        IconButton(onClick = { onValueChange(value.wrapSelection("**")) }) {
            Icon(Lucide.Bold, contentDescription = "Grassetto")
        }
        IconButton(onClick = { onValueChange(value.wrapSelection("*")) }) {
            Icon(Lucide.Italic, contentDescription = "Corsivo")
        }
        IconButton(onClick = { onValueChange(value.wrapSelection("~~")) }) {
            Icon(Lucide.Strikethrough, contentDescription = "Barrato")
        }
        IconButton(onClick = { onValueChange(value.wrapSelection("`")) }) {
            Icon(Lucide.Code, contentDescription = "Codice")
        }
        IconButton(onClick = { onValueChange(value.toggleLinePrefix("- ")) }) {
            Icon(Lucide.List, contentDescription = "Elenco")
        }
        IconButton(onClick = { onValueChange(value.toggleLinePrefix("- [ ] ")) }) {
            Icon(Lucide.ListChecks, contentDescription = "Checklist")
        }
        IconButton(onClick = { onValueChange(value.toggleLinePrefix("> ")) }) {
            Icon(Lucide.TextQuote, contentDescription = "Citazione")
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
