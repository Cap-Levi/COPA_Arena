package com.copaarena.app.ui.screen.tournament

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BracketPreviewScreen(
    navController: NavController,
    sharedViewModel: CreateTournamentViewModel
) {
    val players by sharedViewModel.players.collectAsStateWithLifecycle()
    val format by sharedViewModel.selectedFormat.collectAsStateWithLifecycle()
    val matchesPerFixture by sharedViewModel.matchesPerFixture.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var isKickOffLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "BRACKET PREVIEW",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Summary card
            CopaCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Format", color = OnBackground.copy(alpha = 0.5f))
                        Text(
                            format?.name ?: "Unknown",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Players", color = OnBackground.copy(alpha = 0.5f))
                        Text(
                            "${players.size}",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Legs per Fixture", color = OnBackground.copy(alpha = 0.5f))
                        Text(
                            "$matchesPerFixture",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val infiniteTransition = rememberInfiniteTransition(label = "ballPulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ballPulseScale"
                    )
                    Icon(
                        Icons.Default.SportsSoccer,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .scale(pulseScale),
                        tint = AccentGold.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Ready to go!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        "Bracket will be generated on kick off",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBackground.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isKickOffLoading) return@Button
                    isKickOffLoading = true
                    scope.launch {
                        val tid = sharedViewModel.kickoff()
                        isKickOffLoading = false
                        navController.navigate(Screen.Bracket.createRoute(tid)) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                enabled = !isKickOffLoading
            ) {
                if (isKickOffLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = OnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "⚽ KICK OFF",
                        color = OnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BebasNeue,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    )
                }
            }
        }
    }
}
