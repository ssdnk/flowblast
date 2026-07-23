package com.flowerblast.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flowerblast.game.ui.game.GameScreen
import com.flowerblast.game.ui.game.GameViewModel
import com.flowerblast.game.ui.game.StartScreen
import com.flowerblast.game.ui.theme.FlowerBlastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowerBlastTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val gameVm: GameViewModel = viewModel()
                    var showRules by remember { mutableStateOf(false) }

                    NavHost(navController = navController, startDestination = "start") {
                        composable("start") {
                            var hsc by remember { mutableStateOf(0) }
                            // Collect highScore from ViewModel
                            StartScreen(
                                onStartGame = {
                                    gameVm.startNewGame()
                                    navController.navigate("game")
                                },
                                onShowRules = { showRules = true }
                            )
                        }
                        composable("game") {
                            GameScreen(
                                onNavigateToMenu = { navController.popBackStack() },
                                viewModel = gameVm
                            )
                        }
                    }

                    if (showRules) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showRules = false },
                            title = { androidx.compose.material3.Text("Как играть?") },
                            text = {
                                androidx.compose.material3.Text(
                                    "• Перетаскивай фигуры из лотка на поле\n" +
                                            "• Тапни по фигуре, чтобы повернуть\n" +
                                            "• Заполни строку или столбец полностью — они очистятся\n" +
                                            "• Лаванда даёт бонус ×1.5 к очкам"
                                )
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = { showRules = false }) {
                                    androidx.compose.material3.Text("Понятно")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}