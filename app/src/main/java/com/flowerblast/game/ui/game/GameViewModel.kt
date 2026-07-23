package com.flowerblast.game.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowerblast.game.data.ScoreRepository
import com.flowerblast.game.logic.BoardLogic
import com.flowerblast.game.logic.PieceGenerator
import com.flowerblast.game.logic.ScoreCalculator
import com.flowerblast.game.model.ClearResult
import com.flowerblast.game.model.Flower
import com.flowerblast.game.model.GamePhase
import com.flowerblast.game.model.GameUiState
import com.flowerblast.game.model.Piece
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val scoreRepository = ScoreRepository(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val highScore = scoreRepository.highScore.first()
            _uiState.update { it.copy(highScore = highScore) }
        }
        startNewGame()
    }

    fun startNewGame() {
        val newTray = PieceGenerator.generateTray()
        _uiState.update {
            it.copy(
                board = Array(8) { arrayOfNulls(8) },
                trayPieces = newTray,
                score = 0,
                phase = GamePhase.Playing,
                clearedRows = emptySet(),
                clearedCols = emptySet()
            )
        }
    }

    fun rotatePiece(index: Int) {
        _uiState.update { state ->
            val pieces = state.trayPieces.toMutableList()
            val piece = pieces[index]
            if (piece != null) {
                pieces[index] = piece.rotate()
            }
            state.copy(trayPieces = pieces)
        }
    }

    fun startDrag(pieceIndex: Int) {
        _uiState.update { it.copy(draggingPieceIndex = pieceIndex) }
    }

    fun endDrag() {
        _uiState.update { it.copy(draggingPieceIndex = null) }
    }

    fun placePiece(pieceIndex: Int, startRow: Int, startCol: Int) {
        val state = _uiState.value
        val piece = state.trayPieces.getOrNull(pieceIndex) ?: return

        var rotatedPiece = piece
        for (i in 0..3) {
            if (BoardLogic.canPlace(rotatedPiece, state.board, startRow, startCol)) {
                placePieceOnBoard(rotatedPiece, startRow, startCol, pieceIndex)
                return
            }
            rotatedPiece = rotatedPiece.rotate()
        }
    }

    private fun placePieceOnBoard(piece: Piece, startRow: Int, startCol: Int, pieceIndex: Int) {
        val state = _uiState.value
        val newBoard = state.board.map { it.copyOf() }.toTypedArray()

        for ((row, col) in piece.getCells(startRow, startCol)) {
            newBoard[row][col] = piece.flower
        }

        val placementPoints = ScoreCalculator.calculatePlacementScore(piece)
        val clearResult: ClearResult = BoardLogic.checkAndClearLines(newBoard)
        val totalPoints = placementPoints + clearResult.points

        val newTray = state.trayPieces.toMutableList()
        newTray[pieceIndex] = null

        val hasRemaining = newTray.any { p -> p != null }
        val nextTray = if (!hasRemaining) {
            PieceGenerator.generateTray()
        } else {
            newTray
        }

        val gameOver = checkGameOver(nextTray, newBoard)

        viewModelScope.launch {
            val currentHigh = scoreRepository.highScore.first()
            val newScore = state.score + totalPoints
            val newHigh = if (newScore > currentHigh) newScore else currentHigh

            if (newScore > currentHigh) {
                scoreRepository.updateHighScore(newScore)
            }

            _uiState.update {
                it.copy(
                    board = newBoard,
                    score = newScore,
                    highScore = newHigh,
                    trayPieces = nextTray,
                    phase = if (gameOver) GamePhase.GameOver else GamePhase.Playing,
                    clearedRows = clearResult.clearedRows,
                    clearedCols = clearResult.clearedCols,
                    isRainbowBonus = clearResult.isRainbow
                )
            }

            delay(400)
            _uiState.update {
                it.copy(clearedRows = emptySet(), clearedCols = emptySet(), isRainbowBonus = false)
            }
        }
    }

    private fun checkGameOver(trayPieces: List<Piece?>, board: Array<Array<Flower?>>): Boolean {
        val pieces = trayPieces.filterNotNull()
        if (pieces.isEmpty()) return false
        return pieces.none { BoardLogic.canPlaceAnywhere(it, board) }
    }
}