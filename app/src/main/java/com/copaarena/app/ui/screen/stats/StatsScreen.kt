package com.copaarena.app.ui.screen.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.data.db.dao.GlobalPlayerSummary
import com.copaarena.app.data.db.dao.TopScorerResult
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.ui.components.ActiveTournamentSwitcher
import com.copaarena.app.ui.components.CopaBottomNavigationBar
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.components.StaggeredEntrance
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController, viewModel: StatsViewModel = hiltViewModel()) {
    val topScorers by viewModel.topScorers.collectAsStateWithLifecycle()
    val globalStats by viewModel.globalStats.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val playerGoalCounts by viewModel.playerGoalCounts.collectAsStateWithLifecycle()
    val allTournaments by viewModel.allTournaments.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("This Tournament", "All Time")
    var selectedPlayer by remember { mutableStateOf<PlayerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "STATS",
                        style = MaterialTheme.typography.headlineLarge,
                        color = AccentGold
                    )
                },
                actions = {
                    ActiveTournamentSwitcher(
                        activeTournaments = allTournaments,
                        currentTournamentId = viewModel.tournamentId,
                        onSwitch = { id ->
                            navController.navigate(Screen.Stats.createRoute(id)) {
                                popUpTo(Screen.Stats.route) { inclusive = true }
                            }
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = { CopaBottomNavigationBar(navController) },
        containerColor = Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SurfaceVariant,
                contentColor = OnBackground,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = AccentGold
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTabIndex == index) AccentGold else OnBackground.copy(alpha = 0.5f),
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> if (topScorers.isEmpty() && players.isEmpty()) {
                    StatsEmptyState("No stats available", "Play a match in this tournament to see stats here")
                } else {
                    val goalsByPlayerId = remember(playerGoalCounts) { playerGoalCounts.associate { it.playerId to it.goals } }
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        item {
                            Text(
                                "TOP SCORERS",
                                style = MaterialTheme.typography.headlineSmall,
                                color = AccentGold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        itemsIndexed(topScorers) { index, scorer ->
                          StaggeredEntrance(index = index) {
                            val icon = when (index) {
                                0 -> "👑"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "${index + 1}."
                            }
                            CopaCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Text(icon, modifier = Modifier.width(32.dp))
                                        Text(
                                            scorer.playerName,
                                            fontWeight = FontWeight.Bold,
                                            color = OnBackground
                                        )
                                    }
                                    Text(
                                        "${scorer.goals}",
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold
                                    )
                                }
                            }
                          }
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                "GOALS OVERVIEW",
                                style = MaterialTheme.typography.headlineSmall,
                                color = AccentGold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            if (topScorers.isNotEmpty()) {
                                CopaCard(modifier = Modifier.fillMaxWidth()) {
                                    GoalsBarChart(
                                        scorers = topScorers,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        if (players.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    "PLAYERS",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = AccentGold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            itemsIndexed(players) { index, player ->
                              StaggeredEntrance(index = index) {
                                PlayerStatsCard(
                                    player = player,
                                    goals = goalsByPlayerId[player.id] ?: 0,
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    onClick = { selectedPlayer = player }
                                )
                              }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }

                1 -> if (globalStats.isEmpty()) {
                    StatsEmptyState("No stats available", "Play some matches to build the all-time leaderboard")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        item {
                            Text(
                                "GLOBAL LEADERBOARD",
                                style = MaterialTheme.typography.headlineSmall,
                                color = AccentGold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        itemsIndexed(globalStats) { index, stat ->
                          StaggeredEntrance(index = index) {
                            CopaCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${index + 1}.",
                                            modifier = Modifier.width(28.dp),
                                            fontFamily = JetBrainsMono,
                                            color = OnBackground.copy(alpha = 0.5f)
                                        )
                                        Column {
                                            Text(stat.playerName, fontWeight = FontWeight.Bold, color = OnBackground)
                                            Text(
                                                "Win Rate: ${if (stat.totalMatches > 0) stat.totalWins * 100 / stat.totalMatches else 0}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnBackground.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                    Text(
                                        "${stat.totalPoints} Pts",
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold
                                    )
                                }
                            }
                          }
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                "WIN RATE",
                                style = MaterialTheme.typography.headlineSmall,
                                color = AccentGold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            if (globalStats.isNotEmpty()) {
                                CopaCard(modifier = Modifier.fillMaxWidth()) {
                                    WinRateChart(
                                        stats = globalStats,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }

        val player = selectedPlayer
        if (player != null) {
            val goalsByMatch by remember(player.id) { viewModel.goalsByMatchForPlayer(player.id) }
                .collectAsStateWithLifecycle(initialValue = emptyList())
            ModalBottomSheet(
                onDismissRequest = { selectedPlayer = null },
                containerColor = Surface
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(player.name, style = MaterialTheme.typography.headlineSmall, color = AccentGold)
                    Text(
                        player.teamName,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val totalGoals = goalsByMatch.sumOf { it.goals }
                    Text(
                        "$totalGoals goal${if (totalGoals != 1) "s" else ""} this tournament",
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val relevantMatches = matches.filter { it.playerAId == player.id || it.playerBId == player.id }
                    if (relevantMatches.isEmpty()) {
                        Text("No matches played yet", color = OnBackground.copy(alpha = 0.4f))
                    } else {
                        relevantMatches.forEach { m ->
                            val opponentId = if (m.playerAId == player.id) m.playerBId else m.playerAId
                            val opponent = players.find { it.id == opponentId }
                            val goalsInMatch = goalsByMatch.find { it.matchId == m.id }?.goals ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("vs ${opponent?.name ?: "TBD"}", color = OnBackground)
                                Text(
                                    "$goalsInMatch ⚽",
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMono
                                )
                            }
                            HorizontalDivider(color = SurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatsEmptyState(
    title: String = "No stats available",
    subtitle: String = "Play a match to see stats here"
) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AccentGold.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = OnBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlayerStatsCard(
    player: PlayerEntity,
    goals: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CopaCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(AccentGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.name.firstOrNull()?.uppercase() ?: "?",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, fontWeight = FontWeight.Bold, color = OnBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    player.teamName,
                    fontSize = 11.sp,
                    color = OnBackground.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("$goals ⚽", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = AccentGold)
        }
    }
}

/** Animated, tappable bar chart — bars grow in staggered, tapping one pins its exact value. */
@Composable
private fun GoalsBarChart(scorers: List<TopScorerResult>, modifier: Modifier = Modifier) {
    val top = remember(scorers) { scorers.take(8) }
    val maxGoals = remember(top) { (top.maxOfOrNull { it.goals } ?: 1).coerceAtLeast(1) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val barColors = remember {
        listOf(AccentGold, SuccessColor, Color(0xFF4FC3F7), Color(0xFFBA68C8), Color(0xFFFF8A65))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        top.forEachIndexed { index, scorer ->
            val targetFraction = scorer.goals / maxGoals.toFloat()
            val animatedFraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 600, delayMillis = index * 80, easing = FastOutSlowInEasing),
                label = "barGrow"
            )
            val isSelected = selectedIndex == index
            val barColor = barColors[index % barColors.size]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedIndex = if (isSelected) null else index }
            ) {
                Text(
                    if (isSelected) "${scorer.goals}" else " ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.height(140.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight(animatedFraction.coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(listOf(barColor, barColor.copy(alpha = 0.45f)))
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    scorer.playerName.take(6),
                    fontSize = 10.sp,
                    color = OnBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Animated win-rate bars — tapping a row expands the raw win/match/points breakdown. */
@Composable
private fun WinRateChart(stats: List<GlobalPlayerSummary>, modifier: Modifier = Modifier) {
    val top = remember(stats) { stats.take(8) }
    var selectedName by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        top.forEachIndexed { index, stat ->
            val rate = if (stat.totalMatches > 0) stat.totalWins * 100f / stat.totalMatches else 0f
            val animatedRate by animateFloatAsState(
                targetValue = rate / 100f,
                animationSpec = tween(durationMillis = 700, delayMillis = index * 70, easing = FastOutSlowInEasing),
                label = "winRate"
            )
            val isSelected = selectedName == stat.playerName

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedName = if (isSelected) null else stat.playerName }
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stat.playerName, fontWeight = FontWeight.Bold, color = OnBackground, fontSize = 13.sp)
                    Text("${rate.toInt()}%", fontWeight = FontWeight.Bold, color = AccentGold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSelected) 14.dp else 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedRate.coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(AccentGold, SuccessColor)))
                    )
                }
                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${stat.totalWins}W in ${stat.totalMatches} matches • ${stat.totalPoints} pts",
                        fontSize = 11.sp,
                        color = OnBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
