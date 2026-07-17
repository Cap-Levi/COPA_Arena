package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.dao.GlobalPlayerSummary
import com.copaarena.app.data.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGlobalStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    operator fun invoke(): Flow<List<GlobalPlayerSummary>> {
        return statsRepository.getGlobalStats()
    }
}
