package com.flowerblast.game.logic

import com.flowerblast.game.model.Flower

object ScoreCalculator {

    fun calculatePlacementScore(piece: com.flowerblast.game.model.Piece): Int {
        return piece.shape.sumOf { row -> row.count { it } } * 5
    }

    fun calculateLinesPoints(linesCleared: Int, uniqueFlowers: Set<Flower>): Int {
        if (linesCleared == 0) return 0

        val basePoints = when (linesCleared) {
            1 -> 50
            2 -> 150
            3 -> 350
            else -> 700
        }

        var multiplier = 1.0f
        var bonusPoints = 0

        if (uniqueFlowers.any { it == Flower.LAVENDER }) multiplier *= 1.5f
        if (uniqueFlowers.size >= 3) bonusPoints += 30
        if (uniqueFlowers.size >= 5) bonusPoints += 100

        return (basePoints * multiplier).toInt() + bonusPoints
    }
}