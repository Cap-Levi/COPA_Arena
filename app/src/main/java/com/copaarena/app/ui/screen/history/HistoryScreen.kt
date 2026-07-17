package com.copaarena.app.ui.screen.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.ui.components.CopaBottomNavigationBar
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.components.StaggeredEntrance
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "HISTORY",
                        style = MaterialTheme.typography.headlineLarge,
                        color = AccentGold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = { CopaBottomNavigationBar(navController) },
        containerColor = Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No past tournaments",
                            style = MaterialTheme.typography.titleLarge,
                            color = OnBackground.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Your completed tournaments will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnBackground.copy(alpha = 0.25f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(history) { index, tournament ->
                      StaggeredEntrance(index = index) {
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val dateStr = formatter.format(Date(tournament.date))

                        CopaCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Screen.TournamentDetail.createRoute(tournament.id)) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            tournament.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = OnBackground
                                        )
                                        Text(
                                            "$dateStr • ${tournament.format}",
                                            color = OnBackground.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteTournament(tournament) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorColor)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (tournament.status == "COMPLETED") {
                                    Badge(containerColor = SuccessColor.copy(alpha = 0.2f), contentColor = SuccessColor) {
                                        Text("Completed", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                } else {
                                    Badge(containerColor = SurfaceVariant, contentColor = OnBackground.copy(alpha = 0.6f)) {
                                        Text(tournament.status, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                      }
                    }
                }
            }
        }
    }
}
