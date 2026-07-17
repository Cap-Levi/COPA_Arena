package com.copaarena.app.ui.screen.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.db.dao.GlobalPlayerSummary
import com.copaarena.app.data.db.dao.PlayerGoalCount
import com.copaarena.app.data.db.dao.PlayerMatchGoal
import com.copaarena.app.data.db.dao.TopScorerResult
import com.copaarena.app.data.db.entity.MatchEntity
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.data.db.entity.TournamentEntity
import com.copaarena.app.data.repository.MatchRepository
import com.copaarena.app.data.repository.StatsRepository
import com.copaarena.app.data.repository.TournamentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val statsRepository: StatsRepository,
    private val matchRepository: MatchRepository,
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val tournamentIdRaw: String? = savedStateHandle.get<String>("tournamentId")
    val tournamentId: Long? = tournamentIdRaw?.toLongOrNull()

    // Every tournament (active or completed) — Stats' switcher is how you jump into a
    // finished tournament's numbers, not just the currently-running one(s).
    val allTournaments: StateFlow<List<TournamentEntity>> = tournamentRepository.getAllTournaments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topScorers: StateFlow<List<TopScorerResult>> = statsRepository.getTopScorers(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalStats: StateFlow<List<GlobalPlayerSummary>> = statsRepository.getGlobalStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Only meaningful for the "This Tournament" tab — a player's team is fixed for one
    // tournament but can change between tournaments, so per-player team badges only make
    // sense scoped to a single tournament.
    val players: StateFlow<List<PlayerEntity>> = (
        tournamentId?.let { matchRepository.getPlayersForTournament(it) } ?: flowOf(emptyList())
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matches: StateFlow<List<MatchEntity>> = (
        tournamentId?.let { matchRepository.getMatchesForTournament(it) } ?: flowOf(emptyList())
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Keyed by local player id, not name — topScorers now reports the real FIFA scorer
    // name per goal, which can vary goal-to-goal for the same local player.
    val playerGoalCounts: StateFlow<List<PlayerGoalCount>> = (
        tournamentId?.let { statsRepository.getGoalCountsForTournament(it) } ?: flowOf(emptyList())
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun goalsByMatchForPlayer(playerId: Long): Flow<List<PlayerMatchGoal>> {
        return tournamentId?.let { statsRepository.getGoalsByMatchForPlayer(it, playerId) } ?: flowOf(emptyList())
    }
}
