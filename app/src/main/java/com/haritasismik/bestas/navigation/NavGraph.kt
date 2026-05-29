package com.haritasismik.bestas.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.haritasismik.bestas.game.models.GameMode
import com.haritasismik.bestas.game.models.StoneStyle
import com.haritasismik.bestas.ui.screens.*

/**
 * Navigasyon rotaları
 */
object Routes {
    const val MAIN_MENU = "main_menu"
    const val GAME = "game/{mode}"
    const val SETTINGS = "settings"
    const val LEADERBOARD = "leaderboard"

    fun gameRoute(mode: GameMode): String = "game/${mode.name}"
}

@Composable
fun NavGraph(
    navController: NavHostController
) {
    var stoneStyle by remember { mutableStateOf(StoneStyle.REALISTIC) }

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN_MENU
    ) {
        composable(Routes.MAIN_MENU) {
            MainMenuScreen(
                onPlayClick = { mode ->
                    navController.navigate(Routes.gameRoute(mode))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onLeaderboardClick = {
                    navController.navigate(Routes.LEADERBOARD)
                }
            )
        }

        composable(Routes.GAME) { backStackEntry ->
            val modeStr = backStackEntry.arguments?.getString("mode") ?: GameMode.LOCAL.name
            val mode = GameMode.valueOf(modeStr)

            GameScreen(
                gameMode = mode,
                stoneStyle = stoneStyle,
                onBackToMenu = {
                    navController.popBackStack(Routes.MAIN_MENU, false)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                currentStoneStyle = stoneStyle,
                onStoneStyleChange = { stoneStyle = it },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
