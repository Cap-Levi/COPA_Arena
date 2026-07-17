package com.copaarena.app.ui.screen.tournament

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.domain.model.TournamentFormat
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFormatScreen(
    navController: NavController,
    sharedViewModel: CreateTournamentViewModel
) {
    val players by sharedViewModel.players.collectAsStateWithLifecycle()
    val selectedFormat by sharedViewModel.selectedFormat.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (selectedFormat == null) {
            sharedViewModel.setFormat(sharedViewModel.suggestFormat())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SELECT FORMAT",
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
                .padding(24.dp)
        ) {
            Text(
                "Choose format for ${players.size} players",
                style = MaterialTheme.typography.bodyLarge,
                color = OnBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            val allFormats = listOf(
                TournamentFormat.ROUND_ROBIN to "Round Robin (League)",
                TournamentFormat.SEMIFINALS to "Group Stage + Semifinals",
                TournamentFormat.QUARTERFINALS to "Group Stage + Quarterfinals"
            )

            val formats = if (players.size < 7) allFormats.take(2) else allFormats

            formats.forEach { (format, label) ->
                FormatCard(
                    format = format,
                    label = label,
                    isSelected = selectedFormat == format,
                    onClick = { sharedViewModel.setFormat(format) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Matches per fixture
            Text(
                "MATCHES PER FIXTURE",
                style = MaterialTheme.typography.headlineSmall,
                color = OnBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val matchesPerFixture by sharedViewModel.matchesPerFixture.collectAsStateWithLifecycle()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 4, 5).forEach { count ->
                    val isSelected = matchesPerFixture == count
                    Button(
                        onClick = { sharedViewModel.setMatchesPerFixture(count) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) AccentGold else Surface,
                            contentColor = if (isSelected) OnPrimary else OnBackground.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            count.toString(),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate(Screen.BracketPreview.createRoute(0L)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                Text("Preview Bracket", color = OnPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FormatCard(format: TournamentFormat, label: String, isSelected: Boolean, onClick: () -> Unit) {
    CopaCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentGold else SurfaceVariant,
                shape = MaterialTheme.shapes.large
            ),
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.titleMedium,
            color = if (isSelected) AccentGold else OnBackground
        )
    }
}
