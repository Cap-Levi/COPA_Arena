package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.entity.MatchEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateStandingsUseCaseTest {

    private val useCase = CalculateStandingsUseCase(FixtureOutcomeResolver())

    private fun completedLeg(
        id: Long,
        matchNumber: Int,
        playerA: Long,
        playerB: Long,
        goalsA: Int,
        goalsB: Int
    ): MatchEntity {
        val isDraw = goalsA == goalsB
        val winner = if (isDraw) null else if (goalsA > goalsB) playerA else playerB
        return MatchEntity(
            id = id, tournamentId = 1L, stage = "GROUP", matchNumber = matchNumber,
            playerAId = playerA, playerBId = playerB, goalsA = goalsA, goalsB = goalsB,
            status = "COMPLETED", winnerId = winner, isDraw = isDraw, leg = 1
        )
    }

    @Test
    fun `sorts by points then goal difference then goals for`() {
        // 3-player round robin, single leg each: 1 beats 2 (3-0), 1 beats 3 (2-1), 2 draws 3 (1-1).
        // Player 1: 6pts, GD +4. Player 3: 1pt, GD -1 (GF 2, GA 3). Player 2: 1pt, GD -3 (GF 1, GA 4).
        // So on the 1-pt tie, player 3 outranks player 2 on goal difference.
        val matches = listOf(
            completedLeg(1, 1, 1L, 2L, 3, 0),
            completedLeg(2, 2, 1L, 3L, 2, 1),
            completedLeg(3, 3, 2L, 3L, 1, 1)
        )
        val standings = useCase.invoke(matches, matchesPerFixture = 1, totalGroupFixturesPerPlayer = 2, qualifiedCount = 2, tournamentId = 100L)

        assertEquals(1L, standings[0].playerId) // 6 pts
        assertEquals(6, standings[0].points)
        assertEquals(3L, standings[1].playerId) // 1 pt, GD -1
        assertEquals(2L, standings[2].playerId) // 1 pt, GD -3
        assertTrue(standings[0].isQualified)
        assertTrue(standings[2].isEliminated)
    }

    @Test
    fun `three way tie resolved by mini-league not just aggregate stats`() {
        // Rock-paper-scissors: 1 beats 2, 2 beats 3, 3 beats 1 — all end up with 3 pts, 0 GD, 1 GF
        // but each player's mini-league record against the other two tied players differs only
        // by which single match they won, so the head-to-head mini-league still ties all three;
        // the seeded coin flip must produce a stable, deterministic order.
        val matches = listOf(
            completedLeg(1, 1, 1L, 2L, 1, 0),
            completedLeg(2, 2, 2L, 3L, 1, 0),
            completedLeg(3, 3, 3L, 1L, 1, 0)
        )
        val standingsA = useCase.invoke(matches, matchesPerFixture = 1, totalGroupFixturesPerPlayer = 2, qualifiedCount = 2, tournamentId = 42L)
        val standingsB = useCase.invoke(matches, matchesPerFixture = 1, totalGroupFixturesPerPlayer = 2, qualifiedCount = 2, tournamentId = 42L)

        // deterministic: same tournamentId -> same order every time
        assertEquals(standingsA.map { it.playerId }, standingsB.map { it.playerId })
        assertEquals(setOf(1L, 2L, 3L), standingsA.map { it.playerId }.toSet())
    }

    @Test
    fun `qualification and elimination fire before all matches are played`() {
        // 4-player group, top 2 qualify. Player 1 has already won both matches (6 pts, done).
        // Player 4 has lost both matches played so far and has only 1 fixture left (max 3 pts) —
        // not enough to catch the players above, so should already be flagged eliminated.
        val matches = listOf(
            completedLeg(1, 1, 1L, 2L, 3, 0),
            completedLeg(2, 2, 1L, 3L, 2, 0),
            completedLeg(3, 3, 1L, 4L, 4, 0),
            completedLeg(4, 4, 2L, 4L, 2, 0),
            completedLeg(5, 5, 3L, 4L, 1, 0)
            // remaining PENDING fixture (not included): 2 vs 3
        )
        val standings = useCase.invoke(matches, matchesPerFixture = 1, totalGroupFixturesPerPlayer = 3, qualifiedCount = 2, tournamentId = 7L)

        val player1 = standings.first { it.playerId == 1L }
        val player4 = standings.first { it.playerId == 4L }
        assertTrue("top-of-table player with games in hand should already be qualified", player1.isQualified)
        assertTrue("bottom player who can no longer catch up should already be eliminated", player4.isEliminated)
    }

    @Test
    fun `tied players sharing the qualification cutoff are resolved by rank, not both eliminated`() {
        // Regression for a live bug: 4-player group, top 2 qualify, all matches played (0 remaining
        // for everyone). Player 1 wins outright (5 pts). Players 3 and 4 finish tied on 3 pts/0 GD/0 GF
        // (all their matches were draws) — exactly at the qualification cutoff. Player 2 is last on 2 pts.
        // The independent ceiling/floor bound can't see tiebreaks, so it double-counts the 3-4 tie and
        // marks BOTH as eliminated (2 rivals with points >= their own ceiling). Once the group stage is
        // fully complete, the tiebreak-resolved rank must decide instead: exactly one of {3,4} qualifies.
        val matches = listOf(
            completedLeg(1, 1, 1L, 2L, 1, 0), // 1 beats 2
            completedLeg(2, 2, 1L, 3L, 0, 0), // 1 draws 3
            completedLeg(3, 3, 1L, 4L, 0, 0), // 1 draws 4
            completedLeg(4, 4, 2L, 3L, 0, 0), // 2 draws 3
            completedLeg(5, 5, 2L, 4L, 0, 0), // 2 draws 4
            completedLeg(6, 6, 3L, 4L, 0, 0)  // 3 draws 4
        )
        val standings = useCase.invoke(matches, matchesPerFixture = 1, totalGroupFixturesPerPlayer = 3, qualifiedCount = 2, tournamentId = 55L)

        val byRank = standings.sortedBy { it.rank }
        assertEquals(1L, byRank[0].playerId)
        assertTrue("clear #1 must be qualified", byRank[0].isQualified)

        val rank2 = byRank[1]
        val rank3 = byRank[2]
        assertTrue("the tiebreak-resolved #2 must be qualified, not eliminated", rank2.isQualified)
        assertTrue("the tiebreak-resolved #2 must not also be marked eliminated", !rank2.isEliminated)
        assertTrue("the tiebreak-resolved #3 (just outside 2 qualifying spots) must be eliminated", rank3.isEliminated)
    }
}
