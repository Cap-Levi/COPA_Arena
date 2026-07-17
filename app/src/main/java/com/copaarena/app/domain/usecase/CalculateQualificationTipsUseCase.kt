package com.copaarena.app.domain.usecase

import com.copaarena.app.domain.model.QualificationTip
import com.copaarena.app.domain.model.Standing
import javax.inject.Inject

class CalculateQualificationTipsUseCase @Inject constructor() {
    operator fun invoke(standings: List<Standing>, totalMatches: Int): List<QualificationTip> {
        return standings.map { standing ->
            val remainingMatches = totalMatches - standing.matchesPlayed
            val tipText = when {
                standing.isQualified -> "You've qualified! Relax."
                standing.isEliminated -> "Eliminated — cannot qualify."
                remainingMatches > 0 -> "Win your next match to improve chances."
                else -> "Waiting for other results..."
            }
            QualificationTip(standing.playerId, tipText)
        }
    }
}
