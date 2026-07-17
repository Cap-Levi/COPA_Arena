package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.entity.MatchEntity
import com.copaarena.app.domain.model.Standing
import com.copaarena.app.domain.model.Stage
import javax.inject.Inject

/** Returns the ids of PENDING GROUP matches that can now be skipped because at least one
 *  of the two players is already mathematically qualified or eliminated. */
class CheckEarlyFinalistUseCase @Inject constructor() {
    operator fun invoke(matches: List<MatchEntity>, standings: List<Standing>, qualifiedCount: Int): List<Long> {
        val decidedPlayerIds = standings.filter { it.isQualified || it.isEliminated }.map { it.playerId }.toSet()
        return matches.filter { m ->
            m.stage == Stage.GROUP.name && m.status == "PENDING" &&
                (m.playerAId in decidedPlayerIds || m.playerBId in decidedPlayerIds)
        }.map { it.id }
    }
}
