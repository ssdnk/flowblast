package com.flowerblast.game.ui.game

import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.flowerblast.game.model.Piece
import com.flowerblast.game.ui.theme.CellBorder
import com.flowerblast.game.ui.theme.SurfaceVariant

@Composable
fun PieceTray(
    pieces: List<Piece?>,
    draggingPieceIndex: Int?,
    onRotatePiece: (Int) -> Unit,
    onStartDrag: (Int, Offset) -> Unit,
    onEndDrag: () -> Unit,
    onDragMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Следующие фигуры:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            color = SurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CellBorder)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pieces.forEachIndexed { index, piece ->
                    PieceSlot(
                        piece = piece,
                        isBeingDragged = index == draggingPieceIndex,
                        onTap = { if (piece != null) onRotatePiece(index) },
                        onDragStart = { absPos -> if (piece != null) onStartDrag(index, absPos) },
                        onDragMove = onDragMove,
                        onDragEnd = onEndDrag
                    )
                }
            }
        }
    }
}

@Composable
private fun PieceSlot(
    piece: Piece?,
    isBeingDragged: Boolean,
    onTap: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    var rootPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(80.dp)
            .onGloballyPositioned { rootPos = it.positionInRoot() }
            .pointerInput(piece != null) {
                if (piece == null) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downRootPos = rootPos + down.position

                    val touchSlop = viewConfiguration.touchSlop
                    var dragStarted = false
                    var currentPos = down.position

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            if (!dragStarted) onTap()
                            break
                        }

                        val dist = (change.position - down.position).getDistance()
                        if (!dragStarted && dist > touchSlop) {
                            dragStarted = true
                            change.consume()
                            onDragStart(downRootPos)
                        }

                        if (dragStarted) {
                            val delta = change.position - currentPos
                            change.consume()
                            onDragMove(delta.x, delta.y)
                        }
                        currentPos = change.position
                    }

                    if (dragStarted) {
                        onDragEnd()
                    }
                }
            }
    ) {
        if (piece != null) {
            // Ghost alpha when this piece is currently being dragged
            val drawAlpha = if (isBeingDragged) 0.2f else 1f
            val cellSizeDp = 28.dp
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = cellSizeDp.toPx()
                val startX = (size.width - piece.width * cellSize) / 2
                val startY = (size.height - piece.height * cellSize) / 2

                for (row in piece.shape.indices) {
                    for (col in piece.shape[row].indices) {
                        if (piece.shape[row][col]) {
                            val x = startX + col * cellSize
                            val y = startY + row * cellSize
                            drawRoundRect(
                                color = piece.flower.blockColor.copy(alpha = drawAlpha),
                                topLeft = Offset(x, y),
                                size = Size(cellSize - 1, cellSize - 1),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )
                            drawRoundRect(
                                color = piece.flower.borderColor.copy(alpha = drawAlpha),
                                topLeft = Offset(x, y),
                                size = Size(cellSize - 1, cellSize - 1),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                                style = Stroke(width = 1.5f.dp.toPx())
                            )
                        }
                    }
                }
                for (row in piece.shape.indices) {
                    for (col in piece.shape[row].indices) {
                        if (piece.shape[row][col]) {
                            val x = startX + col * cellSize + cellSize / 2
                            val y = startY + row * cellSize + cellSize / 2
                            val paint = Paint().apply {
                                textSize = cellSize * 0.55f
                                textAlign = Paint.Align.CENTER
                                alpha = (drawAlpha * 255).toInt()
                            }
                            drawContext.canvas.nativeCanvas.drawText(piece.flower.emoji, x, y + cellSize * 0.2f, paint)
                        }
                    }
                }
            }
        }
    }
}
