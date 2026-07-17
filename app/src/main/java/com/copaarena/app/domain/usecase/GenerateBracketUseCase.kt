package com.copaarena.app.domain.usecase

import com.copaarena.app.data.db.entity.MatchEntity
import com.copaarena.app.domain.model.Stage
import com.copaarena.app.domain.model.TournamentFormat
import javax.inject.Inject

class GenerateBracketUseCase @Inject constructor() {
    operator fun invoke(
        tournamentId: Long,
        players: List<com.copaarena.app.data.db.entity.PlayerEntity>,
        format: TournamentFormat,
        matchesPerFixture: Int
    ): List<MatchEntity> {
        val matches = mutableListOf<MatchEntity>()
        val n = players.size
        
        // Step 1: GROUP STAGE fixtures
        var matchNumber = 1
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val fixtureNumber = matchNumber++
                for (leg in 1..matchesPerFixture) {
                    val p1 = if (leg % 2 != 0) players[i].id else players[j].id
                    val p2 = if (leg % 2 != 0) players[j].id else players[i].id

                    matches.add(
                        MatchEntity(
                            tournamentId = tournamentId,
                            stage = Stage.GROUP.name,
                            matchNumber = fixtureNumber,
                            playerAId = p1,
                            playerBId = p2,
                            leg = leg,
                            status = "PENDING"
                        )
                    )
                }
            }
        }
        
        // Step 2: KO STUB generation
        val TBD_PLAYER_ID = -1L
        
        when (format) {
            TournamentFormat.ROUND_ROBIN -> {
                // ROUND_ROBIN with final only -> 1 FINAL stub per leg
                for (leg in 1..matchesPerFixture) {
                    matches.add(MatchEntity(tournamentId = tournamentId, stage = Stage.FINAL.name, matchNumber = 1, playerAId = TBD_PLAYER_ID, playerBId = TBD_PLAYER_ID, leg = leg))
                }
            }
            TournamentFormat.SEMIFINALS -> {
                for (leg in 1..matchesPerFixture) {
                    matches.add(MatchEntity(tournamentId = tournamentId, stage = Stage.SEMI.name, matchNumber = 1, playerAId = TBD_PLAYER_ID, playerBId = TBD_PLAYER_ID, leg = leg))
                    matches.add(MatchEntity(tournamentId = tournamentId, stage = Stage.SEMI.name, matchNumber = 2, playerAId = TBD_PLAYER_ID, playerBId = TBD_PLAYER_ID, leg = leg))
                    matches.add(MatchEntity(tournamentId = tournamentId, stage = Stage.FINAL.name, matchNumber = 1, playerAId = TBD_PLAYER_ID, playerBId = TBD_PLAYER_ID, leg = leg))
                }
            }
            TournamentFormat.QUARTERFINALS -> {
                for (leg in 1..matchesPerFixture) {
                    for (i in 1..4) {
                        matches.add(MatchEntity(tournamentId = tournamentId, stage = Stage.QUARTER.name, matchNumber = i, playerAId = TBD_PLAYER_ID, playerBId = TBD_PLAYER_ID, leg = leg))
                    }
                    matches.add(MatchEntity(tournamentId = tournamentId, stage = Stage.SEMI.name, matchNumber = 1, playerAId = TBD_PLAYER_ID, playerBId = TBD_PLAYER_ID, leg = leg))
                    matches.add(MatchEntity(tournamentId = tournamentId, stage = Stage.SEMI.name, matchNumber = 2, playerAId = TBD_PLAYER_ID, playerBId = TBD_PLAYER_ID, leg = leg))
                    matches.add(MatchEntity(tournamentId = tournamentId, stage = Stage.FINAL.name, matchNumber = 1, playerAId = TBD_PLAYER_ID, playerBId = TBD_PLAYER_ID, leg = leg))
                }
            }
        }
        
        return matches
    }
}
