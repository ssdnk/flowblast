package com.flowerblast.game.model

data class Piece(
    val shape: List<List<Boolean>>,
    val flower: Flower
) {
    val width: Int get() = shape.firstOrNull()?.size ?: 0
    val height: Int get() = shape.size

    fun rotate(): Piece {
        if (shape.isEmpty()) return this
        val rotated = shape.first().indices.map { col ->
            shape.indices.map { row -> shape[row][col] }.reversed()
        }
        return copy(shape = rotated)
    }

    fun getCells(offsetRow: Int = 0, offsetCol: Int = 0): List<Pair<Int, Int>> {
        return shape.flatMapIndexed { row, rowList ->
            rowList.mapIndexedNotNull { col, isSet ->
                if (isSet) Pair(row + offsetRow, col + offsetCol) else null
            }
        }
    }

    fun isValidPosition(board: Array<Array<Flower?>>, startRow: Int, startCol: Int): Boolean {
        for ((row, col) in getCells(startRow, startCol)) {
            if (row < 0 || row >= 8 || col < 0 || col >= 8) return false
            if (board[row][col] != null) return false
        }
        return true
    }
}

data class GameUiState(
    val board: Array<Array<Flower?>> = Array(8) { arrayOfNulls(8) },
    val trayPieces: List<Piece?> = listOf(null, null),
    val score: Int = 0,
    val highScore: Int = 0,
    val phase: GamePhase = GamePhase.Playing,
    val clearedRows: Set<Int> = emptySet(),
    val clearedCols: Set<Int> = emptySet(),
    val isRainbowBonus: Boolean = false,
    val draggingPieceIndex: Int? = null,
    val currentRotation: IntArray = intArrayOf(0, 0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameUiState
        return board.contentDeepEquals(other.board) &&
                trayPieces == other.trayPieces &&
                score == other.score &&
                highScore == other.highScore &&
                phase == other.phase &&
                clearedRows == other.clearedRows &&
                clearedCols == other.clearedCols &&
                draggingPieceIndex == other.draggingPieceIndex
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + trayPieces.hashCode()
        result = 31 * result + score
        result = 31 * result + highScore
        result = 31 * result + phase.hashCode()
        result = 31 * result + clearedRows.hashCode()
        result = 31 * result + clearedCols.hashCode()
        return result
    }
}

sealed class GamePhase {
    data object Playing : GamePhase()
    data object GameOver : GamePhase()
}

data class ClearResult(
    val clearedRows: Set<Int>,
    val clearedCols: Set<Int>,
    val points: Int,
    val isRainbow: Boolean
)