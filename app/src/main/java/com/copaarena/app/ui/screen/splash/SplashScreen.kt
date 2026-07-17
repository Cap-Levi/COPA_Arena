package com.copaarena.app.ui.screen.splash

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.ui.components.LottieView
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.AccentGold
import com.copaarena.app.ui.theme.Background
import com.copaarena.app.ui.theme.BebasNeue
import com.copaarena.app.ui.theme.DmSans
import com.copaarena.app.ui.theme.OnBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: InitialSyncViewModel = hiltViewModel()
) {
    val syncComplete by viewModel.syncComplete.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        viewModel.performInitialSyncIfNeeded()
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(800))
    val titleScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splashTitleScale"
    )

    LaunchedEffect(syncComplete) {
        if (syncComplete) {
            delay(1500)
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            // Animated hero — the bouncing ball IS the splash identity now, no static icon.
            LottieView(
                assetName = "ball_bounce.json",
                modifier = Modifier
                    .size(140.dp)
                    .scale(titleScale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title (Orbitron is noticeably wider per-character than Bebas Neue was, so this
            // is sized down from the original 52.sp/4.sp to reliably fit "COPA ARENA" on
            // narrower screens without overflowing past the screen edges).
            Text(
                text = "COPA ARENA",
                fontFamily = BebasNeue,
                fontSize = 38.sp,
                fontWeight = FontWeight.Normal,
                color = AccentGold,
                letterSpacing = 2.sp,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(titleScale),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline in DM Sans
            Text(
                text = "Your pitch. Your rules.",
                fontFamily = DmSans,
                fontSize = 14.sp,
                color = OnBackground.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )

            if (isSyncing) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    color = AccentGold,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Syncing database…",
                    fontFamily = DmSans,
                    color = OnBackground.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
