package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Highlighter
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Spline
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.X
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Palette shown on the outer ring of the radial menu. */
val InkPalette: List<Long> = listOf(
    0xFF1A1A1A, 0xFF6B7280, 0xFFEF4444, 0xFFF97316,
    0xFFEAB308, 0xFF22C55E, 0xFF0EA5E9, 0xFF3B82F6,
    0xFF8B5CF6, 0xFFEC4899, 0xFF92400E, 0xFFFFFFFF,
)

private data class RadialTool(
    val tool: EditorTool,
    val icon: ImageVector,
    val label: String,
)

/**
 * Samsung Notes-style quick wheel: inner ring with tools, outer ring with
 * colors and a width slider underneath. Opened with the S Pen barrel button.
 */
@Composable
fun RadialMenu(
    center: Offset,
    currentTool: EditorTool,
    currentColor: Long,
    currentWidth: Float,
    paletteColors: List<Long> = InkPalette,
    onToolSelected: (EditorTool) -> Unit,
    onColorSelected: (Long) -> Unit,
    onWidthSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val tools = remember {
        listOf(
            RadialTool(EditorTool.PEN, Lucide.Pen, "Penna"),
            RadialTool(EditorTool.HIGHLIGHTER, Lucide.Highlighter, "Evidenz."),
            RadialTool(EditorTool.ERASER, Lucide.Eraser, "Gomma"),
            RadialTool(EditorTool.TEXT, Lucide.Type, "Testo"),
            RadialTool(EditorTool.CONNECT, Lucide.Spline, "Collega"),
            RadialTool(EditorTool.SELECT, Lucide.Hand, "Selezione"),
        )
    }
    val density = LocalDensity.current
    val appear by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "radialAppear",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        val innerRadius = 64.dp
        val outerRadius = 116.dp

        Box(
            modifier = Modifier
                .offset { IntOffset(center.x.roundToInt(), center.y.roundToInt()) }
                .graphicsLayer {
                    scaleX = appear
                    scaleY = appear
                    alpha = appear.coerceIn(0f, 1f)
                },
        ) {
            // Center: close button.
            RoundButton(
                modifier = Modifier.align(Alignment.Center).offset((-24).dp, (-24).dp),
                size = 48.dp,
                background = MaterialTheme.colorScheme.primary,
                onClick = onDismiss,
            ) {
                Icon(Lucide.X, "Chiudi", tint = Color.White)
            }

            // Inner ring: tools.
            tools.forEachIndexed { index, item ->
                val angle = -90.0 + index * (360.0 / tools.size)
                val x = with(density) { innerRadius.toPx() } * cos(Math.toRadians(angle)).toFloat()
                val y = with(density) { innerRadius.toPx() } * sin(Math.toRadians(angle)).toFloat()
                val selected = currentTool == item.tool
                RoundButton(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset((x - 24.dp.toPx()).roundToInt(), (y - 24.dp.toPx()).roundToInt()) },
                    size = 48.dp,
                    background =
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    onClick = {
                        onToolSelected(item.tool)
                        onDismiss()
                    },
                ) {
                    Icon(
                        item.icon,
                        item.label,
                        tint =
                        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Outer ring: colors.
            paletteColors.forEachIndexed { index, colorValue ->
                val angle = -90.0 + index * (360.0 / paletteColors.size)
                val x = with(density) { outerRadius.toPx() } * cos(Math.toRadians(angle)).toFloat()
                val y = with(density) { outerRadius.toPx() } * sin(Math.toRadians(angle)).toFloat()
                val selected = currentColor == colorValue
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset((x - 16.dp.toPx()).roundToInt(), (y - 16.dp.toPx()).roundToInt()) }
                        .size(32.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(colorValue))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color =
                            if (selected) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.15f),
                            shape = CircleShape,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            onColorSelected(colorValue)
                            onDismiss()
                        },
                )
            }

            // Width slider below the wheel.
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = outerRadius + 56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
            ) {
                Box(
                    Modifier
                        .size(width = 220.dp, height = 56.dp)
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.Center,
                ) {
                    Slider(
                        value = currentWidth,
                        onValueChange = onWidthSelected,
                        valueRange = 1f..40f,
                        modifier = Modifier.size(width = 180.dp, height = 40.dp),
                    )
                    Text(
                        "${currentWidth.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.TopEnd).offset((-10).dp, 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundButton(
    modifier: Modifier,
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
