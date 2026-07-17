package com.copaarena.app.ui.screen.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.components.StaggeredEntrance
import com.copaarena.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    navController: NavController,
    viewModel: TournamentDetailViewModel = hiltViewModel()
) {
    val tournament by viewModel.tournament.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val standings by viewModel.standings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TOURNAMENT DETAIL",
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
        containerColor = Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val t = tournament
            if (t == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = AccentGold)
            } else {
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val dateStr = formatter.format(Date(t.date))

                Text(
                    t.name,
                    // Page hero title, not a navbar header — kept at the old headlineLarge
                    // size explicitly so shrinking navbar headers doesn't shrink this too.
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 40.sp),
                    color = AccentGold
                )
                Text(
                    "$dateStr • ${t.format}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackground.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    t.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (t.status == "COMPLETED") SuccessColor else AccentGold
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "FINAL STANDINGS",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(standings) { index, stat ->
                      StaggeredEntrance(index = index) {
                        val player = players.find { it.id == stat.playerId }
                        CopaCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${stat.finalPosition}.",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = when (stat.finalPosition) {
                                        1 -> AccentGold
                                        else -> OnBackground.copy(alpha = 0.5f)
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(player?.name ?: "Unknown", fontWeight = FontWeight.Bold, color = OnBackground)
                                    Text(
                                        player?.teamName ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnBackground.copy(alpha = 0.3f)
                                    )
                                }
                                Text(
                                    "${stat.points} Pts",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                            }
                        }
                      }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        navController.navigate(com.copaarena.app.ui.navigation.Screen.CreateTournament.createRoute(t.id))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Restart Tournament", color = OnPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
