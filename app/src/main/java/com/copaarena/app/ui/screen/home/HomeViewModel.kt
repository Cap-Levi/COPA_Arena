package com.copaarena.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.db.dao.TournamentHistoryEntry
import com.copaarena.app.data.db.entity.TournamentEntity
import com.copaarena.app.data.repository.MatchRepository
import com.copaarena.app.data.repository.TournamentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    tournamentRepository: TournamentRepository,
    matchRepository: MatchRepository
) : ViewModel() {

    val activeTournaments: StateFlow<List<TournamentEntity>> = tournamentRepository.getActiveTournaments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTournamentId = MutableStateFlow<Long?>(null)

    // Falls back to the newest active tournament whenever the selection is unset or the
    // previously-selected tournament is no longer active (completed/deleted elsewhere).
    val activeTournament: StateFlow<TournamentEntity?> = combine(
        activeTournaments,
        _selectedTournamentId
    ) { tournaments, selectedId ->
        tournaments.find { it.id == selectedId } ?: tournaments.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectTournament(id: Long) {
        _selectedTournamentId.value = id
    }

    val progress: StateFlow<Float> = activeTournament
        .flatMapLatest { t ->
            if (t == null) flowOf(0f)
            else matchRepository.getMatchesForTournament(t.id).map { matches ->
                if (matches.isEmpty()) 0f
                else matches.count { it.status == "COMPLETED" || it.status == "SKIPPED" }.toFloat() / matches.size
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Most recently completed tournament (list is already createdAt DESC) — drives the
    // "Last Champion" card so Home has something to show even with no active tournament.
    val lastCompletedTournament: StateFlow<TournamentHistoryEntry?> = tournamentRepository
        .getTournamentHistoryWithWinners()
        .map { list -> list.firstOrNull { it.status == "COMPLETED" && it.winnerName != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
