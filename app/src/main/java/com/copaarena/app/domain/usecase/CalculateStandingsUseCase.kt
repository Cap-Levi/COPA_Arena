package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.entity.MatchEntity
import com.copaarena.app.domain.model.Standing
import com.copaarena.app.domain.model.Stage
import javax.inject.Inject
import kotlin.random.Random

class CalculateStandingsUseCase @Inject constructor(
    private val fixtureOutcomeResolver: FixtureOutcomeResolver
) {
    operator fun invoke(
        matches: List<MatchEntity>,
        matchesPerFixture: Int,
        totalGroupFixturesPerPlayer: Int,
        qualifiedCount: Int,
        tournamentId: Long
    ): List<Standing> {
        val groupMatches = matches.filter { it.stage == Stage.GROUP.name }
        val statsMap = mutableMapOf<Long, PlayerStats>()
        groupMatches.forEach { m ->
            statsMap.getOrPut(m.playerAId) { PlayerStats(m.playerAId) }
            statsMap.getOrPut(m.playerBId) { PlayerStats(m.playerBId) }
        }

        val fixtures = fixtureOutcomeResolver.groupLegsIntoFixtures(groupMatches)

        fixtures.values.forEach { legs ->
            // Reading back already-recorded history here, not making a live call — a fixture
            // stored as a completed draw (accepted tie or otherwise) should always count as one.
            val outcome = fixtureOutcomeResolver.resolve(legs, matchesPerFixture, allowDraw = true) ?: return@forEach
            val (idA, idB) = outcome.goalsFor.keys.toList()
            val pA = statsMap.getOrPut(idA) { PlayerStats(idA) }
            val pB = statsMap.getOrPut(idB) { PlayerStats(idB) }

            pA.matchesPlayed++; pB.matchesPlayed++
            pA.goalsFor += outcome.goalsFor[idA] ?: 0
            pA.goalsAgainst += outcome.goalsAgainst[idA] ?: 0
            pB.goalsFor += outcome.goalsFor[idB] ?: 0
            pB.goalsAgainst += outcome.goalsAgainst[idB] ?: 0

            when {
                // Penalty shootout: the 90 minutes were level, so it's a draw for both with a
                // one-point bonus for the shootout winner (2/1), not a full win/loss.
                outcome.decidedByPenalties -> {
                    pA.draws++; pB.draws++
                    if (outcome.winnerId == idA) { pA.points += 2; pB.points += 1 }
                    else { pB.points += 2; pA.points += 1 }
                }
                outcome.isDraw -> {
                    pA.draws++; pA.points += 1
                    pB.draws++; pB.points += 1
                }
                outcome.winnerId == idA -> { pA.wins++; pA.points += 3; pB.losses++ }
                outcome.winnerId == idB -> { pB.wins++; pB.points += 3; pA.losses++ }
            }
        }

        fun key(p: PlayerStats) = Triple(p.points, p.goalsFor - p.goalsAgainst, p.goalsFor)

        val basicSorted = statsMap.values.sortedWith(
            compareByDescending<PlayerStats> { it.points }
                .thenByDescending { it.goalsFor - it.goalsAgainst }
                .thenByDescending { it.goalsFor }
        )

        // Cluster players tied on points/GD/GF and break the tie with a mini-league,
        // then a tournamentId-seeded coin flip as the final fallback.
        val clusters = mutableListOf<MutableList<PlayerStats>>()
        for (p in basicSorted) {
            val last = clusters.lastOrNull()
            if (last != null && key(last.first()) == key(p)) last.add(p) else clusters.add(mutableListOf(p))
        }

        val finalOrder = clusters.flatMap { cluster ->
            if (cluster.size == 1) cluster
            else resolveClusterOrder(cluster, fixtures, matchesPerFixture, tournamentId)
        }

        // Once every player has no fixtures left, the table is final: the tiebreak-resolved
        // rank (which already accounts for H2H/mini-league/coin-flip) is authoritative, and must
        // win over the conservative ceiling/floor bound below — that bound can't see tiebreaks,
        // so it double-counts a rival tied at the cutoff as "already ahead" on both sides of the
        // comparison, marking a genuinely-qualified tied player as eliminated.
        val groupStageComplete = finalOrder.all { it.matchesPlayed >= totalGroupFixturesPerPlayer }

        return finalOrder.mapIndexed { index, stat ->
            if (groupStageComplete) {
                return@mapIndexed Standing(
                    rank = index + 1,
                    playerId = stat.playerId,
                    matchesPlayed = stat.matchesPlayed,
                    wins = stat.wins,
                    draws = stat.draws,
                    losses = stat.losses,
                    goalsFor = stat.goalsFor,
                    goalsAgainst = stat.goalsAgainst,
                    goalDifference = stat.goalsFor - stat.goalsAgainst,
                    points = stat.points,
                    isQualified = index < qualifiedCount,
                    isEliminated = index >= qualifiedCount
                )
            }

            val remaining = totalGroupFixturesPerPlayer - stat.matchesPlayed
            val ceiling = stat.points + 3 * remaining
            val floor = stat.points

            val rivalsCanReachFloor = finalOrder.count { rival ->
                rival.playerId != stat.playerId &&
                    (rival.points + 3 * (totalGroupFixturesPerPlayer - rival.matchesPlayed)) >= floor
            }
            val rivalsAlreadyBeatCeiling = finalOrder.count { rival ->
                rival.playerId != stat.playerId && rival.points >= ceiling
            }

            Standing(
                rank = index + 1,
                playerId = stat.playerId,
                matchesPlayed = stat.matchesPlayed,
                wins = stat.wins,
                draws = stat.draws,
                losses = stat.losses,
                goalsFor = stat.goalsFor,
                goalsAgainst = stat.goalsAgainst,
                goalDifference = stat.goalsFor - stat.goalsAgainst,
                points = stat.points,
                isQualified = rivalsCanReachFloor < qualifiedCount,
                isEliminated = rivalsAlreadyBeatCeiling >= qualifiedCount
            )
        }
    }

    private fun resolveClusterOrder(
        cluster: List<PlayerStats>,
        fixtures: Map<Triple<Long, String, Int>, List<MatchEntity>>,
        matchesPerFixture: Int,
        tournamentId: Long
    ): List<PlayerStats> {
        val ids = cluster.map { it.playerId }.toSet()
        data class MiniStats(var points: Int = 0, var gd: Int = 0, var gf: Int = 0)
        val mini = ids.associateWith { MiniStats() }

        fixtures.values.forEach { legs ->
            val first = legs.firstOrNull() ?: return@forEach
            val a = first.playerAId
            val b = first.playerBId
            if (a !in ids || b !in ids) return@forEach
            // Reading back already-recorded history here, not making a live call — a fixture
            // stored as a completed draw (accepted tie or otherwise) should always count as one.
            val outcome = fixtureOutcomeResolver.resolve(legs, matchesPerFixture, allowDraw = true) ?: return@forEach

            val ga = outcome.goalsFor[a] ?: 0
            val gaAgainst = outcome.goalsAgainst[a] ?: 0
            val gb = outcome.goalsFor[b] ?: 0
            val gbAgainst = outcome.goalsAgainst[b] ?: 0

            mini[a]?.let { it.gf += ga; it.gd += ga - gaAgainst }
            mini[b]?.let { it.gf += gb; it.gd += gb - gbAgainst }

            when {
                outcome.decidedByPenalties -> {
                    if (outcome.winnerId == a) { mini[a]?.let { it.points += 2 }; mini[b]?.let { it.points += 1 } }
                    else { mini[b]?.let { it.points += 2 }; mini[a]?.let { it.points += 1 } }
                }
                outcome.isDraw -> {
                    mini[a]?.let { it.points += 1 }
                    mini[b]?.let { it.points += 1 }
                }
                outcome.winnerId == a -> mini[a]?.let { it.points += 3 }
                outcome.winnerId == b -> mini[b]?.let { it.points += 3 }
            }
        }

        return cluster.sortedWith(
            compareByDescending<PlayerStats> { mini[it.playerId]?.points ?: 0 }
                .thenByDescending { mini[it.playerId]?.gd ?: 0 }
                .thenByDescending { mini[it.playerId]?.gf ?: 0 }
                .thenByDescending { coinFlipKey(it.playerId, tournamentId) }
        )
    }

    /** Deterministic seeded "coin flip": stable across recompositions, unique per tournament+player. */
    private fun coinFlipKey(playerId: Long, tournamentId: Long): Int =
        Random(tournamentId * 1_000_003L + playerId).nextInt()

    private class PlayerStats(
        val playerId: Long,
        var matchesPlayed: Int = 0,
        var wins: Int = 0,
        var draws: Int = 0,
        var losses: Int = 0,
        var goalsFor: Int = 0,
        var goalsAgainst: Int = 0,
        var points: Int = 0
    )
}
