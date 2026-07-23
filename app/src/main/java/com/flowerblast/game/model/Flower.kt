package com.flowerblast.game.model

import androidx.compose.ui.graphics.Color

enum class Flower(
    val emoji: String,
    val blockColor: Color,
    val borderColor: Color
) {
    ROSE("🌹",     Color(0xFFFF6B9D), Color(0xFFFF3380)),
    TULIP("🌷",    Color(0xFFFFAA3B), Color(0xFFFF8800)),
    DAISY("🌼",    Color(0xFF7DE87A), Color(0xFF44C742)),
    LAVENDER("🪻", Color(0xFFB39DDB), Color(0xFF8F6FD8)),
    BLOSSOM("🌸",  Color(0xFFFF80CE), Color(0xFFE040A0));

    companion object {
        val all: List<Flower> = entries
    }
}