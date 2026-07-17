package com.copaarena.app.ui.screen.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.db.entity.CachedTeamEntity
import com.copaarena.app.data.repository.TeamRepository

import com.copaarena.app.data.db.fifa.entity.LeagueDbEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class AddPlayersViewModel @Inject constructor(
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CachedTeamEntity>>(emptyList())
    val searchResults: StateFlow<List<CachedTeamEntity>> = _searchResults.asStateFlow()

    private val _selectedLeagueId = MutableStateFlow<Int?>(null)
    val selectedLeagueId: StateFlow<Int?> = _selectedLeagueId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _leagues = MutableStateFlow<List<LeagueDbEntity>>(emptyList())
    val leagues: StateFlow<List<LeagueDbEntity>> = _leagues.asStateFlow()

    init {
        viewModelScope.launch {
            _leagues.value = teamRepository.getAllLeagues()
        }

        _searchQuery
            .debounce(500)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2 || _selectedLeagueId.value != null) {
                    performSearch(query, _selectedLeagueId.value)
                } else {
                    _searchResults.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedLeagueId(leagueId: Int?) {
        _selectedLeagueId.value = leagueId
        viewModelScope.launch {
            if (_searchQuery.value.length >= 2 || leagueId != null) {
                performSearch(_searchQuery.value, leagueId)
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    private suspend fun performSearch(query: String, leagueId: Int?) {
        _isLoading.value = true
        try {
            val results = teamRepository.searchTeams(query, leagueId)
            _searchResults.value = results
        } catch (e: Exception) {
            _searchResults.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    /** Preloads the league+team pickers for editing a player who was already assigned a
     *  team, so reopening their card shows the current selection instead of a blank form. */
    fun prefillForEdit(teamId: Int, onLoaded: (CachedTeamEntity?) -> Unit) {
        viewModelScope.launch {
            val team = teamRepository.getTeamById(teamId)
            setSelectedLeagueId(team?.leagueId)
            onLoaded(team)
        }
    }
}
