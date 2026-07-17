package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.entity.MatchEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FixtureOutcomeResolverTest {

    private val resolver = FixtureOutcomeResolver()

    private fun leg(
        id: Long,
        stage: String = "GROUP",
        matchNumber: Int = 1,
        leg: Int,
        playerA: Long,
        playerB: Long,
        goalsA: Int = 0,
        goalsB: Int = 0,
        status: String = "COMPLETED",
        winnerId: Long? = null,
        isDraw: Boolean = false,
        penA: Int? = null,
        penB: Int? = null
    ) = MatchEntity(
        id = id, tournamentId = 1L, stage = stage, matchNumber = matchNumber,
        playerAId = playerA, playerBId = playerB, goalsA = goalsA, goalsB = goalsB,
        status = status, winnerId = winnerId, isDraw = isDraw, leg = leg,
        penaltyGoalsA = penA, penaltyGoalsB = penB
    )

    @Test
    fun `single leg decisive win`() {
        val legs = listOf(leg(1, leg = 1, playerA = 10, playerB = 20, goalsA = 2, goalsB = 1, winnerId = 10))
        val outcome = resolver.resolve(legs, matchesPerFixture = 1)!!
        assertEquals(10L, outcome.winnerId)
        assertEquals(false, outcome.isDraw)
        assertEquals(2, outcome.goalsFor[10L])
        assertEquals(1, outcome.goalsFor[20L])
    }

    @Test
    fun `group stage single leg draw stays a draw`() {
        val legs = listOf(leg(1, leg = 1, playerA = 10, playerB = 20, goalsA = 1, goalsB = 1, isDraw = true))
        val outcome = resolver.resolve(legs, matchesPerFixture = 1, requireDecisive = false)!!
        assertTrue(outcome.isDraw)
        assertNull(outcome.winnerId)
    }

    @Test
    fun `knockout single leg draw waits for penalties then resolves`() {
        val legs = listOf(leg(1, leg = 1, playerA = 10, playerB = 20, goalsA = 1, goalsB = 1, isDraw = true))
        val pending = resolver.resolve(legs, matchesPerFixture = 1, requireDecisive = true)
        assertNull(pending)

        val withPens = listOf(leg(1, leg = 1, playerA = 10, playerB = 20, goalsA = 1, goalsB = 1, isDraw = true, penA = 5, penB = 4))
        val outcome = resolver.resolve(withPens, matchesPerFixture = 1, requireDecisive = true)!!
        assertEquals(10L, outcome.winnerId)
        assertTrue(outcome.decidedByPenalties)
    }

    @Test
    fun `bo2 decided by away goals when aggregate tied`() {
        val legs = listOf(
            leg(1, leg = 1, playerA = 10, playerB = 20, goalsA = 1, goalsB = 1),
            leg(2, leg = 2, playerA = 20, playerB = 10, goalsA = 2, goalsB = 2)
        )
        val outcome = resolver.resolve(legs, matchesPerFixture = 2)!!
        // aggregate 3-3, away goals: p1 scored 2 away (leg2), p2 scored 1 away (leg1) -> p1 wins
        assertEquals(10L, outcome.winnerId)
        assertEquals(false, outcome.decidedByPenalties)
    }

    @Test
    fun `bo2 tied on away goals waits for penalties`() {
        val legs = listOf(
            leg(1, leg = 1, playerA = 10, playerB = 20, goalsA = 1, goalsB = 1),
            leg(2, leg = 2, playerA = 20, playerB = 10, goalsA = 1, goalsB = 1)
        )
        val pending = resolver.resolve(legs, matchesPerFixture = 2)
        assertNull(pending)

        val withPens = legs.toMutableList().also {
            it[1] = it[1].copy(penaltyGoalsA = 3, penaltyGoalsB = 5)
        }
        val outcome = resolver.resolve(withPens, matchesPerFixture = 2)!!
        // leg2 penaltyGoalsA belongs to p2(20)=3, penaltyGoalsB belongs to p1(10)=5 -> p1 wins
        assertEquals(10L, outcome.winnerId)
        assertTrue(outcome.decidedByPenalties)
    }

    @Test
    fun `bo3 two-nil sweep auto-skips the pending third leg`() {
        val legs = listOf(
            leg(1, leg = 1, playerA = 10, playerB = 20, goalsA = 2, goalsB = 0, winnerId = 10),
            leg(2, leg = 2, playerA = 20, playerB = 10, goalsA = 0, goalsB = 2, winnerId = 10),
            leg(3, leg = 3, playerA = 10, playerB = 20, status = "PENDING")
        )
        val outcome = resolver.resolve(legs, matchesPerFixture = 3)!!
        assertEquals(10L, outcome.winnerId)
        assertEquals(listOf(3L), outcome.legsToAutoSkip)
    }

    @Test
    fun `bo3 split legs need the decider`() {
        val legs = listOf(
            leg(1, leg = 1, playerA = 10, playerB = 20, goalsA = 2, goalsB = 0, winnerId = 10),
            leg(2, leg = 2, playerA = 20, playerB = 10, goalsA = 2, goalsB = 0, winnerId = 20),
            leg(3, leg = 3, playerA = 10, playerB = 20, status = "PENDING")
        )
        val outcome = resolver.resolve(legs, matchesPerFixture = 3)
        assertNull(outcome)
    }

    @Test
    fun `unseeded knockout stub is not resolvable`() {
        val legs = listOf(leg(1, leg = 1, playerA = -1, playerB = -1, status = "PENDING"))
        assertNull(resolver.resolve(legs, matchesPerFixture = 1, requireDecisive = true))
    }
}
