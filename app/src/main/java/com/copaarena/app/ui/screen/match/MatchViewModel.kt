package com.copaarena.app.ui.screen.match

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.datastore.SettingsDataStore
import com.copaarena.app.data.db.dao.PlayerDao
import com.copaarena.app.data.db.entity.GoalEntity
import com.copaarena.app.data.db.entity.MatchEntity
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.data.repository.MatchRepository
import com.copaarena.app.data.repository.StatsRepository
import com.copaarena.app.data.repository.TournamentRepository
import com.copaarena.app.domain.model.Stage
import com.copaarena.app.domain.usecase.ConfirmMatchResultUseCase
import com.copaarena.app.domain.usecase.FixtureOutcomeResolver
import com.copaarena.app.domain.usecase.RecordGoalUseCase
import com.copaarena.app.utils.SoundManager
import com.copaarena.app.data.db.fifa.dao.FifaDao
import com.copaarena.app.data.db.fifa.entity.PlayerDbEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val playerDao: PlayerDao,
    private val statsRepository: StatsRepository,
    private val tournamentRepository: TournamentRepository,
    private val recordGoalUseCase: RecordGoalUseCase,
    private val confirmMatchResultUseCase: ConfirmMatchResultUseCase,
    private val fixtureOutcomeResolver: FixtureOutcomeResolver,
    private val soundManager: SoundManager,
    private val fifaDao: FifaDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val matchId: Long = savedStateHandle.get<Long>("matchId") ?: 0L

    private val _match = MutableStateFlow<MatchEntity?>(null)
    val match: StateFlow<MatchEntity?> = _match.asStateFlow()

    private val _playerA = MutableStateFlow<PlayerEntity?>(null)
    val playerA: StateFlow<PlayerEntity?> = _playerA.asStateFlow()

    private val _playerB = MutableStateFlow<PlayerEntity?>(null)
    val playerB: StateFlow<PlayerEntity?> = _playerB.asStateFlow()

    private val _goals = MutableStateFlow<List<GoalEntity>>(emptyList())
    val goals: StateFlow<List<GoalEntity>> = _goals.asStateFlow()

    private val _fifaPlayersA = MutableStateFlow<List<PlayerDbEntity>>(emptyList())
    val fifaPlayersA: StateFlow<List<PlayerDbEntity>> = _fifaPlayersA.asStateFlow()

    private val _fifaPlayersB = MutableStateFlow<List<PlayerDbEntity>>(emptyList())
    val fifaPlayersB: StateFlow<List<PlayerDbEntity>> = _fifaPlayersB.asStateFlow()

    private val _needsPenalties = MutableStateFlow(false)
    val needsPenalties: StateFlow<Boolean> = _needsPenalties.asStateFlow()

    private val _navigateToCeremony = MutableStateFlow<Long?>(null)
    val navigateToCeremony: StateFlow<Long?> = _navigateToCeremony.asStateFlow()

    private val _celebrateGoal = MutableStateFlow(false)
    val celebrateGoal: StateFlow<Boolean> = _celebrateGoal.asStateFlow()

    private val _celebrateFullTime = MutableStateFlow(false)
    val celebrateFullTime: StateFlow<Boolean> = _celebrateFullTime.asStateFlow()

    fun consumeGoalCelebration() { _celebrateGoal.value = false }
    fun consumeFullTimeCelebration() { _celebrateFullTime.value = false }

    val hapticEnabled: StateFlow<Boolean> = settingsDataStore.hapticEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            matchRepository.getMatchById(matchId).collect { m ->
                _match.value = m
                if (m != null) {
                    val pA = playerDao.getPlayerById(m.playerAId)
                    val pB = playerDao.getPlayerById(m.playerBId)
                    _playerA.value = pA
                    _playerB.value = pB

                    if (pA?.teamId != null && pA.teamId > 0) {
                        _fifaPlayersA.value = fifaDao.getPlayersByClub(pA.teamId)
                    }
                    if (pB?.teamId != null && pB.teamId > 0) {
                        _fifaPlayersB.value = fifaDao.getPlayersByClub(pB.teamId)
                    }
                }
            }
        }
        viewModelScope.launch {
            statsRepository.getGoalsForMatch(matchId).collect { g ->
                _goals.value = g
            }
        }
    }

    fun addGoal(scorerId: Long, isOwnGoal: Boolean, fifaPlayerName: String? = null) {
        viewModelScope.launch {
            val m = _match.value ?: return@launch
            val creditedToId = if (isOwnGoal) {
                if (scorerId == m.playerAId) m.playerBId else m.playerAId
            } else {
                scorerId
            }
            recordGoalUseCase.invoke(matchId, scorerId, creditedToId, isOwnGoal, null, fifaPlayerName)
            soundManager.playGoalCheer()
            _celebrateGoal.value = true
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            statsRepository.deleteGoal(goal)
        }
    }

    fun consumeCeremonyNavigation() {
        _navigateToCeremony.value = null
    }

    /** Confirms this leg. If the fixture still can't be decided afterwards (BO2/BO3 tie, or a
     * drawn knockout match), [needsPenalties] flips true so the UI can collect a shootout result
     * and call this again with the penalty counts. */
    suspend fun confirmMatch(penaltyGoalsA: Int? = null, penaltyGoalsB: Int? = null) {
        val m = _match.value ?: return
        val g = _goals.value
        val goalsA = g.count { it.creditedToId == m.playerAId }
        val goalsB = g.count { it.creditedToId == m.playerBId }
        confirmMatchResultUseCase.invoke(matchId, goalsA, goalsB, m.playerAId, m.playerBId, penaltyGoalsA, penaltyGoalsB)
        soundManager.playWhistle()

        val refreshed = matchRepository.getMatchByIdOnce(matchId) ?: return
        val tournament = tournamentRepository.getTournamentByIdOnce(refreshed.tournamentId) ?: return
        val legs = matchRepository.getFixtureLegs(refreshed.tournamentId, refreshed.stage, refreshed.matchNumber)
        val requireDecisive = refreshed.stage != Stage.GROUP.name
        val outcome = fixtureOutcomeResolver.resolve(legs, tournament.matchesPerFixture, requireDecisive)
        _needsPenalties.value = outcome == null && legs.isNotEmpty() && legs.all { it.status == "COMPLETED" }

        if (!_needsPenalties.value) {
            _celebrateFullTime.value = true
        }

        if (tournament.status == "COMPLETED") {
            _navigateToCeremony.value = refreshed.tournamentId
        }
    }
}
