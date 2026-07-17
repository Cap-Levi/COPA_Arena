package com.copaarena.app.ui.screen.ceremony

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.copaarena.app.ui.components.CelebrationOverlay
import com.copaarena.app.ui.components.LottieView
import com.copaarena.app.ui.components.TeamBadge
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.AccentGold
import com.copaarena.app.ui.theme.OnPrimary
import com.copaarena.app.ui.theme.OverlayScrim
import com.copaarena.app.ui.theme.Primary
import kotlinx.coroutines.delay

@Composable
fun CeremonyScreen(
    navController: NavController,
    viewModel: CeremonyViewModel = hiltViewModel()
) {
    val tournament by viewModel.tournament.collectAsStateWithLifecycle()
    val champion by viewModel.champion.collectAsStateWithLifecycle()
    val mvp by viewModel.mvp.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1200) // Reveal name
        step = 1
        delay(400) // Reveal badge
        step = 2
        delay(400) // "Champion" label
        step = 3
        delay(400) // MVP
        step = 4
        delay(400) // Standings
        step = 5
        delay(400) // Buttons
        step = 6
    }

    LaunchedEffect(step) {
        if (step == 3) {
            viewModel.playChampionFanfare()
            showConfetti = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Primary)) {
        // Fallback or Lottie if we had one
        // Try-catch Lottie load as specified in Plan.md
        var lottieFailed by remember { mutableStateOf(false) }
        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/trophy_reveal.json"))
        
        if (composition != null && !lottieFailed) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.fillMaxSize().alpha(0.5f)
            )
        } else {
            // Fallback just dark background
        }

        // Ambient crowd, looping quietly for the whole ceremony
        LottieView(
            assetName = "crowd_wave.json",
            modifier = Modifier.fillMaxSize().alpha(0.15f),
            iterations = LottieConstants.IterateForever
        )

        // One-shot confetti burst timed with the champion reveal
        CelebrationOverlay(
            visible = showConfetti,
            modifier = Modifier.fillMaxSize(),
            durationMillis = 1500L,
            onFinished = { showConfetti = false }
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(visible = step >= 1, enter = fadeIn(tween(600))) {
                Text(champion?.name ?: "", fontSize = 64.sp, color = AccentGold, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = step >= 2, enter = fadeIn(tween(600))) {
                TeamBadge(
                    badgeUrl = champion?.teamBadgeUrl,
                    teamName = champion?.teamName ?: champion?.name ?: "",
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(OnPrimary),
                    size = 128.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = step >= 3, enter = fadeIn(tween(600))) {
                Text("TOURNAMENT CHAMPION", style = MaterialTheme.typography.titleLarge, color = OnPrimary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(visible = step >= 4, enter = fadeIn(tween(600))) {
                Card(colors = CardDefaults.cardColors(containerColor = OverlayScrim)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ MVP: ", color = AccentGold)
                        Text(mvp?.name ?: "", color = OnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            AnimatedVisibility(visible = step >= 6, enter = fadeIn(tween(600))) {
                Column {
                    Button(
                        onClick = { viewModel.shareCard() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                    ) {
                        Text("Share Card")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.Home.route) { popUpTo(0) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finish", color = OnPrimary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            tournament?.let { t ->
                                navController.navigate(Screen.CreateTournament.createRoute(t.id)) {
                                    popUpTo(0)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OnPrimary)
                    ) {
                        Text("Restart Tournament", color = Primary)
                    }
                }
            }
        }
    }
}

