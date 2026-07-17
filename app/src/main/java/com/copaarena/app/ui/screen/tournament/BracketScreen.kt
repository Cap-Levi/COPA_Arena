package com.copaarena.app.ui.screen.tournament

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.data.db.entity.MatchEntity
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.domain.model.Standing
import com.copaarena.app.domain.model.Stage
import com.copaarena.app.ui.components.ActiveTournamentSwitcher
import com.copaarena.app.ui.components.CopaBottomNavigationBar
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BracketScreen(
    navController: NavController,
    viewModel: BracketViewModel = hiltViewModel()
) {
    val tournament by viewModel.tournament.collectAsStateWithLifecycle()
    val allTournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val standings by viewModel.standings.collectAsStateWithLifecycle()
    val tips by viewModel.qualificationTips.collectAsStateWithLifecycle()
    val skippableMatchIds by viewModel.skippableMatchIds.collectAsStateWithLifecycle()
    val nextMatchId by viewModel.nextMatchId.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Matches", "Standings", "Tips")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            (tournament?.name ?: "Bracket").uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = AccentGold
                        )
                        Text(
                            tournament?.format ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBackground.copy(alpha = 0.5f)
                        )
                    }
                },
                actions = {
                    ActiveTournamentSwitcher(
                        activeTournaments = allTournaments,
                        currentTournamentId = tournament?.id,
                        onSwitch = { id ->
                            navController.navigate(Screen.Bracket.createRoute(id)) {
                                popUpTo(Screen.Bracket.route) { inclusive = true }
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
        if (tournament == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AccentGold.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "NO ACTIVE TOURNAMENT",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (allTournaments.isNotEmpty())
                            "Use the switcher above to view an old tournament's bracket and scores"
                        else
                            "Start a tournament from Home to see its bracket here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Tabs
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
                    0 -> MatchesTab(matches, players, skippableMatchIds, nextMatchId, navController) { viewModel.skipMatch(it) }
                    1 -> StandingsTab(standings, players)
                    2 -> TipsTab(tips, players)
                }
            }
        }
    }
}

@Composable
private fun MatchesTab(
    matches: List<MatchEntity>,
    players: List<PlayerEntity>,
    skippableMatchIds: Set<Long>,
    nextMatchId: Long?,
    navController: NavController,
    onSkip: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val groupMatches = matches.filter { it.stage == Stage.GROUP.name }
        val koMatches = matches.filter { it.stage != Stage.GROUP.name }

        item {
            Text(
                "GROUP STAGE",
                style = MaterialTheme.typography.headlineSmall,
                color = AccentGold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(groupMatches) { match ->
            val isDecided = match.status == "COMPLETED" || match.status == "SKIPPED"
            val isSeeded = match.playerAId >= 0 && match.playerBId >= 0
            val isLocked = isSeeded && !isDecided && match.id != nextMatchId
            Column {
                MatchCard(match = match, players = players, isLocked = isLocked) {
                    navController.navigate(Screen.Match.createRoute(match.id))
                }
                if (match.id in skippableMatchIds) {
                    TextButton(onClick = { onSkip(match.id) }, modifier = Modifier.align(Alignment.End)) {
                        Text("Skip — already decided", color = AccentGold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (koMatches.isNotEmpty()) {
            item {
                Text(
                    "KNOCKOUTS",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AccentGold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            items(koMatches) { match ->
                val isDecided = match.status == "COMPLETED" || match.status == "SKIPPED"
                val isLocked = !isDecided && match.id != nextMatchId
                MatchCard(match = match, players = players, isLocked = isLocked) {
                    navController.navigate(Screen.Match.createRoute(match.id))
                }
            }
        }
    }
}

@Composable
private fun StandingsTab(standings: List<Standing>, players: List<PlayerEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(20.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = AccentGold)
                Text("Player", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = AccentGold)
                Text("P", modifier = Modifier.width(20.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = OnBackground.copy(0.7f), textAlign = TextAlign.Center)
                Text("W", modifier = Modifier.width(20.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = OnBackground.copy(0.7f), textAlign = TextAlign.Center)
                Text("D", modifier = Modifier.width(20.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = OnBackground.copy(0.7f), textAlign = TextAlign.Center)
                Text("L", modifier = Modifier.width(20.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = OnBackground.copy(0.7f), textAlign = TextAlign.Center)
                Text("GF", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = OnBackground.copy(0.7f), textAlign = TextAlign.Center)
                Text("GA", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = OnBackground.copy(0.7f), textAlign = TextAlign.Center)
                Text("GD", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = OnBackground.copy(0.7f), textAlign = TextAlign.Center)
                Text("Pts", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = AccentGold, textAlign = TextAlign.Center)
            }
        }

        // Data rows
        itemsIndexed(standings) { index, standing ->
            val player = players.find { it.id == standing.playerId }
            val bgColor by animateColorAsState(
                when {
                    standing.isQualified -> SuccessColor.copy(alpha = 0.1f)
                    standing.isEliminated -> ErrorColor.copy(alpha = 0.1f)
                    else -> if (index % 2 == 0) Surface else Color.Transparent
                },
                label = "rowBg"
            )

            val isLast = index == standings.size - 1
            val shape = if (isLast) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(0.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor, shape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    standing.rank.toString(),
                    modifier = Modifier.width(20.dp),
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = OnBackground
                )
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            player?.name ?: "Unknown",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = OnBackground
                        )
                        Text(
                            player?.teamName ?: "-",
                            fontSize = 10.sp,
                            color = OnBackground.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (standing.isQualified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(containerColor = SuccessColor) { Text("Q", fontSize = 9.sp) }
                    } else if (standing.isEliminated) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(containerColor = ErrorColor) { Text("E", fontSize = 9.sp) }
                    }
                }
                Text(standing.matchesPlayed.toString(), modifier = Modifier.width(20.dp), fontFamily = JetBrainsMono, fontSize = 11.sp, textAlign = TextAlign.Center, color = OnBackground)
                Text(standing.wins.toString(), modifier = Modifier.width(20.dp), fontFamily = JetBrainsMono, fontSize = 11.sp, textAlign = TextAlign.Center, color = OnBackground)
                Text(standing.draws.toString(), modifier = Modifier.width(20.dp), fontFamily = JetBrainsMono, fontSize = 11.sp, textAlign = TextAlign.Center, color = OnBackground)
                Text(standing.losses.toString(), modifier = Modifier.width(20.dp), fontFamily = JetBrainsMono, fontSize = 11.sp, textAlign = TextAlign.Center, color = OnBackground)
                Text(standing.goalsFor.toString(), modifier = Modifier.width(24.dp), fontFamily = JetBrainsMono, fontSize = 11.sp, textAlign = TextAlign.Center, color = OnBackground)
                Text(standing.goalsAgainst.toString(), modifier = Modifier.width(24.dp), fontFamily = JetBrainsMono, fontSize = 11.sp, textAlign = TextAlign.Center, color = OnBackground)
                Text(
                    (if (standing.goalDifference > 0) "+" else "") + standing.goalDifference.toString(),
                    modifier = Modifier.width(28.dp),
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = when {
                        standing.goalDifference > 0 -> SuccessColor
                        standing.goalDifference < 0 -> ErrorColor
                        else -> OnBackground
                    }
                )
                Text(
                    standing.points.toString(),
                    modifier = Modifier.width(28.dp),
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = AccentGold
                )
            }
        }
    }
}

@Composable
private fun TipsTab(
    tips: List<com.copaarena.app.domain.model.QualificationTip>,
    players: List<PlayerEntity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tips) { tip ->
            val player = players.find { it.id == tip.playerId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        player?.name ?: "Unknown",
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(tip.tipText, color = OnBackground.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun MatchCard(match: MatchEntity, players: List<PlayerEntity>, isLocked: Boolean = false, onClick: () -> Unit) {
    val p1 = players.find { it.id == match.playerAId }
    val p2 = players.find { it.id == match.playerBId }
    val isCompleted = match.status == "COMPLETED"
    val isSeeded = match.playerAId >= 0 && match.playerBId >= 0

    CopaCard(
        modifier = Modifier.fillMaxWidth().alpha(if (isLocked) 0.55f else 1f),
        onClick = if (isSeeded && !isLocked) onClick else null
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val aWon = isCompleted && match.winnerId != null && match.winnerId == match.playerAId
            val bWon = isCompleted && match.winnerId != null && match.winnerId == match.playerBId
            val decidedByPenalties = match.penaltyGoalsA != null && match.penaltyGoalsB != null

            // Player A
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    p1?.name ?: "TBD",
                    fontWeight = FontWeight.Bold,
                    color = if (aWon) AccentGold else OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    p1?.teamName ?: "",
                    fontSize = 11.sp,
                    color = if (aWon) AccentGold.copy(alpha = 0.7f) else OnBackground.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${match.goalsA} - ${match.goalsB}",
                    fontFamily = BebasNeue,
                    fontSize = 24.sp,
                    color = if (isCompleted) AccentGold else OnBackground.copy(alpha = 0.3f)
                )
                if (decidedByPenalties) {
                    Text(
                        "PENS ${match.penaltyGoalsA} - ${match.penaltyGoalsB}",
                        fontSize = 9.sp,
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isCompleted && match.isDraw) {
                    Text("TIED", fontSize = 9.sp, color = OnBackground.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                } else if (isCompleted) {
                    Text("FT", fontSize = 9.sp, color = SuccessColor, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        match.status ?: "PENDING",
                        fontSize = 9.sp,
                        color = OnBackground.copy(alpha = 0.3f)
                    )
                }
            }

            // Player B
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    p2?.name ?: "TBD",
                    fontWeight = FontWeight.Bold,
                    color = if (bWon) AccentGold else OnBackground,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    p2?.teamName ?: "",
                    fontSize = 11.sp,
                    color = if (bWon) AccentGold.copy(alpha = 0.7f) else OnBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isLocked) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp).padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = OnBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Locked — finish earlier matches first",
                    fontSize = 10.sp,
                    color = OnBackground.copy(alpha = 0.4f)
                )
            }
        }
      }
    }
}
