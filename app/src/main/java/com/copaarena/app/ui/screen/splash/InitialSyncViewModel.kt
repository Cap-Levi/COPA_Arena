package com.copaarena.app.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InitialSyncViewModel @Inject constructor(
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncComplete = MutableStateFlow(false)
    val syncComplete: StateFlow<Boolean> = _syncComplete.asStateFlow()

    fun performInitialSyncIfNeeded() {
        if (_isSyncing.value || _syncComplete.value) return
        
        viewModelScope.launch {
            val count = teamRepository.getTeamCount()
            if (count > 0) {
                _syncComplete.value = true
                return@launch
            }

            _isSyncing.value = true
            // Popular Leagues: Premier League(13), La Liga(53), Serie A(31), Bundesliga(19), Ligue 1(16), International(322)
            val popularLeagues = listOf(13, 53, 31, 19, 16, 322)
            
            popularLeagues.forEach { leagueId ->
                teamRepository.syncLeague(leagueId)
            }
            
            _isSyncing.value = false
            _syncComplete.value = true
        }
    }
}
