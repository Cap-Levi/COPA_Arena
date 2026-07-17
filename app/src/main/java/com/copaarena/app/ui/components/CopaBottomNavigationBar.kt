package com.copaarena.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.copaarena.app.data.repository.TournamentRepository
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.AccentGold
import com.copaarena.app.ui.theme.OnBackground
import com.copaarena.app.ui.theme.Surface
import com.copaarena.app.ui.theme.SurfaceVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BottomNavViewModel @Inject constructor(
    tournamentRepository: TournamentRepository
) : ViewModel() {
    val activeTournamentId: StateFlow<Long?> = tournamentRepository.getActiveTournament()
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val matchPrefix: Boolean = false
)

@Composable
fun CopaBottomNavigationBar(navController: NavController, viewModel: BottomNavViewModel = hiltViewModel()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val activeTournamentId by viewModel.activeTournamentId.collectAsStateWithLifecycle()

    val items = listOf(
        BottomNavItem("Home", Icons.Filled.Home, Screen.Home.route),
        BottomNavItem("Bracket", Icons.Filled.EmojiEvents, "bracket", matchPrefix = true),
        BottomNavItem("Stats", Icons.Filled.Leaderboard, "stats", matchPrefix = true),
        BottomNavItem("History", Icons.Filled.History, Screen.History.route)
    )

    NavigationBar(
        containerColor = Surface,
        contentColor = OnBackground
    ) {
        items.forEach { item ->
            val isSelected = if (item.matchPrefix) {
                currentRoute?.startsWith(item.route) == true
            } else {
                currentRoute == item.route
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        when {
                            item.route == Screen.Home.route -> navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                            item.route == "bracket" -> navController.navigate(Screen.Bracket.createRoute(activeTournamentId))
                            item.route == "stats" -> navController.navigate(Screen.Stats.createRoute(activeTournamentId))
                            else -> navController.navigate(item.route)
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) AccentGold else OnBackground.copy(alpha = 0.5f)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (isSelected) AccentGold else OnBackground.copy(alpha = 0.5f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = SurfaceVariant
                )
            )
        }
    }
}
