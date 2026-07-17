package com.copaarena.app.ui.screen.tournament

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.db.entity.MatchEntity
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.data.db.entity.TournamentEntity
import com.copaarena.app.data.repository.MatchRepository
import com.copaarena.app.data.repository.TournamentRepository
import com.copaarena.app.domain.model.QualificationTip
import com.copaarena.app.domain.model.Standing
import com.copaarena.app.domain.model.TournamentFormat
import com.copaarena.app.domain.model.Stage
import com.copaarena.app.domain.usecase.CalculateQualificationTipsUseCase
import com.copaarena.app.domain.usecase.CalculateStandingsUseCase
import com.copaarena.app.domain.usecase.CheckEarlyFinalistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BracketViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tournamentRepository: TournamentRepository,
    private val matchRepository: MatchRepository,
    private val calculateStandingsUseCase: CalculateStandingsUseCase,
    private val calculateQualificationTipsUseCase: CalculateQualificationTipsUseCase,
    private val checkEarlyFinalistUseCase: CheckEarlyFinalistUseCase
) : ViewModel() {

    private val tournamentIdRaw: String? = savedStateHandle.get<String>("tournamentId")
    val tournamentId: Long? = tournamentIdRaw?.toLongOrNull()

    val tournament: StateFlow<TournamentEntity?> = (
        tournamentId?.let { tournamentRepository.getTournamentById(it) } ?: flowOf(null)
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Every tournament (active or completed) — the switcher is how you jump into a
    // finished tournament's bracket/score, not just whichever one is currently active.
    val allTournaments: StateFlow<List<TournamentEntity>> = tournamentRepository.getAllTournaments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val players: StateFlow<List<PlayerEntity>> = (
        tournamentId?.let { matchRepository.getPlayersForTournament(it) } ?: flowOf(emptyList())
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matches: StateFlow<List<MatchEntity>> = (
        tournamentId?.let { matchRepository.getMatchesForTournament(it) } ?: flowOf(emptyList())
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val qualifiedCount: StateFlow<Int> = tournament
        .map { it?.let { t -> TournamentFormat.valueOf(t.format).groupQualifierCount } ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val standings: StateFlow<List<Standing>> = combine(tournament, matches, players) { t, m, p ->
        if (t == null || p.isEmpty()) return@combine emptyList()
        calculateStandingsUseCase.invoke(
            matches = m,
            matchesPerFixture = t.matchesPerFixture,
            totalGroupFixturesPerPlayer = p.size - 1,
            qualifiedCount = TournamentFormat.valueOf(t.format).groupQualifierCount,
            tournamentId = t.id
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val qualificationTips: StateFlow<List<QualificationTip>> = combine(standings, tournament, players) { s, t, p ->
        if (t == null || p.isEmpty()) return@combine emptyList()
        calculateQualificationTipsUseCase.invoke(s, p.size - 1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val skippableMatchIds: StateFlow<Set<Long>> = combine(matches, standings, qualifiedCount) { m, s, qc ->
        checkEarlyFinalistUseCase.invoke(m, s, qc).toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Matches must be played in the order they were drawn — the earliest not-yet-decided
    // seeded match is the only one unlockable; everything after it stays locked until it
    // resolves (played or skipped via the early-finalist skip above).
    val nextMatchId: StateFlow<Long?> = matches.map { list ->
        list.filter { it.playerAId >= 0 && it.playerBId >= 0 }
            .sortedWith(compareBy({ Stage.valueOf(it.stage).ordinal }, { it.matchNumber }, { it.leg }))
            .firstOrNull { it.status != "COMPLETED" && it.status != "SKIPPED" }
            ?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun skipMatch(matchId: Long) {
        viewModelScope.launch {
            matchRepository.markSkipped(matchId)
        }
    }
}
