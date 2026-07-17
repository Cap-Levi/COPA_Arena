package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.entity.GoalEntity
import com.copaarena.app.data.repository.StatsRepository
import javax.inject.Inject

class RecordGoalUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(
        matchId: Long,
        scorerId: Long,
        creditedToId: Long,
        isOwnGoal: Boolean,
        minute: Int?,
        fifaPlayerName: String? = null
    ): Long {
        val goal = GoalEntity(
            matchId = matchId,
            scorerId = scorerId,
            creditedToId = creditedToId,
            isOwnGoal = isOwnGoal,
            minute = minute,
            fifaPlayerName = fifaPlayerName
        )
        statsRepository.insertGoal(goal)
        return goal.id
    }
}
