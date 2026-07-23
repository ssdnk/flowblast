package com.flowerblast.game.ui.game

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.flowerblast.game.model.Flower
import com.flowerblast.game.ui.theme.CellEmpty
import com.flowerblast.game.ui.theme.CellBorder
import com.flowerblast.game.ui.theme.Primary
import kotlin.math.cos
import kotlin.math.sin

// Particle directions in radians (6 evenly spaced)
private val PARTICLE_ANGLES = Array(6) { i -> (i * 60f) * (Math.PI / 180f).toFloat() }

@Composable
fun GameBoard(
    board: Array<Array<Flower?>>,
    clearedRows: Set<Int>,
    clearedCols: Set<Int>,
    previewCells: List<Pair<Int, Int>>?,
    previewFlower: Flower?,
    isValidPreview: Boolean,
    modifier: Modifier = Modifier
) {
    val gap = 3f
    val cornerRadius = 6f

    // Drive a 0→1 animation whenever new lines are cleared
    val clearAnim = remember { Animatable(1f) }
    LaunchedEffect(clearedRows, clearedCols) {
        if (clearedRows.isNotEmpty() || clearedCols.isNotEmpty()) {
            clearAnim.snapTo(0f)
            clearAnim.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
    }
    val animT = clearAnim.value  // 0 = just cleared, 1 = animation done

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        val totalWidth = size.width
        val cellSize = (totalWidth - 7 * gap) / 8f

        for (row in 0..7) {
            for (col in 0..7) {
                val x = col * (cellSize + gap)
                val y = row * (cellSize + gap)
                val isCleared = row in clearedRows || col in clearedCols

                val bgColor = if (isCleared) {
                    // Cells flash bright then fade transparent
                    Primary.copy(alpha = (1f - animT).coerceIn(0f, 1f))
                } else {
                    board[row][col]?.blockColor ?: CellEmpty
                }
                val borderColor = if (isCleared) {
                    Primary.copy(alpha = (1f - animT).coerceIn(0f, 1f))
                } else {
                    board[row][col]?.borderColor ?: CellBorder
                }

                drawRoundRect(color = bgColor, topLeft = Offset(x, y), size = Size(cellSize, cellSize), cornerRadius = CornerRadius(cornerRadius))
                drawRoundRect(color = borderColor, topLeft = Offset(x, y), size = Size(cellSize, cellSize), cornerRadius = CornerRadius(cornerRadius), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))

                if (!isCleared) {
                    board[row][col]?.let { flower ->
                        drawCellEmoji(flower.emoji, x + cellSize / 2f, y + cellSize / 2f, cellSize * 0.7f)
                    }
                }
            }
        }

        // Sparkle particles for cleared cells
        if (animT < 1f && (clearedRows.isNotEmpty() || clearedCols.isNotEmpty())) {
            val particleAlpha = (1f - animT * animT).coerceIn(0f, 1f)  // fast fade
            val maxTravel = cellSize * 0.9f
            val particleRadius = 3f + animT * 4f

            for (row in clearedRows) {
                for (col in 0..7) {
                    drawSparkles(
                        cx = col * (cellSize + gap) + cellSize / 2f,
                        cy = row * (cellSize + gap) + cellSize / 2f,
                        t = animT,
                        maxTravel = maxTravel,
                        radius = particleRadius,
                        alpha = particleAlpha,
                        color = Color.White
                    )
                }
            }
            for (col in clearedCols) {
                for (row in 0..7) {
                    if (row !in clearedRows) {  // already drawn by row loop
                        drawSparkles(
                            cx = col * (cellSize + gap) + cellSize / 2f,
                            cy = row * (cellSize + gap) + cellSize / 2f,
                            t = animT,
                            maxTravel = maxTravel,
                            radius = particleRadius,
                            alpha = particleAlpha,
                            color = Color.White
                        )
                    }
                }
            }
        }

        previewCells?.let { cells ->
            if (cells.isNotEmpty() && previewFlower != null) {
                val pColor = if (isValidPreview) previewFlower.blockColor.copy(alpha = 0.65f) else Color.Red.copy(alpha = 0.5f)
                for ((r, c) in cells) {
                    val px = c * (cellSize + gap)
                    val py = r * (cellSize + gap)
                    drawRoundRect(color = pColor, topLeft = Offset(px, py), size = Size(cellSize, cellSize), cornerRadius = CornerRadius(cornerRadius))
                }
            }
        }
    }
}

private fun DrawScope.drawSparkles(
    cx: Float, cy: Float,
    t: Float, maxTravel: Float,
    radius: Float, alpha: Float,
    color: Color
) {
    for (angle in PARTICLE_ANGLES) {
        val dist = maxTravel * t
        val px = cx + cos(angle) * dist
        val py = cy + sin(angle) * dist
        drawCircle(color = color.copy(alpha = alpha), radius = radius, center = Offset(px, py))
    }
}

private fun DrawScope.drawCellEmoji(emoji: String, cx: Float, cy: Float, size: Float) {
    drawContext.canvas.nativeCanvas.drawText(emoji, cx, cy + size * 0.3f, Paint().apply {
        textSize = size
        textAlign = Paint.Align.CENTER
    })
}
