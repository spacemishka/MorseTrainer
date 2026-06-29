package com.spacemishka.app.morsetrainer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.spacemishka.app.morsetrainer.R
import com.spacemishka.app.morsetrainer.ui.*

// ==========================================
// 1. Games Lobby Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gamification_modules), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("dashboard") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThemeAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeSurface)
            )
        },
        containerColor = ThemeBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(stringResource(R.string.select_game), color = Color.White, fontSize = 16.sp)
            }

            item {
                GameLobbyItem(
                    title = stringResource(R.string.hangman_title),
                    desc = stringResource(R.string.hangman_desc),
                    route = "hangman",
                    navController = navController
                )
            }

            item {
                GameLobbyItem(
                    title = stringResource(R.string.treasure_hunt_title),
                    desc = stringResource(R.string.treasure_hunt_desc),
                    route = "treasure_hunt",
                    navController = navController
                )
            }

            item {
                GameLobbyItem(
                    title = stringResource(R.string.typing_challenge_title),
                    desc = stringResource(R.string.typing_challenge_desc),
                    route = "typing_challenge",
                    navController = navController
                )
            }

            item {
                GameLobbyItem(
                    title = stringResource(R.string.memory_title),
                    desc = stringResource(R.string.memory_desc),
                    route = "memory",
                    navController = navController
                )
            }

            item {
                GameLobbyItem(
                    title = stringResource(R.string.qso_simulator_title),
                    desc = stringResource(R.string.qso_simulator_desc),
                    route = "qso_simulator",
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun GameLobbyItem(title: String, desc: String, route: String, navController: NavController) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemeSlate700),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(route) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = ThemeAmber, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, color = Color.LightGray, fontSize = 13.sp)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ThemeAmber, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

// ==========================================
// 2. Hangman Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangmanScreen(viewModel: HangmanViewModel, navController: NavController) {
    val revealedWord by viewModel.revealedWord.collectAsState()
    val remainingAttempts by viewModel.remainingAttempts.collectAsState()
    val gameStatus by viewModel.gameStatus.collectAsState()
    val clueChar by viewModel.clueChar.collectAsState()
    val guessedLetters by viewModel.guessedLetters.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.playClueSound()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hangman_title), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAudio()
                        navController.navigate("games")
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThemeAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeSurface)
            )
        },
        containerColor = ThemeBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Heart stats & reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.attempts_left, remainingAttempts), color = ThemeRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = { viewModel.startNewGame() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Game", tint = ThemeAmber)
                }
            }

            // Clue sound playing
            IconButton(
                onClick = { viewModel.playClueSound() },
                modifier = Modifier
                    .size(80.dp)
                    .background(ThemeSurface, RoundedCornerShape(40.dp))
                    .border(1.dp, ThemeAmber, RoundedCornerShape(40.dp))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Clue", tint = ThemeAmber, modifier = Modifier.size(40.dp))
            }

            // Word revealed spaces
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                revealedWord.forEach { char ->
                    Text(
                        text = "$char ",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Status message
            Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                when (gameStatus) {
                    "WON" -> Text(stringResource(R.string.hangman_won), color = ThemeGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    "LOST" -> Text(stringResource(R.string.hangman_game_over), color = ThemeRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    else -> Text(stringResource(R.string.hangman_instruction), color = Color.Gray, fontSize = 13.sp)
                }
            }

            // Keyboard grid
            val letters = ('A'..'Z').toList()
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(letters) { letter ->
                    val isUsed = guessedLetters.contains(letter)
                    val cardColor = if (isUsed) ThemeSlate700.copy(alpha = 0.5f) else ThemeSurface
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(1.dp, if (isUsed) Color.Transparent else ThemeSlate700),
                        modifier = Modifier
                            .height(40.dp)
                            .clickable(enabled = gameStatus == "PLAYING" && !isUsed) {
                                viewModel.makeGuess(letter)
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                letter.toString(),
                                color = if (isUsed) Color.Gray else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. Treasure Hunt Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureHuntScreen(viewModel: TreasureHuntViewModel, navController: NavController) {
    val currentIndex by viewModel.currentClueIndex.collectAsState()
    val feedback by viewModel.feedbackState.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()

    val clue = viewModel.getCurrentClue()
    var userAns by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.treasure_hunt_title), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAudio()
                        navController.navigate("games")
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThemeAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeSurface)
            )
        },
        containerColor = ThemeBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isCompleted) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.treasure_found), color = ThemeGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.treasure_won_text), color = Color.White, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.restartHunt() },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber)
                    ) {
                        Text(stringResource(R.string.restart_challenge), color = ThemeBackground)
                    }
                }
            } else if (clue != null) {
                Text(stringResource(R.string.clue_progress, currentIndex + 1), color = ThemeAmber, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { viewModel.playClueSound() },
                        modifier = Modifier
                            .size(90.dp)
                            .background(ThemeSurface, RoundedCornerShape(45.dp))
                            .border(1.dp, ThemeAmber, RoundedCornerShape(45.dp))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Clue", tint = ThemeAmber, modifier = Modifier.size(45.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.hint_label, clue.hint), color = Color.LightGray, fontSize = 15.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }

                // Answer form
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (feedback) {
                            "CORRECT" -> Text(stringResource(R.string.accurate), color = ThemeGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            "INCORRECT" -> Text(stringResource(R.string.try_again), color = ThemeRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            else -> Spacer(modifier = Modifier.fillMaxSize())
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = userAns,
                            onValueChange = { userAns = it },
                            label = { Text(stringResource(R.string.transcribe_clue)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemeAmber,
                                unfocusedBorderColor = ThemeSlate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.submitAnswer(userAns)
                                userAns = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text(stringResource(R.string.submit), color = ThemeBackground)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. Typing Arcade Challenge Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingChallengeScreen(viewModel: TypingChallengeViewModel, navController: NavController) {
    val score by viewModel.score.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val bestStreak by viewModel.bestStreak.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val currentSymbol by viewModel.currentStreamChar.collectAsState()

    var textInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.typing_challenge_title), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.endGame()
                        navController.navigate("games")
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThemeAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeSurface)
            )
        },
        containerColor = ThemeBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (gameState) {
                "IDLE" -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.typing_challenge_header), color = ThemeAmber, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.typing_challenge_instruction),
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.startGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber)
                        ) {
                            Text(stringResource(R.string.start_game_60s), color = ThemeBackground)
                        }
                    }
                }
                "RUNNING" -> {
                    // Game Panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.time_label, timeRemaining), color = ThemeAmber, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.combo_streak, streak), color = ThemeGreen, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.score_label, score), color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Large radar style screen
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color(0xFF070B13), RoundedCornerShape(100.dp))
                            .border(2.dp, ThemeAmber, RoundedCornerShape(100.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CW", color = ThemeAmber.copy(alpha = 0.5f), fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(if (currentSymbol.isEmpty()) "?" else currentSymbol, color = ThemeAmber, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Input
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = {
                                textInput = it
                                if (it.isNotEmpty()) {
                                    viewModel.submitInput(it)
                                    textInput = ""
                                }
                            },
                            label = { Text(stringResource(R.string.type_heard_char)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemeAmber,
                                unfocusedBorderColor = ThemeSlate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                "FINISHED" -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.times_up), color = ThemeRed, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.final_score, score), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.best_combo, bestStreak), color = ThemeGreen, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.startGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber)
                        ) {
                            Text(stringResource(R.string.play_again), color = ThemeBackground)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. Memory Match Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(viewModel: MemoryViewModel, navController: NavController) {
    val cards by viewModel.cards.collectAsState()
    val moves by viewModel.moves.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.memory_title), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("games") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThemeAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeSurface)
            )
        },
        containerColor = ThemeBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.moves_label, moves), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.startNewGame() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = ThemeAmber)
                }
            }

            if (isCompleted) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.matched_all), color = ThemeGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.moves_taken, moves), color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.startNewGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber)
                    ) {
                        Text(stringResource(R.string.play_again), color = ThemeBackground)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cards.size) { index ->
                        val card = cards[index]
                        val bgColor = when {
                            card.isMatched -> ThemeGreen.copy(alpha = 0.2f)
                            card.isFaceUp -> ThemeSurface
                            else -> ThemeSlate700
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = BorderStroke(1.dp, if (card.isFaceUp) ThemeAmber else Color.Transparent),
                            modifier = Modifier
                                .height(72.dp)
                                .clickable {
                                    viewModel.selectCard(index)
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (card.isFaceUp || card.isMatched) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (card.type == MemoryViewModel.CardType.CHAR) card.text else card.morsePattern,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (card.type == MemoryViewModel.CardType.CHAR) 24.sp else 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = if (card.type == MemoryViewModel.CardType.CHAR) "text" else "code",
                                            color = Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    }
                                } else {
                                    // Card back (Morse key icon placeholder or ?)
                                    Text("?", color = ThemeAmber.copy(alpha = 0.5f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. QSO Radio Simulator Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QsoSimulatorScreen(viewModel: QsoSimulatorViewModel, navController: NavController) {
    val stepIdx by viewModel.currentStepIdx.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val feedback by viewModel.feedback.collectAsState()

    val step = viewModel.getCurrentStep()

    LaunchedEffect(Unit) {
        viewModel.startQso()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qso_simulator_title), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAudio()
                        navController.navigate("games")
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThemeAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeSurface)
            )
        },
        containerColor = ThemeBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isCompleted) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.qso_logged), color = ThemeGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.qso_closed), color = Color.White, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.startQso() },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber)
                    ) {
                        Text(stringResource(R.string.simulate_again), color = ThemeBackground)
                    }
                }
            } else if (step != null) {
                // Audio play
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { viewModel.playStepSound() },
                        modifier = Modifier
                            .size(80.dp)
                            .background(ThemeSurface, RoundedCornerShape(40.dp))
                            .border(1.dp, ThemeAmber, RoundedCornerShape(40.dp))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Incoming Message", tint = ThemeAmber, modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.incoming_signal), color = ThemeAmber, fontWeight = FontWeight.Bold)
                    Text(step.hintText, color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                // Interactive choice board
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (feedback) {
                            "CORRECT" -> Text(stringResource(R.string.qso_feedback_sent), color = ThemeGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            "INCORRECT" -> Text(stringResource(R.string.qso_feedback_wrong), color = ThemeRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            else -> Spacer(modifier = Modifier.fillMaxSize())
                        }
                    }

                    Text(stringResource(R.string.select_response), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                    step.userOptions.forEachIndexed { idx, option ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                            border = BorderStroke(1.dp, ThemeSlate700),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.selectOption(idx)
                                }
                        ) {
                            Box(modifier = Modifier.padding(14.dp)) {
                                Text(option, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
