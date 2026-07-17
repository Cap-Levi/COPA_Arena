package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.dao.TournamentHistoryEntry
import com.copaarena.app.data.repository.TournamentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTournamentHistoryUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository
) {
    operator fun invoke(): Flow<List<TournamentHistoryEntry>> {
        return tournamentRepository.getTournamentHistoryWithWinners()
    }
}
