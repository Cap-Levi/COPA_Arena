package com.copaarena.app.data.repository

import com.copaarena.app.data.db.dao.MatchDao
import com.copaarena.app.data.db.dao.PlayerDao
import com.copaarena.app.data.db.entity.MatchEntity
import com.copaarena.app.data.db.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepository @Inject constructor(
    private val matchDao: MatchDao,
    private val playerDao: PlayerDao
) {
    suspend fun insertAll(matches: List<MatchEntity>): List<Long> {
        return matchDao.insertAll(matches)
    }
    
    suspend fun insertPlayers(players: List<PlayerEntity>): List<Long> {
        return playerDao.insertAll(players)
    }

    fun getPlayersForTournament(tid: Long): Flow<List<PlayerEntity>> {
        return playerDao.getPlayersForTournament(tid)
    }

    fun getMatchesForTournament(id: Long): Flow<List<MatchEntity>> {
        return matchDao.getMatchesForTournament(id)
    }

    fun getMatchById(id: Long): Flow<MatchEntity?> {
        return matchDao.getMatchById(id)
    }

    suspend fun updateMatchResult(matchId: Long, goalsA: Int, goalsB: Int, status: String, winnerId: Long?, draw: Boolean) {
        matchDao.updateResult(matchId, goalsA, goalsB, status, winnerId, draw)
    }

    suspend fun updateMatchResult(matchId: Long, goalsA: Int, goalsB: Int, status: String, winnerId: Long?, draw: Boolean, penaltyGoalsA: Int?, penaltyGoalsB: Int?) {
        matchDao.updateResultWithPenalties(matchId, goalsA, goalsB, status, winnerId, draw, penaltyGoalsA, penaltyGoalsB)
    }

    suspend fun getHeadToHead(tournamentId: Long, p1: Long, p2: Long): List<MatchEntity> {
        return matchDao.getHeadToHead(tournamentId, p1, p2)
    }

    suspend fun getMatchByIdOnce(id: Long): MatchEntity? = matchDao.getMatchByIdOnce(id)

    suspend fun getFixtureLegs(tournamentId: Long, stage: String, matchNumber: Int): List<MatchEntity> =
        matchDao.getFixtureLegs(tournamentId, stage, matchNumber)

    suspend fun markSkipped(matchId: Long) = matchDao.markSkipped(matchId)

    suspend fun fillSlotA(tournamentId: Long, stage: String, matchNumber: Int, playerId: Long) =
        matchDao.fillSlotA(tournamentId, stage, matchNumber, playerId)

    suspend fun fillSlotB(tournamentId: Long, stage: String, matchNumber: Int, playerId: Long) =
        matchDao.fillSlotB(tournamentId, stage, matchNumber, playerId)

    suspend fun seedFixture(tournamentId: Long, stage: String, matchNumber: Int, slotAPlayerId: Long, slotBPlayerId: Long) {
        matchDao.fillSlotA(tournamentId, stage, matchNumber, slotAPlayerId)
        matchDao.fillSlotB(tournamentId, stage, matchNumber, slotBPlayerId)
    }
}
