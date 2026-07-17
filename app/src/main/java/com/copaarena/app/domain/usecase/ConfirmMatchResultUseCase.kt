package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.entity.MatchEntity
import com.copaarena.app.data.db.entity.TournamentEntity
import com.copaarena.app.data.db.entity.TournamentStatsEntity
import com.copaarena.app.data.repository.MatchRepository
import com.copaarena.app.data.repository.StatsRepository
import com.copaarena.app.data.repository.TournamentRepository
import com.copaarena.app.domain.model.Stage
import com.copaarena.app.domain.model.TournamentFormat
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ConfirmMatchResultUseCase @Inject constructor(
    private val matchRepository: MatchRepository,
    private val statsRepository: StatsRepository,
    private val tournamentRepository: TournamentRepository,
    private val fixtureOutcomeResolver: FixtureOutcomeResolver,
    private val calculateStandingsUseCase: CalculateStandingsUseCase,
    private val advanceBracketUseCase: AdvanceBracketUseCase
) {
    suspend operator fun invoke(
        matchId: Long,
        goalsA: Int,
        goalsB: Int,
        playerAId: Long,
        playerBId: Long,
        penaltyGoalsA: Int? = null,
        penaltyGoalsB: Int? = null,
        acceptDraw: Boolean = false
    ) {
        val hasPenalties = penaltyGoalsA != null && penaltyGoalsB != null
        val draw = goalsA == goalsB && !hasPenalties
        val winnerId = when {
            hasPenalties -> if (penaltyGoalsA!! > penaltyGoalsB!!) playerAId else playerBId
            draw -> null
            goalsA > goalsB -> playerAId
            else -> playerBId
        }

        if (hasPenalties) {
            matchRepository.updateMatchResult(matchId, goalsA, goalsB, "COMPLETED", winnerId, draw = false, penaltyGoalsA, penaltyGoalsB)
        } else {
            matchRepository.updateMatchResult(matchId, goalsA, goalsB, "COMPLETED", winnerId, draw)
        }

        val match = matchRepository.getMatchByIdOnce(matchId) ?: return
        val tournament = tournamentRepository.getTournamentByIdOnce(match.tournamentId) ?: return

        val legs = matchRepository.getFixtureLegs(match.tournamentId, match.stage, match.matchNumber)
        // A still-tied fixture only resolves as a plain draw once the user has explicitly said
        // so (acceptDraw) — otherwise it stays pending so the UI can ask "was this decided by
        // penalties?" regardless of stage (group or knockout).
        val outcome = fixtureOutcomeResolver.resolve(legs, tournament.matchesPerFixture, allowDraw = acceptDraw) ?: return

        outcome.legsToAutoSkip.forEach { matchRepository.markSkipped(it) }

        val completedLegIds = legs.filter { it.status == "COMPLETED" }.map { it.id }
        outcome.goalsFor.keys.forEach { playerId ->
            upsertFixtureStats(match.tournamentId, playerId, completedLegIds, outcome)
        }

        if (match.stage == Stage.GROUP.name) {
            handleGroupFixtureResolved(match.tournamentId, tournament)
        } else {
            handleKoFixtureResolved(match, tournament, outcome)
        }
    }

    private suspend fun upsertFixtureStats(
        tournamentId: Long,
        playerId: Long,
        completedLegIds: List<Long>,
        outcome: FixtureOutcome
    ) {
        val existing = statsRepository.getStatsForPlayerOnce(playerId, tournamentId)
        val goalsScored = statsRepository.countGoalsForPlayerInMatches(completedLegIds, playerId)
        val ownGoals = statsRepository.countOwnGoalsByPlayerInMatches(completedLegIds, playerId)
        val isWin = outcome.winnerId == playerId
        // A penalty shootout only decides who advances/tops the table — the 90 minutes were
        // still level, so it's recorded as a draw with a one-point bonus for the shootout
        // winner (2/1), not a full win/loss (3/0).
        val isPenaltyDecider = outcome.decidedByPenalties
        val isRegulationDraw = outcome.isDraw
        val isLoss = !isWin && !isRegulationDraw && !isPenaltyDecider
        val gf = outcome.goalsFor[playerId] ?: 0
        val ga = outcome.goalsAgainst[playerId] ?: 0

        statsRepository.upsertStats(
            TournamentStatsEntity(
                playerId = playerId,
                tournamentId = tournamentId,
                goals = (existing?.goals ?: 0) + goalsScored,
                ownGoals = (existing?.ownGoals ?: 0) + ownGoals,
                wins = (existing?.wins ?: 0) + if (isWin && !isPenaltyDecider) 1 else 0,
                draws = (existing?.draws ?: 0) + if (isRegulationDraw || isPenaltyDecider) 1 else 0,
                losses = (existing?.losses ?: 0) + if (isLoss) 1 else 0,
                goalsFor = (existing?.goalsFor ?: 0) + gf,
                goalsAgainst = (existing?.goalsAgainst ?: 0) + ga,
                points = (existing?.points ?: 0) + when {
                    isPenaltyDecider && isWin -> 2
                    isPenaltyDecider -> 1
                    isWin -> 3
                    isRegulationDraw -> 1
                    else -> 0
                },
                finalPosition = existing?.finalPosition
            )
        )
    }

    private suspend fun handleGroupFixtureResolved(tournamentId: Long, tournament: TournamentEntity) {
        val allMatches = matchRepository.getMatchesForTournament(tournamentId).first()
        val groupMatches = allMatches.filter { it.stage == Stage.GROUP.name }
        val allDone = groupMatches.isNotEmpty() && groupMatches.all { it.status == "COMPLETED" || it.status == "SKIPPED" }
        if (!allDone) return

        val players = matchRepository.getPlayersForTournament(tournamentId).first()
        val format = TournamentFormat.valueOf(tournament.format)
        val standings = calculateStandingsUseCase.invoke(
            matches = allMatches,
            matchesPerFixture = tournament.matchesPerFixture,
            totalGroupFixturesPerPlayer = players.size - 1,
            qualifiedCount = format.groupQualifierCount,
            tournamentId = tournamentId
        )
        standings.forEach { statsRepository.setFinalPosition(it.playerId, tournamentId, it.rank) }

        advanceBracketUseCase.seedFromGroupStandings(tournamentId, format, standings)

        // Round Robin has no QUARTER/SEMI rounds — its single FINAL stub is now seeded and
        // playable, so there is no bracket progression to wait on beyond that match completing.
    }

    private suspend fun handleKoFixtureResolved(match: MatchEntity, tournament: TournamentEntity, outcome: FixtureOutcome) {
        val winnerId = outcome.winnerId ?: return
        val isFinal = advanceBracketUseCase.advanceWinner(match.tournamentId, match.stage, match.matchNumber, winnerId)
        if (isFinal) {
            completeTournament(match.tournamentId, winnerId)
        }
    }

    private suspend fun completeTournament(tournamentId: Long, winnerId: Long) {
        val finalStats = statsRepository.getStandingsForTournamentOnce(tournamentId)
        val mvp = finalStats.maxWithOrNull(
            compareBy<TournamentStatsEntity> { it.goals }
                .thenBy { it.goalsFor - it.goalsAgainst }
                .thenBy { -it.ownGoals }
        )
        val mvpId = mvp?.playerId ?: winnerId
        tournamentRepository.completeTournament(tournamentId, winnerId, mvpId)

        // finalPosition was last set from the GROUP stage alone (handleGroupFixtureResolved,
        // before the knockout rounds/final even played) and never revisited since — re-rank now
        // that every fixture including the final has contributed points. The actual champion is
        // pinned to rank 1 outright: two separate fixtures (e.g. a group leg + a rematch final)
        // can legitimately land the runner-up on equal points, but who lifted the trophy isn't a
        // tiebreak question.
        val reordered = finalStats.sortedWith(
            compareByDescending<TournamentStatsEntity> { it.playerId == winnerId }
                .thenByDescending { it.points }
                .thenByDescending { it.goalsFor - it.goalsAgainst }
                .thenByDescending { it.goalsFor }
        )
        reordered.forEachIndexed { index, stat ->
            statsRepository.setFinalPosition(stat.playerId, tournamentId, index + 1)
        }
    }
}
