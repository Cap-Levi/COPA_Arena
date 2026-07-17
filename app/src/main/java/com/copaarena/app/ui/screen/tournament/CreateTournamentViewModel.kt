package com.copaarena.app.ui.screen.tournament

import androidx.lifecycle.ViewModel
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.domain.model.TournamentFormat
import com.copaarena.app.domain.usecase.CreateTournamentUseCase
import com.copaarena.app.domain.usecase.GenerateBracketUseCase
import com.copaarena.app.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class CreateTournamentViewModel @Inject constructor(
    private val createTournamentUseCase: CreateTournamentUseCase,
    private val generateBracketUseCase: GenerateBracketUseCase,
    private val matchRepository: MatchRepository,
    private val tournamentRepository: com.copaarena.app.data.repository.TournamentRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()
    
    private val _date = MutableStateFlow(System.currentTimeMillis())
    val date: StateFlow<Long> = _date.asStateFlow()
    
    private val _matchesPerFixture = MutableStateFlow(1)
    val matchesPerFixture: StateFlow<Int> = _matchesPerFixture.asStateFlow()
    
    private val _players = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val players: StateFlow<List<PlayerEntity>> = _players.asStateFlow()
    
    private val _isRestartMode = MutableStateFlow(false)
    val isRestartMode: StateFlow<Boolean> = _isRestartMode.asStateFlow()
    
    private val _selectedFormat = MutableStateFlow<TournamentFormat?>(null)
    val selectedFormat: StateFlow<TournamentFormat?> = _selectedFormat.asStateFlow()

    fun setName(newName: String) { _name.value = newName }
    fun setDate(newDate: Long) { _date.value = newDate }
    fun setMatchesPerFixture(count: Int) { _matchesPerFixture.value = count }
    fun addPlayer(player: PlayerEntity) { _players.value = _players.value + player }
    fun removePlayer(player: PlayerEntity) { _players.value = _players.value - player }
    fun setFormat(format: TournamentFormat) { _selectedFormat.value = format }

    fun updatePlayerTeamAt(index: Int, newTeamName: String, newBadgeUrl: String, newOverall: Int, newTeamId: Int) {
        _players.value = _players.value.mapIndexed { i, p ->
            if (i == index) p.copy(teamName = newTeamName, teamBadgeUrl = newBadgeUrl, teamOverall = newOverall, teamId = newTeamId) else p
        }
    }

    fun loadPlayersForRestart(oldTournamentId: Long) {
        _isRestartMode.value = true
        viewModelScope.launch {
            // Load old name
            tournamentRepository.getTournamentById(oldTournamentId).collect { t ->
                if (t != null && _name.value.isBlank()) {
                    _name.value = "${t.name} - Restart"
                }
            }
        }
        viewModelScope.launch {
            matchRepository.getPlayersForTournament(oldTournamentId).collect { oldPlayers ->
                // Make sure to reset their ID and tournamentId to 0, so they get inserted as new
                _players.value = oldPlayers.map { it.copy(id = 0L, tournamentId = 0L, seed = 0) }
            }
        }
    }

    fun suggestFormat(): TournamentFormat {
        val count = _players.value.size
        return when {
            count <= 4 -> TournamentFormat.ROUND_ROBIN
            count in 5..6 -> TournamentFormat.SEMIFINALS
            else -> TournamentFormat.QUARTERFINALS
        }
    }

    suspend fun kickoff(): Long {
        val tid = createTournamentUseCase.invoke(
            name = _name.value,
            date = _date.value,
            format = _selectedFormat.value ?: suggestFormat(),
            matchesPerFixture = _matchesPerFixture.value
        )
        val playersWithTid = _players.value.mapIndexed { index, p -> 
            p.copy(tournamentId = tid, seed = index + 1) 
        }
        val pIds = matchRepository.insertPlayers(playersWithTid)
        val finalPlayers = playersWithTid.mapIndexed { i, p -> p.copy(id = pIds[i]) }
        
        val matches = generateBracketUseCase.invoke(
            tournamentId = tid,
            players = finalPlayers,
            format = _selectedFormat.value ?: suggestFormat(),
            matchesPerFixture = _matchesPerFixture.value
        )
        matchRepository.insertAll(matches)
        tournamentRepository.updateStatus(tid, "ACTIVE")

        return tid
    }
}
