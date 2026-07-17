package com.copaarena.app.ui.screen.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(navController: NavController, viewModel: CreateTournamentViewModel) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val isRestartMode by viewModel.isRestartMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isRestartMode) "RESTART TOURNAMENT" else "NEW TOURNAMENT",
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
            Spacer(modifier = Modifier.height(32.dp))

            // Icon
            Icon(
                Icons.Default.SportsSoccer,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AccentGold.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Give your tournament a name",
                style = MaterialTheme.typography.bodyLarge,
                color = OnBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.setName(it) },
                label = { Text("Tournament Name") },
                placeholder = { Text("COPA MUNDIAL 2025") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = SurfaceVariant,
                    cursorColor = AccentGold,
                    focusedLabelColor = AccentGold,
                    unfocusedLabelColor = OnBackground.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate(Screen.AddPlayers.createRoute(0L)) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    disabledContainerColor = SurfaceVariant
                )
            ) {
                Text(
                    if (isRestartMode) "Next → Review Teams" else "Next → Add Players",
                    color = OnPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
