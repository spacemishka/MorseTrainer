package com.spacemishka.app.morsetrainer.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.spacemishka.app.morsetrainer.ui.screens.*

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MorseViewModel,
    hangmanViewModel: HangmanViewModel,
    treasureHuntViewModel: TreasureHuntViewModel,
    typingChallengeViewModel: TypingChallengeViewModel,
    memoryViewModel: MemoryViewModel,
    qsoSimulatorViewModel: QsoSimulatorViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "profiles"
    ) {
        composable("profiles") {
            ProfileScreen(viewModel, navController)
        }
        composable("dashboard") {
            DashboardScreen(viewModel, navController)
        }
        composable("koch_lesson") {
            KochLessonScreen(viewModel, navController)
        }
        composable("word_training") {
            WordTrainingScreen(viewModel, navController)
        }
        composable("free_text") {
            FreeTextScreen(viewModel, navController)
        }
        composable("stats") {
            StatsScreen(viewModel, navController)
        }
        composable("settings") {
            SettingsScreen(viewModel, navController)
        }
        composable("games") {
            GamesScreen(navController)
        }
        composable("hangman") {
            HangmanScreen(hangmanViewModel, navController)
        }
        composable("treasure_hunt") {
            TreasureHuntScreen(treasureHuntViewModel, navController)
        }
        composable("typing_challenge") {
            TypingChallengeScreen(typingChallengeViewModel, navController)
        }
        composable("memory") {
            MemoryScreen(memoryViewModel, navController)
        }
        composable("qso_simulator") {
            QsoSimulatorScreen(qsoSimulatorViewModel, navController)
        }
    }
}
