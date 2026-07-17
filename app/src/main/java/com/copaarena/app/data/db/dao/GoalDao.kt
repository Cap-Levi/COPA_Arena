package com.copaarena.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.copaarena.app.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

data class TopScorerResult(val playerName: String, val goals: Int)
data class PlayerMatchGoal(val matchId: Long, val goals: Int)
data class PlayerGoalCount(val playerId: Long, val goals: Int)

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(g: GoalEntity): Long
    
    @Delete
    suspend fun delete(g: GoalEntity)
    
    @Query("SELECT * FROM goals WHERE matchId = :mid ORDER BY timestamp ASC")
    fun getGoalsForMatch(mid: Long): Flow<List<GoalEntity>>
    
    @Query("SELECT COALESCE(g.fifaPlayerName, p.name) as playerName, COUNT(*) as goals FROM goals g INNER JOIN players p ON g.creditedToId = p.id WHERE g.matchId IN (SELECT id FROM matches WHERE tournamentId = :tid) AND g.isOwnGoal = 0 GROUP BY COALESCE(g.fifaPlayerName, p.name) ORDER BY goals DESC")
    fun getTopScorersForTournament(tid: Long): Flow<List<TopScorerResult>>

    @Query("SELECT COALESCE(g.fifaPlayerName, p.name) as playerName, COUNT(*) as goals FROM goals g INNER JOIN players p ON g.creditedToId = p.id WHERE g.isOwnGoal = 0 GROUP BY COALESCE(g.fifaPlayerName, p.name) ORDER BY goals DESC LIMIT 10")
    fun getGlobalTopScorers(): Flow<List<TopScorerResult>>

    @Query("SELECT creditedToId as playerId, COUNT(*) as goals FROM goals WHERE isOwnGoal = 0 AND matchId IN (SELECT id FROM matches WHERE tournamentId = :tid) GROUP BY creditedToId")
    fun getGoalCountsForTournament(tid: Long): Flow<List<PlayerGoalCount>>

    @Query("SELECT COUNT(*) FROM goals WHERE matchId IN (:matchIds) AND creditedToId = :playerId AND isOwnGoal = 0")
    suspend fun countGoalsForPlayerInMatches(matchIds: List<Long>, playerId: Long): Int

    @Query("SELECT COUNT(*) FROM goals WHERE matchId IN (:matchIds) AND scorerId = :playerId AND isOwnGoal = 1")
    suspend fun countOwnGoalsByPlayerInMatches(matchIds: List<Long>, playerId: Long): Int

    @Query("SELECT matchId, COUNT(*) as goals FROM goals WHERE creditedToId = :pid AND isOwnGoal = 0 AND matchId IN (SELECT id FROM matches WHERE tournamentId = :tid) GROUP BY matchId")
    fun getGoalsByMatchForPlayer(tid: Long, pid: Long): Flow<List<PlayerMatchGoal>>
}
