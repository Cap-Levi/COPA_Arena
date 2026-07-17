package com.copaarena.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object CreateTournament : Screen("create_tournament?restartId={restartId}") {
        fun createRoute(restartId: Long? = null) = if (restartId != null) "create_tournament?restartId=$restartId" else "create_tournament"
    }
    object AddPlayers : Screen("add_players/{tournamentId}") {
        fun createRoute(tournamentId: Long) = "add_players/$tournamentId"
    }
    object SelectFormat : Screen("select_format/{tournamentId}") {
        fun createRoute(tournamentId: Long) = "select_format/$tournamentId"
    }
    object BracketPreview : Screen("bracket_preview/{tournamentId}") {
        fun createRoute(tournamentId: Long) = "bracket_preview/$tournamentId"
    }
    object Bracket : Screen("bracket/{tournamentId}") {
        fun createRoute(tournamentId: Long) = "bracket/$tournamentId"
    }
    object Match : Screen("match/{matchId}") {
        fun createRoute(matchId: Long) = "match/$matchId"
    }
    object Stats : Screen("stats?tournamentId={tournamentId}") {
        fun createRoute(tournamentId: Long?) = tournamentId?.let { "stats?tournamentId=$it" } ?: "stats"
    }
    object History : Screen("history")
    object TournamentDetail : Screen("tournament_detail/{tournamentId}") {
        fun createRoute(tournamentId: Long) = "tournament_detail/$tournamentId"
    }
    object Ceremony : Screen("ceremony/{tournamentId}") {
        fun createRoute(tournamentId: Long) = "ceremony/$tournamentId"
    }
    object Settings : Screen("settings")
}
