package com.flowerblast.game.logic

import com.flowerblast.game.model.ClearResult
import com.flowerblast.game.model.Flower
import com.flowerblast.game.model.Piece

object BoardLogic {

    fun canPlace(piece: Piece, board: Array<Array<Flower?>>, startRow: Int, startCol: Int): Boolean {
        for ((row, col) in piece.getCells(startRow, startCol)) {
            if (row < 0 || row >= 8 || col < 0 || col >= 8) return false
            if (board[row][col] != null) return false
        }
        return true
    }

    fun canPlaceAnywhere(piece: Piece, board: Array<Array<Flower?>>): Boolean {
        var rotated = piece
        for (r in 0..3) {
            for (row in 0..(8 - rotated.height)) {
                for (col in 0..(8 - rotated.width)) {
                    if (canPlace(rotated, board, row, col)) return true
                }
            }
            rotated = rotated.rotate()
        }
        return false
    }

    fun checkAndClearLines(board: Array<Array<Flower?>>): ClearResult {
        val clearedRows = mutableSetOf<Int>()
        val clearedCols = mutableSetOf<Int>()

        for (row in 0..7) {
            if (board[row].all { it != null }) clearedRows.add(row)
        }

        for (col in 0..7) {
            if ((0..7).all { board[it][col] != null }) clearedCols.add(col)
        }

        val flowersInLines = mutableSetOf<Flower>()
        for (row in clearedRows) {
            board[row].forEach { flower -> flower?.let { flowersInLines.add(it) } }
        }
        for (col in clearedCols) {
            for (row in 0..7) {
                board[row][col]?.let { flowersInLines.add(it) }
            }
        }
        val isRainbow = flowersInLines.size >= 5

        for (row in clearedRows) {
            for (col in 0..7) board[row][col] = null
        }
        for (col in clearedCols) {
            for (row in 0..7) board[row][col] = null
        }

        val totalLines = clearedRows.size + clearedCols.size
        val points = ScoreCalculator.calculateLinesPoints(totalLines, flowersInLines)

        return ClearResult(clearedRows, clearedCols, points, isRainbow)
    }
}