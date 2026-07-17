package com.copaarena.app.data.repository

import com.copaarena.app.data.db.dao.GlobalPlayerSummary
import com.copaarena.app.data.db.dao.GoalDao
import com.copaarena.app.data.db.dao.PlayerGoalCount
import com.copaarena.app.data.db.dao.PlayerMatchGoal
import com.copaarena.app.data.db.dao.StatsDao
import com.copaarena.app.data.db.dao.TopScorerResult
import com.copaarena.app.data.db.entity.GoalEntity
import com.copaarena.app.data.db.entity.TournamentStatsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val statsDao: StatsDao,
    private val goalDao: GoalDao
) {
    fun getStandingsForTournament(id: Long): Flow<List<TournamentStatsEntity>> {
        return statsDao.getStandingsForTournament(id)
    }

    fun getGlobalStats(): Flow<List<GlobalPlayerSummary>> {
        return statsDao.getGlobalPlayerSummary()
    }

    fun getTopScorers(tournamentId: Long? = null): Flow<List<TopScorerResult>> {
        return if (tournamentId != null) {
            goalDao.getTopScorersForTournament(tournamentId)
        } else {
            goalDao.getGlobalTopScorers()
        }
    }
    
    suspend fun upsertStats(stats: TournamentStatsEntity) {
        statsDao.upsert(stats)
    }
    
    suspend fun insertGoal(goal: GoalEntity) {
        goalDao.insert(goal)
    }
    
    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.delete(goal)
    }
    
    fun getGoalsForMatch(matchId: Long): Flow<List<GoalEntity>> {
        return goalDao.getGoalsForMatch(matchId)
    }

    fun getGoalsByMatchForPlayer(tournamentId: Long, playerId: Long): Flow<List<PlayerMatchGoal>> {
        return goalDao.getGoalsByMatchForPlayer(tournamentId, playerId)
    }

    fun getGoalCountsForTournament(tournamentId: Long): Flow<List<PlayerGoalCount>> {
        return goalDao.getGoalCountsForTournament(tournamentId)
    }

    suspend fun countGoalsForPlayerInMatches(matchIds: List<Long>, playerId: Long): Int =
        goalDao.countGoalsForPlayerInMatches(matchIds, playerId)

    suspend fun countOwnGoalsByPlayerInMatches(matchIds: List<Long>, playerId: Long): Int =
        goalDao.countOwnGoalsByPlayerInMatches(matchIds, playerId)

    suspend fun getStatsForPlayerOnce(playerId: Long, tournamentId: Long): TournamentStatsEntity? =
        statsDao.getStatsForPlayerInTournament(playerId, tournamentId)

    suspend fun getStandingsForTournamentOnce(tournamentId: Long): List<TournamentStatsEntity> =
        statsDao.getStandingsForTournamentOnce(tournamentId)

    suspend fun setFinalPosition(playerId: Long, tournamentId: Long, position: Int) =
        statsDao.setFinalPosition(playerId, tournamentId, position)
}
