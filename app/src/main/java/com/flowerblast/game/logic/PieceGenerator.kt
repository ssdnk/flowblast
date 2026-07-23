package com.flowerblast.game.logic

import com.flowerblast.game.model.Flower
import com.flowerblast.game.model.Piece
import kotlin.random.Random

object PieceGenerator {

    private val SHAPES = listOf(
        listOf(listOf(true, true)),
        listOf(listOf(true, true, true)),
        listOf(listOf(true, true, true, true)),
        listOf(listOf(true)),
        listOf(listOf(true, true), listOf(true, true)),
        listOf(listOf(true, false), listOf(true, false), listOf(true, true)),
        listOf(listOf(false, true), listOf(false, true), listOf(true, true)),
        listOf(listOf(true, true, true), listOf(false, true, false)),
        listOf(listOf(false, true, true), listOf(true, true, false)),
        listOf(listOf(true, true, false), listOf(false, true, true)),
        listOf(listOf(true, true), listOf(true, false))
    )

    fun generateTray(): List<Piece> {
        val flowers = pickFlowers(2)
        return (0..1).map {
            Piece(SHAPES.random().map { row -> row.toList() }, flowers[it])
        }
    }

    private fun pickFlowers(count: Int): List<Flower> {
        return Flower.all.shuffled().take(count)
    }
}