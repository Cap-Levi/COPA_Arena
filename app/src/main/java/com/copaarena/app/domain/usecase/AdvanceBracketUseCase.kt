package com.copaarena.app.domain.usecase

import com.copaarena.app.data.repository.MatchRepository
import com.copaarena.app.domain.model.Stage
import com.copaarena.app.domain.model.Standing
import com.copaarena.app.domain.model.TournamentFormat
import javax.inject.Inject

/** Seeds KO stubs from final group standings and advances winners round-to-round,
 * preserving bracket-seed separation (top seeds can't meet before the final). */
class AdvanceBracketUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    suspend fun seedFromGroupStandings(tournamentId: Long, format: TournamentFormat, finalStandings: List<Standing>) {
        val seeds = finalStandings.sortedBy { it.rank }.map { it.playerId }
        when (format) {
            TournamentFormat.ROUND_ROBIN -> {
                if (seeds.size >= 2) {
                    matchRepository.seedFixture(tournamentId, Stage.FINAL.name, 1, seeds[0], seeds[1])
                }
            }
            TournamentFormat.SEMIFINALS -> {
                if (seeds.size >= 4) {
                    matchRepository.seedFixture(tournamentId, Stage.SEMI.name, 1, seeds[0], seeds[3]) // 1v4
                    matchRepository.seedFixture(tournamentId, Stage.SEMI.name, 2, seeds[1], seeds[2]) // 2v3
                }
            }
            TournamentFormat.QUARTERFINALS -> {
                if (seeds.size >= 8) {
                    matchRepository.seedFixture(tournamentId, Stage.QUARTER.name, 1, seeds[0], seeds[7]) // 1v8
                    matchRepository.seedFixture(tournamentId, Stage.QUARTER.name, 2, seeds[1], seeds[6]) // 2v7
                    matchRepository.seedFixture(tournamentId, Stage.QUARTER.name, 3, seeds[2], seeds[5]) // 3v6
                    matchRepository.seedFixture(tournamentId, Stage.QUARTER.name, 4, seeds[3], seeds[4]) // 4v5
                }
            }
        }
    }

    /** Advances a KO fixture's winner into its next-round slot. Returns true if [completedStage]
     * was FINAL (i.e. the tournament itself is now decided) so the caller can complete it. */
    suspend fun advanceWinner(tournamentId: Long, completedStage: String, completedMatchNumber: Int, winnerId: Long): Boolean {
        when (completedStage) {
            Stage.QUARTER.name -> {
                when (completedMatchNumber) {
                    1 -> matchRepository.fillSlotA(tournamentId, Stage.SEMI.name, 1, winnerId)
                    4 -> matchRepository.fillSlotB(tournamentId, Stage.SEMI.name, 1, winnerId)
                    2 -> matchRepository.fillSlotA(tournamentId, Stage.SEMI.name, 2, winnerId)
                    3 -> matchRepository.fillSlotB(tournamentId, Stage.SEMI.name, 2, winnerId)
                }
                return false
            }
            Stage.SEMI.name -> {
                when (completedMatchNumber) {
                    1 -> matchRepository.fillSlotA(tournamentId, Stage.FINAL.name, 1, winnerId)
                    2 -> matchRepository.fillSlotB(tournamentId, Stage.FINAL.name, 1, winnerId)
                }
                return false
            }
            Stage.FINAL.name -> return true
            else -> return false
        }
    }
}
