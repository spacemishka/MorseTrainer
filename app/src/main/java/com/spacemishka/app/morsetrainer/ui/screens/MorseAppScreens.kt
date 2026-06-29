package com.spacemishka.app.morsetrainer.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.spacemishka.app.morsetrainer.R
import com.spacemishka.app.morsetrainer.data.entity.UserProfile
import com.spacemishka.app.morsetrainer.ui.MorseViewModel
import com.spacemishka.app.morsetrainer.audio.MorseAudioEngine
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

// Theme Colors
val ThemeBackground = Color(0xFF0F172A)
val ThemeSurface = Color(0xFF1E293B)
val ThemeAmber = Color(0xFFF59E0B)
val ThemeGreen = Color(0xFF10B981)
val ThemeRed = Color(0xFFEF4444)
val ThemeSlate700 = Color(0xFF334155)

@Composable
fun AudioVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(Color(0xFF0B0F19), RoundedCornerShape(8.dp))
            .border(1.dp, ThemeAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        val width = size.width
        val height = size.height
        val points = 80
        val path = Path()

        path.moveTo(0f, height / 2)
        for (i in 0..points) {
            val x = width * i / points
            val amp = if (isPlaying) height * 0.35f else height * 0.04f
            // Generate standard sine shape
            val freq = if (isPlaying) 0.15f else 0.05f
            val y = (height / 2) + sin((i * freq) - phase) * amp
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = ThemeAmber,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// ==========================================
// 1. Profile Selection Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MorseViewModel, navController: NavController) {
    val profiles by viewModel.allProfiles.collectAsState()
    var nameInput by remember { mutableStateOf("") }
    var showDeleteConfirmFor by remember { mutableStateOf<UserProfile?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), color = ThemeAmber, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.select_create_profile),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // New Profile Form
            Card(
                colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                border = BorderStroke(1.dp, ThemeSlate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.create_new_profile), color = ThemeAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.profile_name), color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeAmber,
                            unfocusedBorderColor = ThemeSlate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                viewModel.createProfile(nameInput.trim())
                                nameInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.add_profile), color = ThemeBackground)
                    }
                }
            }

            // Existing Profiles List
            Text(stringResource(R.string.active_profiles), color = ThemeAmber, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), fontWeight = FontWeight.Bold)
            
            if (profiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_profiles), color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(profiles) { profile ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        viewModel.selectProfile(profile)
                                        navController.navigate("dashboard")
                                    },
                            border = BorderStroke(1.dp, ThemeSlate700)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(profile.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("Level ${profile.currentKochLevel} (${stringResource(R.string.speed_wpm, profile.speedWpm)})", color = Color.Gray, fontSize = 13.sp)
                                }
                                Row {
                                    IconButton(onClick = { showDeleteConfirmFor = profile }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_profile), tint = ThemeRed)
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Select", tint = ThemeAmber)
                                }
                            }
                        }
                    }
                }
            }
        }

        showDeleteConfirmFor?.let { profileToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmFor = null },
                title = { Text(stringResource(R.string.delete_profile_confirm_title), color = ThemeRed, fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.delete_profile_confirm_msg, profileToDelete.name), color = Color.White) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteProfile(profileToDelete)
                            showDeleteConfirmFor = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeRed)
                    ) {
                        Text(stringResource(R.string.delete), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmFor = null }) {
                        Text(stringResource(R.string.cancel), color = Color.LightGray)
                    }
                },
                containerColor = ThemeSurface
            )
        }
    }
}

// ==========================================
// 2. Main Dashboard Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MorseViewModel, navController: NavController) {
    val profile by viewModel.currentProfile.collectAsState()
    val currentStats by viewModel.currentStats.collectAsState()
    val achievements by viewModel.currentAchievements.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ThemeAmber)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("profiles") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.profile_title), tint = ThemeAmber)
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
                .padding(16.dp)
        ) {
            // Header Profile Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    border = BorderStroke(1.dp, ThemeAmber.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(profile!!.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Badge(containerColor = ThemeAmber) {
                                Text("${stringResource(R.string.level_label)} ${profile!!.currentKochLevel}", color = ThemeBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.speed_wpm, profile!!.speedWpm), color = Color.LightGray)
                            Text(stringResource(R.string.farnsworth_wpm, profile!!.effectiveSpeedWpm), color = Color.LightGray)
                        }
                    }
                }
            }

            // Training Modes Grid
            item {
                Text(stringResource(R.string.training_modes), color = ThemeAmber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }
            
            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        DashboardButton(
                            title = stringResource(R.string.koch_lesson_title),
                            desc = stringResource(R.string.koch_lesson_desc),
                            icon = Icons.Default.Star,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.resetSessionStats()
                            viewModel.generateNextTarget()
                            navController.navigate("koch_lesson")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        DashboardButton(
                            title = stringResource(R.string.word_practice_title),
                            desc = stringResource(R.string.word_practice_desc),
                            icon = Icons.Default.List,
                            modifier = Modifier.weight(1f)
                        ) {
                            navController.navigate("word_training")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        DashboardButton(
                            title = stringResource(R.string.free_text_title),
                            desc = stringResource(R.string.free_text_desc),
                            icon = Icons.Default.Build,
                            modifier = Modifier.weight(1f)
                        ) {
                            navController.navigate("free_text")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        DashboardButton(
                            title = stringResource(R.string.games_title),
                            desc = stringResource(R.string.games_desc),
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.weight(1f)
                        ) {
                            navController.navigate("games")
                        }
                    }
                }
            }

            // Navigation Options
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.info_configs), color = ThemeAmber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, ThemeSlate700)
                ) {
                    Column {
                        NavigationListItem(stringResource(R.string.my_stats), Icons.Default.Info) {
                            navController.navigate("stats")
                        }
                        Divider(color = ThemeSlate700)
                        NavigationListItem(stringResource(R.string.settings), Icons.Default.Settings) {
                            navController.navigate("settings")
                        }
                    }
                }
            }

            // Backup & Restore
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    border = BorderStroke(1.dp, ThemeSlate700)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.backup_data), color = Color.White)
                        Row {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val backupJson = viewModel.exportDataJson()
                                        // Copy to clipboard or share
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("MorseTrainerBackup", backupJson)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber)
                            ) {
                                Text(stringResource(R.string.export), color = ThemeBackground)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    // Normally file selector, but copy-paste clip prompt is extremely safe and offline-capable for android testing.
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clipData = clipboard.primaryClip
                                    if (clipData != null && clipData.itemCount > 0) {
                                        val text = clipData.getItemAt(0).text.toString()
                                        scope.launch {
                                            val success = viewModel.importDataJson(text)
                                            if (success) {
                                                Toast.makeText(context, "Backup restored from clipboard!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Invalid backup payload in clipboard", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeSlate700)
                            ) {
                                Text(stringResource(R.string.import_clipboard), color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardButton(
    title: String,
    desc: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemeSlate700),
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = title, tint = ThemeAmber, modifier = Modifier.size(28.dp))
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(desc, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun NavigationListItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ThemeAmber)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontSize = 16.sp)
        }
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray)
    }
}

// ==========================================
// 3. Koch Lesson Training Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KochLessonScreen(viewModel: MorseViewModel, navController: NavController) {
    val targetChar by viewModel.currentTargetChar.collectAsState()
    val isPlaying by viewModel.isPlayingAudio.collectAsState()
    val lastFeedback by viewModel.lastInputFeedback.collectAsState()
    val recentAttempts by viewModel.recentAttempts.collectAsState()
    val accuracy by viewModel.lessonAccuracy.collectAsState()
    val promotionUnlocked by viewModel.unlockedPromotion.collectAsState()
    val profile by viewModel.currentProfile.collectAsState()
    val currentStats by viewModel.currentStats.collectAsState()

    var inputState by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Key list of active characters for on-screen buttons
    val activeChars = remember(profile?.currentKochLevel) {
        profile?.let {
            val count = (it.currentKochLevel + 1).coerceIn(2, it.customSequence.length)
            it.customSequence.substring(0, count).map { c -> c.toString() }
        } ?: emptyList()
    }

    // List of characters to introduce at the start of this level
    val charsToIntroduce = remember(profile?.currentKochLevel, profile?.customSequence) {
        profile?.let {
            if (it.currentKochLevel == 1) {
                listOf(
                    it.customSequence.getOrNull(0)?.toString() ?: "K",
                    it.customSequence.getOrNull(1)?.toString() ?: "M"
                )
            } else {
                val index = it.currentKochLevel
                if (index < it.customSequence.length) {
                    listOf(it.customSequence[index].toString())
                } else emptyList()
            }
        } ?: emptyList()
    }

    // Track which characters have been dismissed during the current introduction flow
    var introducedChars by remember { mutableStateOf(setOf<String>()) }

    // The character currently being introduced
    val currentIntroChar = remember(charsToIntroduce, introducedChars, currentStats) {
        charsToIntroduce.firstOrNull { char ->
            val stat = currentStats.find { it.character == char }
            val hasPracticed = stat != null && stat.attempts > 0
            !hasPracticed && char !in introducedChars
        }
    }

    val showIntro = currentIntroChar != null

    // Auto-play the target character sound when the introduction dialog is shown
    LaunchedEffect(showIntro, currentIntroChar) {
        if (showIntro) {
            currentIntroChar?.let { viewModel.playTextDirectly(it) }
        }
    }

    // Auto-play the target character sound whenever a new target is generated or on entering screen (if not in introduction)
    LaunchedEffect(lastFeedback, targetChar, showIntro) {
        if (!showIntro && lastFeedback == null && targetChar.isNotEmpty()) {
            viewModel.playCurrentTarget()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${stringResource(R.string.koch_lesson_title)} - ${stringResource(R.string.level_label)} ${profile?.currentKochLevel ?: 0}", color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAudio()
                        navController.navigate("dashboard")
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
            // Top visualizer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AudioVisualizer(isPlaying)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.recent_accuracy, (accuracy * 100).toInt()), color = Color.LightGray)
                    Text(stringResource(R.string.attempts, recentAttempts.size, profile?.kochWindowSize ?: 20), color = Color.LightGray)
                }
            }

            // Target play button & status
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { viewModel.playCurrentTarget() },
                    enabled = lastFeedback == null,
                    modifier = Modifier
                        .size(100.dp)
                        .background(ThemeSurface, RoundedCornerShape(50.dp))
                        .border(2.dp, if (lastFeedback == null) ThemeAmber else Color.Gray, RoundedCornerShape(50.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play_text),
                        tint = if (lastFeedback == null) ThemeAmber else Color.Gray,
                        modifier = Modifier.size(50.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.practice_instruction),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }

            // Input UI
            Column(modifier = Modifier.fillMaxWidth()) {
                // Feedback message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (lastFeedback) {
                        true -> Text(stringResource(R.string.correct), color = ThemeGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        false -> Text(stringResource(R.string.wrong_correct_was, targetChar), color = ThemeRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        null -> Spacer(modifier = Modifier.fillMaxSize())
                    }
                }

                // Custom keyboard with learned characters
                Text(stringResource(R.string.select_learned_char), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeChars) { char ->
                        val isNew = char == currentIntroChar
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNew) ThemeAmber.copy(alpha = 0.2f) else ThemeSurface
                            ),
                            border = BorderStroke(
                                width = if (isNew) 2.dp else 1.dp,
                                color = if (isNew) ThemeAmber else ThemeSlate700
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .clickable(enabled = lastFeedback == null) {
                                    viewModel.handleUserInput(char)
                                    // Generate next target after delay
                                    scope.launch {
                                        delay(1500)
                                        viewModel.generateNextTarget()
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(char, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (isNew) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("★", color = ThemeAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputState,
                        onValueChange = {
                            if (it.length <= 1) {
                                inputState = it
                            }
                        },
                        enabled = lastFeedback == null,
                        label = { Text(stringResource(R.string.or_type_here)) },
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
                            if (inputState.isNotBlank()) {
                                viewModel.handleUserInput(inputState)
                                inputState = ""
                                scope.launch {
                                    delay(1500)
                                    viewModel.generateNextTarget()
                                }
                            }
                        },
                        enabled = lastFeedback == null,
                        colors = ButtonDefaults.buttonColors(containerColor = if (lastFeedback == null) ThemeAmber else Color.Gray),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(stringResource(R.string.submit), color = ThemeBackground)
                    }
                }
            }
        }

        // Promotion alert dialog
        if (promotionUnlocked) {
            AlertDialog(
                onDismissRequest = { /* Force response */ },
                title = { Text(stringResource(R.string.level_promoted), color = ThemeGreen, fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.promotion_text), color = Color.White) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.promoteUser() },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber)
                    ) {
                        Text(stringResource(R.string.introduce_new_char), color = ThemeBackground)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetSessionStats() }) {
                        Text(stringResource(R.string.stay_for_practice), color = Color.LightGray)
                    }
                },
                containerColor = ThemeSurface
            )
        }

        // New character introduction dialog
        if (showIntro) {
            currentIntroChar?.let { charToShow ->
                val pattern = MorseAudioEngine.MORSE_MAP[charToShow.firstOrNull() ?: ' '] ?: ""
                val prettyPattern = pattern.map { if (it == '.') "•" else "—" }.joinToString(" ")

                val isLast = remember(charsToIntroduce, charToShow) {
                    charsToIntroduce.indexOf(charToShow) == charsToIntroduce.size - 1
                }

                AlertDialog(
                    onDismissRequest = { /* Force response */ },
                    title = { Text(stringResource(R.string.introducing_new_char_title), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = charToShow,
                                color = Color.White,
                                fontSize = 60.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = prettyPattern,
                                color = ThemeAmber,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.new_char_practice_instruction),
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { introducedChars = introducedChars + charToShow },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber)
                        ) {
                            Text(
                                text = if (isLast) stringResource(R.string.start_practice) else stringResource(R.string.next),
                                color = ThemeBackground
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.playTextDirectly(charToShow) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.replay_sound), color = ThemeAmber)
                            }
                        }
                    },
                    containerColor = ThemeSurface
                )
            }
        }
    }
}

// ==========================================
// 4. Word Training Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordTrainingScreen(viewModel: MorseViewModel, navController: NavController) {
    val isPlaying by viewModel.isPlayingAudio.collectAsState()
    val wordPool = remember { listOf("CQ", "TEST", "73", "QTH", "OP", "RST", "QRZ", "DE", "HAM", "RADIO", "HELLO") }

    var currentWord by remember { mutableStateOf("CQ") }
    var inputVal by remember { mutableStateOf("") }
    var isCorrectFeedback by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    fun playWord() {
        viewModel.playTextDirectly(currentWord)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.word_copy_title), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAudio()
                        navController.navigate("dashboard")
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
            AudioVisualizer(isPlaying)

            // Play & Hint block
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { playWord() },
                    enabled = isCorrectFeedback == null,
                    modifier = Modifier
                        .size(80.dp)
                        .background(ThemeSurface, RoundedCornerShape(40.dp))
                        .border(
                            1.dp,
                            if (isCorrectFeedback == null) ThemeAmber else Color.Gray,
                            RoundedCornerShape(40.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isCorrectFeedback == null) ThemeAmber else Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.transcribe_word_instruction), color = Color.Gray, textAlign = TextAlign.Center)
            }

            // Input field and Check action
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (isCorrectFeedback) {
                        true -> Text(stringResource(R.string.correct), color = ThemeGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        false -> Text(stringResource(R.string.wrong_correct, currentWord), color = ThemeRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        null -> Spacer(modifier = Modifier.fillMaxSize())
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = { inputVal = it },
                        enabled = isCorrectFeedback == null,
                        label = { Text(stringResource(R.string.or_type_here), color = Color.Gray) },
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
                            val correct = inputVal.trim().uppercase() == currentWord.uppercase()
                            isCorrectFeedback = correct
                            if (correct) {
                                viewModel.unlockAchievement("FIRST_WORD_COPIED")
                            }
                            scope.launch {
                                delay(2000)
                                isCorrectFeedback = null
                                inputVal = ""
                                currentWord = wordPool.random()
                                playWord()
                            }
                        },
                        enabled = isCorrectFeedback == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCorrectFeedback == null) ThemeAmber else Color.Gray
                        ),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(stringResource(R.string.check), color = ThemeBackground)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. Free Text Practice Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeTextScreen(viewModel: MorseViewModel, navController: NavController) {
    val isPlaying by viewModel.isPlayingAudio.collectAsState()
    var textInput by remember { mutableStateOf("CQ CQ CQ DE IN3XYZ IN3XYZ K") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.free_text_player), color = ThemeAmber, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAudio()
                        navController.navigate("dashboard")
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AudioVisualizer(isPlaying)

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text(stringResource(R.string.custom_text), color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemeAmber,
                    unfocusedBorderColor = ThemeSlate700,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.playTextDirectly(textInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ThemeBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.play_text), color = ThemeBackground)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { viewModel.stopAudio() },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.stop_playback), color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// 6. Statistics Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: MorseViewModel, navController: NavController) {
    val stats by viewModel.currentStats.collectAsState()
    val profile by viewModel.currentProfile.collectAsState()

    // Full 40 letters in Koch sequence
    val kochSeq = profile?.customSequence ?: UserProfile.DEFAULT_KOCH_SEQUENCE
    val activeCount = (profile?.currentKochLevel?.let { it + 1 }) ?: 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_stats), color = ThemeAmber, fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.char_mastery), color = ThemeAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.colors_reflect_accuracy), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

            val statsMap = stats.associateBy { it.character }

            // 40 char grid layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(kochSeq.length) { index ->
                    val char = kochSeq[index].toString()
                    val isUnlocked = index < activeCount
                    val stat = statsMap[char]

                    val (bgColor, textColor) = when {
                        !isUnlocked -> Pair(ThemeSlate700.copy(alpha = 0.3f), Color.DarkGray)
                        stat == null || stat.attempts == 0 -> Pair(ThemeSurface, Color.White)
                        else -> {
                            val acc = stat.correct.toFloat() / stat.attempts
                            when {
                                acc >= 0.90f -> Pair(ThemeGreen.copy(alpha = 0.7f), Color.White)
                                acc >= 0.75f -> Pair(ThemeAmber.copy(alpha = 0.7f), ThemeBackground)
                                else -> Pair(ThemeRed.copy(alpha = 0.7f), Color.White)
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, if (isUnlocked) ThemeAmber.copy(alpha = 0.3f) else Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(char, color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                if (isUnlocked && stat != null) {
                                    Text("${stat.correct}/${stat.attempts}", color = textColor.copy(alpha = 0.8f), fontSize = 10.sp)
                                } else if (isUnlocked) {
                                    Text("0/0", color = textColor.copy(alpha = 0.5f), fontSize = 10.sp)
                                } else {
                                    Text(stringResource(R.string.locked), color = textColor, fontSize = 9.sp)
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
// 7. Configuration Settings Screen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MorseViewModel, navController: NavController) {
    val profile by viewModel.currentProfile.collectAsState()

    if (profile == null) return

    var wpm by remember { mutableStateOf(profile!!.speedWpm.toFloat()) }
    var effectiveWpm by remember { mutableStateOf(profile!!.effectiveSpeedWpm.toFloat()) }
    var frequency by remember { mutableStateOf(profile!!.toneFrequencyHz.toFloat()) }
    var volume by remember { mutableStateOf(profile!!.volume.toFloat()) }
    var audioProfileState by remember { mutableStateOf(profile!!.audioProfile) }
    var isExpanded by remember { mutableStateOf(false) }
    var preferredLanguageState by remember { mutableStateOf(profile!!.preferredLanguage) }
    var isLangExpanded by remember { mutableStateOf(false) }
    var kochWindowSize by remember { mutableStateOf(profile!!.kochWindowSize.toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), color = ThemeAmber, fontWeight = FontWeight.Bold) },
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
            // Speed Configurations
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ThemeSurface), modifier = Modifier.fillMaxWidth().border(1.dp, ThemeSlate700, RoundedCornerShape(8.dp))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.speed_params), color = ThemeAmber, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.char_speed, wpm.toInt()), color = Color.White)
                        Slider(
                            value = wpm,
                            onValueChange = {
                                wpm = it
                                if (effectiveWpm > wpm) {
                                    effectiveWpm = wpm
                                }
                            },
                            valueRange = 5f..40f,
                            colors = SliderDefaults.colors(thumbColor = ThemeAmber, activeTrackColor = ThemeAmber)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.farnsworth_effective, effectiveWpm.toInt()), color = Color.White)
                        Slider(
                            value = effectiveWpm,
                            onValueChange = { effectiveWpm = it.coerceAtMost(wpm) },
                            valueRange = 5f..40f,
                            colors = SliderDefaults.colors(thumbColor = ThemeAmber, activeTrackColor = ThemeAmber)
                        )
                    }
                }
            }

            // Koch attempts per level config
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ThemeSurface), modifier = Modifier.fillMaxWidth().border(1.dp, ThemeSlate700, RoundedCornerShape(8.dp))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.koch_attempts_title), color = ThemeAmber, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.attempts_per_level, kochWindowSize.toInt()), color = Color.White)
                        Slider(
                            value = kochWindowSize,
                            onValueChange = { kochWindowSize = it },
                            valueRange = 5f..50f,
                            colors = SliderDefaults.colors(thumbColor = ThemeAmber, activeTrackColor = ThemeAmber)
                        )
                    }
                }
            }

            // Audio settings
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ThemeSurface), modifier = Modifier.fillMaxWidth().border(1.dp, ThemeSlate700, RoundedCornerShape(8.dp))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.audio_params), color = ThemeAmber, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.tone_pitch, frequency.toInt()), color = Color.White)
                        Slider(
                            value = frequency,
                            onValueChange = { frequency = it },
                            valueRange = 300f..1000f,
                            colors = SliderDefaults.colors(thumbColor = ThemeAmber, activeTrackColor = ThemeAmber)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.volume_level, volume.toInt()), color = Color.White)
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = ThemeAmber, activeTrackColor = ThemeAmber)
                        )
                    }
                }
            }

            // Sound Simulation Profile
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ThemeSurface), modifier = Modifier.fillMaxWidth().border(1.dp, ThemeSlate700, RoundedCornerShape(8.dp))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.transceiver_profile), color = ThemeAmber, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = when (audioProfileState) {
                                    "SINE" -> stringResource(R.string.pure_sine)
                                    "NOISY" -> stringResource(R.string.qrm_sim)
                                    "FADING" -> stringResource(R.string.qsb_sim)
                                    else -> stringResource(R.string.pure_sine)
                                },
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = true }
                                    .border(1.dp, ThemeSlate700, RoundedCornerShape(4.dp))
                                    .padding(12.dp)
                            )
                            DropdownMenu(
                                expanded = isExpanded,
                                onDismissRequest = { isExpanded = false },
                                modifier = Modifier.background(ThemeSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.pure_sine), color = Color.White) },
                                    onClick = { audioProfileState = "SINE"; isExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.qrm_sim), color = Color.White) },
                                    onClick = { audioProfileState = "NOISY"; isExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.qsb_sim), color = Color.White) },
                                    onClick = { audioProfileState = "FADING"; isExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // Language Override Settings
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ThemeSurface), modifier = Modifier.fillMaxWidth().border(1.dp, ThemeSlate700, RoundedCornerShape(8.dp))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.language_settings), color = ThemeAmber, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = when (preferredLanguageState) {
                                    "SYSTEM" -> stringResource(R.string.lang_system)
                                    "en" -> stringResource(R.string.lang_en)
                                    "de" -> stringResource(R.string.lang_de)
                                    "fr" -> stringResource(R.string.lang_fr)
                                    "es" -> stringResource(R.string.lang_es)
                                    else -> stringResource(R.string.lang_system)
                                },
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isLangExpanded = true }
                                    .border(1.dp, ThemeSlate700, RoundedCornerShape(4.dp))
                                    .padding(12.dp)
                            )
                            DropdownMenu(
                                expanded = isLangExpanded,
                                onDismissRequest = { isLangExpanded = false },
                                modifier = Modifier.background(ThemeSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.lang_system), color = Color.White) },
                                    onClick = { preferredLanguageState = "SYSTEM"; isLangExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.lang_en), color = Color.White) },
                                    onClick = { preferredLanguageState = "en"; isLangExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.lang_de), color = Color.White) },
                                    onClick = { preferredLanguageState = "de"; isLangExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.lang_fr), color = Color.White) },
                                    onClick = { preferredLanguageState = "fr"; isLangExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.lang_es), color = Color.White) },
                                    onClick = { preferredLanguageState = "es"; isLangExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // Save Actions
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.updateProfileSettings(
                                wpm = wpm.toInt(),
                                effectiveWpm = effectiveWpm.toInt(),
                                frequency = frequency.toInt(),
                                volume = volume.toInt(),
                                audioProfile = audioProfileState,
                                preferredLanguage = preferredLanguageState,
                                kochWindowSize = kochWindowSize.toInt()
                            )
                            navController.navigate("dashboard")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save_configs), color = ThemeBackground)
                    }
                }
            }
        }
    }
}
