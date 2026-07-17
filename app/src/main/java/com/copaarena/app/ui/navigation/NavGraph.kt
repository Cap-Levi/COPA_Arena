package com.copaarena.app.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

import com.copaarena.app.ui.screen.splash.SplashScreen
import com.copaarena.app.ui.screen.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { 
            slideInHorizontally(
                initialOffsetX = { 1000 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = { 
            slideOutHorizontally(
                targetOffsetX = { -1000 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = { 
            slideInHorizontally(
                initialOffsetX = { -1000 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = { 
            slideOutHorizontally(
                targetOffsetX = { 1000 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(
            route = Screen.CreateTournament.route,
            arguments = listOf(navArgument("restartId") { 
                type = NavType.StringType 
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.copaarena.app.ui.screen.tournament.CreateTournamentViewModel>()
            val restartId = backStackEntry.arguments?.getString("restartId")?.toLongOrNull()
            
            androidx.compose.runtime.LaunchedEffect(restartId) {
                if (restartId != null) {
                    viewModel.loadPlayersForRestart(restartId)
                }
            }
            
            com.copaarena.app.ui.screen.tournament.CreateTournamentScreen(navController, viewModel) 
        }
        
        composable(
            route = Screen.AddPlayers.route,
            arguments = listOf(navArgument("tournamentId") { type = NavType.LongType })
        ) { backStackEntry -> 
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.CreateTournament.route)
            }
            val viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.copaarena.app.ui.screen.tournament.CreateTournamentViewModel>(parentEntry)
            com.copaarena.app.ui.screen.tournament.AddPlayersScreen(navController, viewModel)
        }
        
        composable(
            route = Screen.SelectFormat.route,
            arguments = listOf(navArgument("tournamentId") { type = NavType.LongType })
        ) { backStackEntry -> 
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.CreateTournament.route)
            }
            val viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.copaarena.app.ui.screen.tournament.CreateTournamentViewModel>(parentEntry)
            com.copaarena.app.ui.screen.tournament.SelectFormatScreen(navController, viewModel)
        }
        
        composable(
            route = Screen.BracketPreview.route,
            arguments = listOf(navArgument("tournamentId") { type = NavType.LongType })
        ) { backStackEntry -> 
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.CreateTournament.route)
            }
            val viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.copaarena.app.ui.screen.tournament.CreateTournamentViewModel>(parentEntry)
            com.copaarena.app.ui.screen.tournament.BracketPreviewScreen(navController, viewModel)
        }
        
        composable(
            route = Screen.Bracket.route,
            arguments = listOf(navArgument("tournamentId") { type = NavType.LongType })
        ) { 
            com.copaarena.app.ui.screen.tournament.BracketScreen(navController) 
        }
        
        composable(
            route = Screen.Match.route,
            arguments = listOf(navArgument("matchId") { type = NavType.LongType }),
            deepLinks = listOf(navDeepLink { uriPattern = "copa-arena://match/{matchId}" })
        ) { 
            com.copaarena.app.ui.screen.match.MatchScreen(navController) 
        }
        
        composable(
            route = Screen.Stats.route,
            arguments = listOf(navArgument("tournamentId") { 
                type = NavType.StringType 
                nullable = true
                defaultValue = null
            })
        ) { 
            com.copaarena.app.ui.screen.stats.StatsScreen(navController = navController) 
        }
        
        composable(Screen.History.route) { 
            val navController = navController
            com.copaarena.app.ui.screen.history.HistoryScreen(navController) 
        }
        
        composable(
            route = Screen.TournamentDetail.route,
            arguments = listOf(navArgument("tournamentId") { type = NavType.LongType })
        ) { 
            com.copaarena.app.ui.screen.tournament.TournamentDetailScreen(navController) 
        }
        
        composable(
            route = Screen.Ceremony.route,
            arguments = listOf(navArgument("tournamentId") { type = NavType.LongType }),
            enterTransition = { fadeIn(animationSpec = tween(600)) }
        ) { 
            com.copaarena.app.ui.screen.ceremony.CeremonyScreen(navController) 
        }
        
        composable(Screen.Settings.route) { 
            com.copaarena.app.ui.screen.settings.SettingsScreen(navController) 
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}
