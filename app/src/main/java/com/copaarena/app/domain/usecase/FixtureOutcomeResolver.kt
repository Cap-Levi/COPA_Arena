package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.entity.MatchEntity
import javax.inject.Inject

data class FixtureOutcome(
    val winnerId: Long?,
    val isDraw: Boolean,
    val goalsFor: Map<Long, Int>,
    val goalsAgainst: Map<Long, Int>,
    val decidedByPenalties: Boolean,
    val legsToAutoSkip: List<Long>
)

/**
 * Resolves the outcome of one fixture (all legs sharing the same tournamentId/stage/matchNumber)
 * for BO1/BO2/BO3 formats: aggregate goals -> away-goals rule -> penalty shootout.
 * Returns null when the fixture is not yet decided (more legs/penalties needed).
 */
class FixtureOutcomeResolver @Inject constructor() {

    fun groupLegsIntoFixtures(matches: List<MatchEntity>): Map<Triple<Long, String, Int>, List<MatchEntity>> =
        matches.groupBy { Triple(it.tournamentId, it.stage, it.matchNumber) }

    /**
     * @param requireDecisive true for KNOCKOUT stages (QUARTER/SEMI/FINAL) where a single-match
     * (BO1) draw must go to penalties rather than standing as a real draw; false for GROUP stage.
     */
    fun resolve(legs: List<MatchEntity>, matchesPerFixture: Int, requireDecisive: Boolean = false): FixtureOutcome? {
        if (legs.isEmpty()) return null
        val first = legs.first()
        if (first.playerAId < 0 || first.playerBId < 0) return null // TBD KO stub, not yet seeded

        val p1 = first.playerAId
        val p2 = first.playerBId

        return when (matchesPerFixture) {
            1 -> resolveSingle(legs, p1, p2, requireDecisive)
            2 -> resolveBestOfTwo(legs, p1, p2)
            else -> resolveBestOfThree(legs, p1, p2)
        }
    }

    private fun goalsForPlayer(leg: MatchEntity, playerId: Long): Int =
        if (leg.playerAId == playerId) leg.goalsA else leg.goalsB

    private fun awayGoalsForPlayer(legs: List<MatchEntity>, playerId: Long): Int =
        legs.filter { it.playerBId == playerId }.sumOf { it.goalsB }

    private fun aggregate(legs: List<MatchEntity>, p1: Long, p2: Long): Pair<Int, Int> {
        val g1 = legs.sumOf { goalsForPlayer(it, p1) }
        val g2 = legs.sumOf { goalsForPlayer(it, p2) }
        return g1 to g2
    }

    /** Finds penalty counts on whichever leg row carries them, remapped to (p1, p2) order. */
    private fun penaltiesFor(legs: List<MatchEntity>, p1: Long, p2: Long): Pair<Int, Int>? {
        val legWithPens = legs.firstOrNull { it.penaltyGoalsA != null && it.penaltyGoalsB != null } ?: return null
        val penP1 = if (legWithPens.playerAId == p1) legWithPens.penaltyGoalsA!! else legWithPens.penaltyGoalsB!!
        val penP2 = if (legWithPens.playerAId == p2) legWithPens.penaltyGoalsA!! else legWithPens.penaltyGoalsB!!
        return penP1 to penP2
    }

    private fun resolveSingle(legs: List<MatchEntity>, p1: Long, p2: Long, requireDecisive: Boolean): FixtureOutcome? {
        val leg = legs.first()
        if (leg.status != "COMPLETED") return null
        val (g1, g2) = aggregate(legs, p1, p2)
        if (g1 != g2) return decisive(p1, p2, g1, g2, decidedByPenalties = false)

        if (!requireDecisive) {
            return FixtureOutcome(
                winnerId = null,
                isDraw = true,
                goalsFor = mapOf(p1 to g1, p2 to g2),
                goalsAgainst = mapOf(p1 to g2, p2 to g1),
                decidedByPenalties = false,
                legsToAutoSkip = emptyList()
            )
        }
        // Knockout stage: a drawn single match must go to a shootout before it can be resolved.
        val (pen1, pen2) = penaltiesFor(legs, p1, p2) ?: return null
        val winner = if (pen1 > pen2) p1 else p2
        return FixtureOutcome(winner, false, mapOf(p1 to g1, p2 to g2), mapOf(p1 to g2, p2 to g1), decidedByPenalties = true, legsToAutoSkip = emptyList())
    }

    private fun resolveBestOfTwo(legs: List<MatchEntity>, p1: Long, p2: Long): FixtureOutcome? {
        if (legs.size < 2 || legs.any { it.status != "COMPLETED" }) return null
        val (g1, g2) = aggregate(legs, p1, p2)

        if (g1 != g2) {
            return decisive(p1, p2, g1, g2, decidedByPenalties = false)
        }
        val away1 = awayGoalsForPlayer(legs, p1)
        val away2 = awayGoalsForPlayer(legs, p2)
        if (away1 != away2) {
            val winner = if (away1 > away2) p1 else p2
            return FixtureOutcome(winner, false, mapOf(p1 to g1, p2 to g2), mapOf(p1 to g2, p2 to g1), decidedByPenalties = false, legsToAutoSkip = emptyList())
        }
        val (pen1, pen2) = penaltiesFor(legs, p1, p2) ?: return null // waiting on shootout entry
        val winner = if (pen1 > pen2) p1 else p2
        return FixtureOutcome(winner, false, mapOf(p1 to g1, p2 to g2), mapOf(p1 to g2, p2 to g1), decidedByPenalties = true, legsToAutoSkip = emptyList())
    }

    private fun resolveBestOfThree(legs: List<MatchEntity>, p1: Long, p2: Long): FixtureOutcome? {
        val sorted = legs.sortedBy { it.leg }
        val completed = sorted.filter { it.status == "COMPLETED" }
        if (completed.size < 2) return null

        fun legWinner(leg: MatchEntity): Long? = if (leg.isDraw) null else leg.winnerId
        val wins1 = completed.count { legWinner(it) == p1 }
        val wins2 = completed.count { legWinner(it) == p2 }

        // 2-0 sweep: series is decided, auto-skip any still-pending 3rd leg
        if (wins1 == 2 || wins2 == 2) {
            val pendingLeg = sorted.firstOrNull { it.status == "PENDING" }
            val (g1, g2) = aggregate(completed, p1, p2)
            val winner = if (wins1 == 2) p1 else p2
            return FixtureOutcome(
                winnerId = winner,
                isDraw = false,
                goalsFor = mapOf(p1 to g1, p2 to g2),
                goalsAgainst = mapOf(p1 to g2, p2 to g1),
                decidedByPenalties = false,
                legsToAutoSkip = listOfNotNull(pendingLeg?.id)
            )
        }

        if (completed.size < 3) return null // 1-1 split, waiting on the decider leg

        if (wins1 != wins2) {
            val winner = if (wins1 > wins2) p1 else p2
            val (g1, g2) = aggregate(completed, p1, p2)
            return FixtureOutcome(winner, false, mapOf(p1 to g1, p2 to g2), mapOf(p1 to g2, p2 to g1), decidedByPenalties = false, legsToAutoSkip = emptyList())
        }

        // leg-win count still tied (e.g. one drawn leg) -> aggregate goals -> away goals -> penalties
        val (g1, g2) = aggregate(completed, p1, p2)
        if (g1 != g2) {
            val winner = if (g1 > g2) p1 else p2
            return FixtureOutcome(winner, false, mapOf(p1 to g1, p2 to g2), mapOf(p1 to g2, p2 to g1), decidedByPenalties = false, legsToAutoSkip = emptyList())
        }
        val away1 = awayGoalsForPlayer(completed, p1)
        val away2 = awayGoalsForPlayer(completed, p2)
        if (away1 != away2) {
            val winner = if (away1 > away2) p1 else p2
            return FixtureOutcome(winner, false, mapOf(p1 to g1, p2 to g2), mapOf(p1 to g2, p2 to g1), decidedByPenalties = false, legsToAutoSkip = emptyList())
        }
        val (pen1, pen2) = penaltiesFor(completed, p1, p2) ?: return null
        val winner = if (pen1 > pen2) p1 else p2
        return FixtureOutcome(winner, false, mapOf(p1 to g1, p2 to g2), mapOf(p1 to g2, p2 to g1), decidedByPenalties = true, legsToAutoSkip = emptyList())
    }

    private fun decisive(p1: Long, p2: Long, g1: Int, g2: Int, decidedByPenalties: Boolean): FixtureOutcome {
        val winner = if (g1 > g2) p1 else p2
        return FixtureOutcome(
            winnerId = winner,
            isDraw = false,
            goalsFor = mapOf(p1 to g1, p2 to g2),
            goalsAgainst = mapOf(p1 to g2, p2 to g1),
            decidedByPenalties = decidedByPenalties,
            legsToAutoSkip = emptyList()
        )
    }
}
