package com.copaarena.app.ui.screen.ceremony

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.data.db.entity.TournamentEntity
import com.copaarena.app.data.db.entity.TournamentStatsEntity
import com.copaarena.app.data.repository.MatchRepository
import com.copaarena.app.data.repository.StatsRepository
import com.copaarena.app.data.repository.TournamentRepository
import com.copaarena.app.utils.ShareCardGenerator
import com.copaarena.app.utils.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CeremonyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tournamentRepository: TournamentRepository,
    matchRepository: MatchRepository,
    statsRepository: StatsRepository,
    private val shareCardGenerator: ShareCardGenerator,
    private val soundManager: SoundManager
) : ViewModel() {

    private val tournamentId: Long = savedStateHandle.get<Long>("tournamentId") ?: 0L

    val tournament: StateFlow<TournamentEntity?> = tournamentRepository.getTournamentById(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val players: StateFlow<List<PlayerEntity>> = matchRepository.getPlayersForTournament(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val standings: StateFlow<List<TournamentStatsEntity>> = statsRepository.getStandingsForTournament(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val champion: StateFlow<PlayerEntity?> = combine(tournament, players) { t, p ->
        val winnerId = t?.winnerId ?: return@combine null
        p.find { it.id == winnerId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mvp: StateFlow<PlayerEntity?> = combine(tournament, players) { t, p ->
        val mvpId = t?.mvpPlayerId ?: return@combine null
        p.find { it.id == mvpId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun playChampionFanfare() {
        soundManager.playChampionFanfare()
    }

    fun shareCard() {
        viewModelScope.launch {
            val t = tournament.value ?: return@launch
            shareCardGenerator.generateAndShare(t.name, t.winnerId, standings.value, players.value)
        }
    }
}
