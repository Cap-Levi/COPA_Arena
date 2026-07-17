package com.copaarena.app.ui.screen.tournament

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.data.db.entity.TournamentEntity
import com.copaarena.app.data.db.entity.TournamentStatsEntity
import com.copaarena.app.data.repository.MatchRepository
import com.copaarena.app.data.repository.StatsRepository
import com.copaarena.app.data.repository.TournamentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TournamentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tournamentRepository: TournamentRepository,
    matchRepository: MatchRepository,
    statsRepository: StatsRepository
) : ViewModel() {

    private val tournamentId: Long = savedStateHandle.get<Long>("tournamentId") ?: 0L

    val tournament: StateFlow<TournamentEntity?> = tournamentRepository.getTournamentById(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val players: StateFlow<List<PlayerEntity>> = matchRepository.getPlayersForTournament(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val standings: StateFlow<List<TournamentStatsEntity>> = statsRepository.getStandingsForTournament(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
