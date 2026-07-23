package com.flowerblast.game.ui.game

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowerblast.game.logic.BoardLogic
import com.flowerblast.game.model.Flower
import com.flowerblast.game.model.GamePhase
import com.flowerblast.game.model.Piece
import com.flowerblast.game.ui.theme.Primary
import kotlinx.coroutines.launch

private const val BOARD_GAP = 3f

/** Anchor is center-bottom of the piece; search up to ±2 cells for nearest valid placement. */
private fun findNearestValidPlacement(
    piece: Piece, board: Array<Array<Flower?>>, initRow: Int, initCol: Int
): Pair<Int, Int>? {
    val maxRow = (8 - piece.height).coerceAtLeast(0)
    val maxCol = (8 - piece.width).coerceAtLeast(0)
    var best: Pair<Int, Int>? = null
    var bestDistSq = Int.MAX_VALUE
    for (dr in -2..2) {
        for (dc in -2..2) {
            val r = (initRow + dr).coerceIn(0, maxRow)
            val c = (initCol + dc).coerceIn(0, maxCol)
            if (BoardLogic.canPlace(piece, board, r, c)) {
                val d = dr * dr + dc * dc
                if (d < bestDistSq) { bestDistSq = d; best = r to c }
            }
        }
    }
    return best
}

private data class ScoreEvent(val delta: Int, val lineCount: Int, val isRainbow: Boolean, val id: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onNavigateToMenu: () -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val boardPadPx = with(density) { 4.dp.toPx() }

    var boxRootOffset by remember { mutableStateOf(Offset.Zero) }
    var boardOffsetX by remember { mutableFloatStateOf(0f) }
    var boardOffsetY by remember { mutableFloatStateOf(0f) }
    var boardWidth by remember { mutableFloatStateOf(0f) }

    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var localDraggingIndex by remember { mutableStateOf<Int?>(null) }

    // Score popup
    var prevScore by remember { mutableIntStateOf(uiState.score) }
    var scoreEventCounter by remember { mutableIntStateOf(0) }
    var currentScoreEvent by remember { mutableStateOf<ScoreEvent?>(null) }

    LaunchedEffect(uiState.clearedRows, uiState.clearedCols) {
        val lineCount = uiState.clearedRows.size + uiState.clearedCols.size
        val delta = uiState.score - prevScore
        prevScore = uiState.score
        if (lineCount > 0 && delta > 0) {
            currentScoreEvent = ScoreEvent(delta, lineCount, uiState.isRainbowBonus, scoreEventCounter++)
        }
    }

    // Board canvas geometry (accounts for 4dp canvas padding and 3px gaps)
    val boardCanvasWidth = boardWidth - 2 * boardPadPx
    val boardCellSize = if (boardCanvasWidth > 0f) (boardCanvasWidth - 7 * BOARD_GAP) / 8f else 40f
    val boardCanvasLeft = boardOffsetX + boardPadPx
    val boardCanvasTop = boardOffsetY + boardPadPx

    val draggingPiece = if (isDragging) localDraggingIndex?.let { uiState.trayPieces.getOrNull(it) } else null
    val pieceH = draggingPiece?.height ?: 1
    val pieceW = draggingPiece?.width ?: 1

    val overBoard = boardCanvasWidth > 0f &&
        dragX >= boardCanvasLeft && dragX < boardCanvasLeft + boardCanvasWidth &&
        dragY >= boardCanvasTop && dragY < boardCanvasTop + boardCanvasWidth

    // Center-bottom anchor: piece's bottom row aligns with finger row → piece is above finger
    val fingerRow = if (overBoard) ((dragY - boardCanvasTop) / (boardCellSize + BOARD_GAP)).toInt().coerceIn(0, 7) else 0
    val fingerCol = if (overBoard) ((dragX - boardCanvasLeft) / (boardCellSize + BOARD_GAP)).toInt().coerceIn(0, 7) else 0
    val initRow = (fingerRow - pieceH + 1).coerceIn(0, (8 - pieceH).coerceAtLeast(0))
    val initCol = (fingerCol - pieceW / 2).coerceIn(0, (8 - pieceW).coerceAtLeast(0))

    val snapResult = if (overBoard && draggingPiece != null)
        findNearestValidPlacement(draggingPiece, uiState.board, initRow, initCol) else null
    val snapRow = if (overBoard) (snapResult?.first ?: initRow) else 0
    val snapCol = if (overBoard) (snapResult?.second ?: initCol) else 0
    val snapIsValid = snapResult != null && overBoard

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FlowerBlast", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToMenu) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onGloballyPositioned { boxRootOffset = it.positionInRoot() }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                ScoreHeader(
                    score = uiState.score,
                    highScore = uiState.highScore,
                    isNewHighScore = uiState.score >= uiState.highScore && uiState.score > 0
                )

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        boardOffsetX = pos.x
                        boardOffsetY = pos.y
                        boardWidth = coords.size.width.toFloat()
                    }
                ) {
                    val previewCells = if (draggingPiece != null && overBoard) draggingPiece.getCells(snapRow, snapCol) else null

                    GameBoard(
                        board = uiState.board,
                        clearedRows = uiState.clearedRows,
                        clearedCols = uiState.clearedCols,
                        previewCells = previewCells,
                        previewFlower = draggingPiece?.flower,
                        isValidPreview = snapIsValid
                    )

                    // Score popup floats over the board
                    currentScoreEvent?.let { event ->
                        key(event.id) {
                            ScorePopup(
                                delta = event.delta,
                                lineCount = event.lineCount,
                                isRainbow = event.isRainbow,
                                onDismiss = { if (currentScoreEvent?.id == event.id) currentScoreEvent = null }
                            )
                        }
                    }
                }

                PieceTray(
                    pieces = uiState.trayPieces,
                    draggingPieceIndex = localDraggingIndex,
                    onRotatePiece = { viewModel.rotatePiece(it) },
                    onStartDrag = { index, absPos ->
                        isDragging = true
                        dragX = absPos.x
                        dragY = absPos.y
                        localDraggingIndex = index
                        viewModel.startDrag(index)
                    },
                    onEndDrag = {
                        val pieceIdx = localDraggingIndex
                        if (isDragging && pieceIdx != null) {
                            // Recompute from delegated state — always current
                            val cLeft = boardOffsetX + boardPadPx
                            val cTop = boardOffsetY + boardPadPx
                            val cWidth = boardWidth - 2 * boardPadPx
                            val cs = if (cWidth > 0f) (cWidth - 7 * BOARD_GAP) / 8f else 40f
                            val overB = cWidth > 0f &&
                                dragX >= cLeft && dragX < cLeft + cWidth &&
                                dragY >= cTop && dragY < cTop + cWidth
                            if (overB) {
                                val piece = uiState.trayPieces.getOrNull(pieceIdx)
                                if (piece != null) {
                                    val fRow = ((dragY - cTop) / (cs + BOARD_GAP)).toInt().coerceIn(0, 7)
                                    val fCol = ((dragX - cLeft) / (cs + BOARD_GAP)).toInt().coerceIn(0, 7)
                                    val iRow = (fRow - piece.height + 1).coerceIn(0, (8 - piece.height).coerceAtLeast(0))
                                    val iCol = (fCol - piece.width / 2).coerceIn(0, (8 - piece.width).coerceAtLeast(0))
                                    val result = findNearestValidPlacement(piece, uiState.board, iRow, iCol)
                                    if (result != null) viewModel.placePiece(pieceIdx, result.first, result.second)
                                }
                            }
                        }
                        isDragging = false
                        localDraggingIndex = null
                        viewModel.endDrag()
                    },
                    onDragMove = { dx, dy -> dragX += dx; dragY += dy }
                )
            }

            // Ghost piece overlay — snaps to board grid and floats above finger
            if (isDragging && draggingPiece != null) {
                val pieceLeft: Float
                val pieceTop: Float
                if (overBoard) {
                    pieceLeft = boardCanvasLeft - boxRootOffset.x + snapCol * (boardCellSize + BOARD_GAP)
                    pieceTop = boardCanvasTop - boxRootOffset.y + snapRow * (boardCellSize + BOARD_GAP)
                } else {
                    pieceLeft = dragX - boxRootOffset.x - pieceW * boardCellSize / 2f
                    pieceTop = dragY - boxRootOffset.y - pieceH * boardCellSize / 2f
                }
                DraggingPieceOverlay(
                    piece = draggingPiece,
                    cellSize = boardCellSize,
                    pieceLeft = pieceLeft,
                    pieceTop = pieceTop
                )
            }

            if (uiState.phase == GamePhase.GameOver) {
                GameOverlay(
                    score = uiState.score,
                    highScore = uiState.highScore,
                    onPlayAgain = { viewModel.startNewGame() },
                    onGoToMenu = onNavigateToMenu
                )
            }
        }
    }
}

@Composable
private fun ScorePopup(delta: Int, lineCount: Int, isRainbow: Boolean, onDismiss: () -> Unit) {
    val yOff = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { yOff.animateTo(-80f, tween(1100, easing = LinearEasing)) }
        kotlinx.coroutines.delay(680)
        alpha.animateTo(0f, tween(420, easing = LinearEasing))
        onDismiss()
    }

    val label: String? = when {
        isRainbow -> "🌈 RAINBOW!"
        lineCount >= 4 -> "MEGA!"
        lineCount == 3 -> "TRIPLE!"
        lineCount == 2 -> "DOUBLE!"
        else -> null
    }

    // Non-interactive overlay fills the board so text centers automatically
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = yOff.value; this.alpha = alpha.value },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+$delta",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD700)
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DraggingPieceOverlay(piece: Piece, cellSize: Float, pieceLeft: Float, pieceTop: Float) {
    val density = LocalDensity.current
    val shadowBleed = 8f
    val boxWidthPx = piece.width * cellSize + shadowBleed * 2
    val boxHeightPx = piece.height * cellSize + shadowBleed * 2

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = pieceLeft - shadowBleed
                translationY = pieceTop - shadowBleed
            }
            .size(with(density) { boxWidthPx.toDp() }, with(density) { boxHeightPx.toDp() })
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val ox = shadowBleed
            val oy = shadowBleed

            for (row in piece.shape.indices) {
                for (col in piece.shape[row].indices) {
                    if (piece.shape[row][col]) {
                        drawRoundRect(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f),
                            topLeft = Offset(ox + col * cellSize + 4f, oy + row * cellSize + 4f),
                            size = androidx.compose.ui.geometry.Size(cellSize - 2, cellSize - 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }
            for (row in piece.shape.indices) {
                for (col in piece.shape[row].indices) {
                    if (piece.shape[row][col]) {
                        val x = ox + col * cellSize
                        val y = oy + row * cellSize
                        drawRoundRect(
                            color = piece.flower.blockColor,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(cellSize - 2, cellSize - 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        drawRoundRect(
                            color = piece.flower.borderColor,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(cellSize - 2, cellSize - 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
            for (row in piece.shape.indices) {
                for (col in piece.shape[row].indices) {
                    if (piece.shape[row][col]) {
                        val x = ox + col * cellSize + cellSize / 2
                        val y = oy + row * cellSize + cellSize / 2
                        drawContext.canvas.nativeCanvas.drawText(
                            piece.flower.emoji, x, y + cellSize * 0.2f,
                            Paint().apply { textSize = cellSize * 0.5f; textAlign = Paint.Align.CENTER }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreHeader(score: Int, highScore: Int, isNewHighScore: Boolean) {
    val scoreScale = remember { Animatable(1f) }
    LaunchedEffect(score) {
        if (score > 0) {
            scoreScale.snapTo(1.22f)
            scoreScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "СЧЁТ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer { scaleX = scoreScale.value; scaleY = scoreScale.value }
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "РЕКОРД", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$highScore", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (isNewHighScore) Primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
