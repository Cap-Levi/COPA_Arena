package com.copaarena.app.ui.screen.match

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.data.db.entity.GoalEntity
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.ui.components.CelebrationOverlay
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.components.TeamBadge
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    navController: NavController,
    viewModel: MatchViewModel = hiltViewModel()
) {
    val match by viewModel.match.collectAsStateWithLifecycle()
    val pA by viewModel.playerA.collectAsStateWithLifecycle()
    val pB by viewModel.playerB.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val fifaPlayersA by viewModel.fifaPlayersA.collectAsStateWithLifecycle()
    val fifaPlayersB by viewModel.fifaPlayersB.collectAsStateWithLifecycle()
    val needsTieDecision by viewModel.needsTieDecision.collectAsStateWithLifecycle()
    val needsPenalties by viewModel.needsPenalties.collectAsStateWithLifecycle()
    val ceremonyTournamentId by viewModel.navigateToCeremony.collectAsStateWithLifecycle()
    val celebrateGoal by viewModel.celebrateGoal.collectAsStateWithLifecycle()
    val celebrateFullTime by viewModel.celebrateFullTime.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticEnabled.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val triggerHaptic = {
        if (hapticEnabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }

    var showGoalSheet by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(ceremonyTournamentId) {
        ceremonyTournamentId?.let { tid ->
            navController.navigate(Screen.Ceremony.createRoute(tid)) {
                popUpTo(Screen.Home.route)
            }
            viewModel.consumeCeremonyNavigation()
        }
    }

    val scoreA = goals.count { it.creditedToId == match?.playerAId }
    val scoreB = goals.count { it.creditedToId == match?.playerBId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MATCH CENTER",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AccentGold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (match?.status != "COMPLETED") {
                FloatingActionButton(
                    onClick = { showGoalSheet = true },
                    containerColor = AccentGold,
                    contentColor = OnPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal")
                }
            }
        },
        containerColor = Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Scoreboard ──
            CopaCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(SurfaceVariant, Surface)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerMatchCard(
                            player = pA,
                            modifier = Modifier.weight(1f),
                            isWinner = match?.status == "COMPLETED" && match?.winnerId != null && match?.winnerId == match?.playerAId
                        )
                        val totalGoals = scoreA + scoreB
                        val scorePunch = remember { Animatable(1f) }
                        LaunchedEffect(totalGoals) {
                            if (totalGoals > 0) {
                                scorePunch.snapTo(1f)
                                scorePunch.animateTo(1.3f, animationSpec = tween(120))
                                scorePunch.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                        Text(
                            text = "$scoreA - $scoreB",
                            style = com.copaarena.app.ui.theme.numericTextStyle(56.sp, emphasized = true),
                            color = AccentGold,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .scale(scorePunch.value)
                        )
                        PlayerMatchCard(
                            player = pB,
                            modifier = Modifier.weight(1f),
                            isWinner = match?.status == "COMPLETED" && match?.winnerId != null && match?.winnerId == match?.playerBId
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Status / Confirm ──
            if (needsTieDecision) {
                TieDecisionPrompt(
                    onDecline = {
                        scope.launch {
                            triggerHaptic()
                            viewModel.confirmMatch(acceptDraw = true)
                        }
                    },
                    onAccept = { viewModel.choosePenalties() }
                )
            } else if (needsPenalties) {
                PenaltyShootoutEntry(
                    playerAName = pA?.name ?: "Player A",
                    playerBName = pB?.name ?: "Player B",
                    onSubmit = { penA, penB ->
                        scope.launch {
                            triggerHaptic()
                            viewModel.confirmMatch(penA, penB)
                        }
                    }
                )
            } else if (match?.status == "COMPLETED") {
                Text(
                    "FULL TIME",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SuccessColor
                )
                val m = match
                if (m != null && m.penaltyGoalsA != null && m.penaltyGoalsB != null) {
                    val winnerName = if (m.winnerId == m.playerAId) pA?.name else pB?.name
                    Text(
                        "${winnerName ?: "Winner"} won on penalties ${m.penaltyGoalsA}-${m.penaltyGoalsB}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGold
                    )
                } else if (m?.isDraw == true) {
                    Text(
                        "Match tied",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnBackground.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Bracket", color = OnBackground)
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            triggerHaptic()
                            viewModel.confirmMatch()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Confirm Full Time", color = OnPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Goal Log Header ──
            Text(
                "GOAL LOG",
                style = MaterialTheme.typography.headlineSmall,
                color = OnBackground.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // ── Goal Log List ──
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(goals) { goal ->
                    val scorer = if (goal.scorerId == pA?.id) pA else pB
                    CopaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = { goalToDelete = goal })
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Player initial circle
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "⚽",
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    scorer?.name ?: "Unknown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = OnBackground
                                )
                                if (goal.fifaPlayerName != null) {
                                    Text(
                                        goal.fifaPlayerName,
                                        fontSize = 11.sp,
                                        color = AccentGold.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (goal.isOwnGoal) {
                                Badge(containerColor = ErrorColor) {
                                    Text("OG", fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Goal celebration (small, near scoreboard) ──
        CelebrationOverlay(
            visible = celebrateGoal,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .size(220.dp),
            durationMillis = 700L,
            onFinished = { viewModel.consumeGoalCelebration() }
        )

        // ── Full-time celebration (bigger, fullscreen) ──
        // confetti.json is a genuine ~10s cannon-burst-and-fall animation — cutting it off
        // after just over a second (the old 1200ms) only ever showed the initial launch frame.
        // Let it play out a real chunk of the fall before dismissing.
        CelebrationOverlay(
            visible = celebrateFullTime,
            modifier = Modifier.fillMaxSize(),
            durationMillis = 4000L,
            onFinished = { viewModel.consumeFullTimeCelebration() }
        )

        // ── Goal Sheet ──
        if (showGoalSheet) {
            ModalBottomSheet(
                onDismissRequest = { showGoalSheet = false },
                sheetState = sheetState,
                containerColor = Surface
            ) {
                var selectedScorerTab by remember { mutableStateOf(0) }
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)
                ) {
                    Text(
                        "SELECT SCORER",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AccentGold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TabRow(
                        selectedTabIndex = selectedScorerTab,
                        containerColor = SurfaceVariant,
                        contentColor = OnBackground,
                        indicator = { tabPositions ->
                            if (selectedScorerTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedScorerTab]),
                                    color = AccentGold
                                )
                            }
                        }
                    ) {
                        Tab(selected = selectedScorerTab == 0, onClick = { selectedScorerTab = 0 }) {
                            Text(
                                pA?.name ?: "Player A",
                                modifier = Modifier.padding(16.dp),
                                color = if (selectedScorerTab == 0) AccentGold else OnBackground.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Tab(selected = selectedScorerTab == 1, onClick = { selectedScorerTab = 1 }) {
                            Text(
                                pB?.name ?: "Player B",
                                modifier = Modifier.padding(16.dp),
                                color = if (selectedScorerTab == 1) AccentGold else OnBackground.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val currentPlayer = if (selectedScorerTab == 0) pA else pB
                    val currentFifaPlayers = if (selectedScorerTab == 0) fifaPlayersA else fifaPlayersB

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (currentFifaPlayers.isEmpty()) {
                            item {
                                ListItem(
                                    headlineContent = {
                                        Text("Log Goal for ${currentPlayer?.name ?: "Player"}", color = OnBackground, fontWeight = FontWeight.Bold)
                                    },
                                    supportingContent = {
                                        Text("No FIFA roster linked to this team", fontSize = 11.sp, color = OnBackground.copy(alpha = 0.4f))
                                    },
                                    modifier = Modifier.clickable {
                                        triggerHaptic()
                                        viewModel.addGoal(currentPlayer?.id ?: 0L, false, null)
                                        showGoalSheet = false
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                                HorizontalDivider(color = SurfaceVariant)
                            }
                        }
                        items(currentFifaPlayers) { fifaPlayer ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        fifaPlayer.shortName ?: fifaPlayer.longName ?: "Unknown",
                                        color = OnBackground
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        fifaPlayer.playerPositions ?: "",
                                        fontSize = 11.sp,
                                        color = OnBackground.copy(alpha = 0.4f)
                                    )
                                },
                                trailingContent = {
                                    Text(
                                        "${fifaPlayer.overall ?: "?"}",
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold
                                    )
                                },
                                modifier = Modifier.clickable {
                                    triggerHaptic()
                                    viewModel.addGoal(
                                        currentPlayer?.id ?: 0L,
                                        false,
                                        fifaPlayer.shortName ?: fifaPlayer.longName
                                    )
                                    showGoalSheet = false
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            HorizontalDivider(color = SurfaceVariant)
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    triggerHaptic()
                                    viewModel.addGoal(currentPlayer?.id ?: 0L, true, "Own Goal")
                                    showGoalSheet = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor)
                            ) {
                                Text("Record as Own Goal (OG)")
                            }
                        }
                    }
                }
            }
        }

        // ── Delete Confirm ──
        if (goalToDelete != null) {
            AlertDialog(
                onDismissRequest = { goalToDelete = null },
                title = { Text("Delete Goal", color = OnBackground) },
                text = { Text("Are you sure you want to delete this goal?", color = OnBackground.copy(alpha = 0.7f)) },
                containerColor = Surface,
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteGoal(goalToDelete!!)
                        goalToDelete = null
                    }) { Text("Delete", color = ErrorColor) }
                },
                dismissButton = {
                    TextButton(onClick = { goalToDelete = null }) { Text("Cancel", color = OnBackground) }
                }
            )
        }
        }
    }
}

@Composable
fun PlayerMatchCard(player: PlayerEntity?, modifier: Modifier = Modifier, isWinner: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        TeamBadge(
            badgeUrl = player?.teamBadgeUrl,
            teamName = player?.teamName ?: player?.name ?: "?",
            size = 48.dp,
            backgroundColor = SurfaceVariant,
            textColor = AccentGold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            player?.name ?: "TBD",
            fontFamily = BebasNeue,
            fontSize = 18.sp,
            color = if (isWinner) AccentGold else OnBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            player?.teamName ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = if (isWinner) AccentGold.copy(alpha = 0.7f) else OnBackground.copy(alpha = 0.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TieDecisionPrompt(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    CopaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "MATCH ENDED LEVEL",
                style = MaterialTheme.typography.titleMedium,
                color = AccentGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Was this decided by a penalty shootout?",
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("No — Tie", color = OnBackground)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Yes — Penalties", color = OnPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PenaltyShootoutEntry(
    playerAName: String,
    playerBName: String,
    onSubmit: (penA: Int, penB: Int) -> Unit
) {
    var penA by remember { mutableStateOf(0) }
    var penB by remember { mutableStateOf(0) }

    CopaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "STILL LEVEL — PENALTY SHOOTOUT",
                style = MaterialTheme.typography.titleMedium,
                color = AccentGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PenaltyStepper(label = playerAName, value = penA, onChange = { penA = it })
                Text("-", fontSize = 24.sp, color = OnBackground)
                PenaltyStepper(label = playerBName, value = penB, onChange = { penB = it })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSubmit(penA, penB) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                enabled = penA != penB
            ) {
                Text("Confirm Shootout Result", color = OnPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PenaltyStepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = OnBackground.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > 0) onChange(value - 1) }) {
                Text("-", fontSize = 20.sp, color = OnBackground)
            }
            Text(value.toString(), fontFamily = BebasNeue, fontSize = 28.sp, color = AccentGold)
            IconButton(onClick = { onChange(value + 1) }) {
                Text("+", fontSize = 20.sp, color = OnBackground)
            }
        }
    }
}
