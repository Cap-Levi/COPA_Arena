package com.copaarena.app.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.db.dao.TournamentHistoryEntry
import com.copaarena.app.domain.usecase.GetTournamentHistoryUseCase
import com.copaarena.app.data.repository.TournamentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getTournamentHistoryUseCase: GetTournamentHistoryUseCase,
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    val history: StateFlow<List<TournamentHistoryEntry>> = getTournamentHistoryUseCase.invoke()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTournament(entry: TournamentHistoryEntry) {
        viewModelScope.launch {
            tournamentRepository.deleteTournamentById(entry.id)
        }
    }
}
