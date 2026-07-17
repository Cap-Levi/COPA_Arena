package com.copaarena.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.copaarena.app.data.db.entity.TournamentStatsEntity
import kotlinx.coroutines.flow.Flow

data class GlobalPlayerSummary(val playerName: String, val totalWins: Int, val totalMatches: Int, val totalPoints: Int)

@Dao
interface StatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: TournamentStatsEntity)
    
    @Query("SELECT * FROM tournament_stats WHERE tournamentId = :tid ORDER BY finalPosition ASC, points DESC, goalsFor - goalsAgainst DESC")
    fun getStandingsForTournament(tid: Long): Flow<List<TournamentStatsEntity>>

    @Query("SELECT * FROM tournament_stats WHERE tournamentId = :tid ORDER BY finalPosition ASC, points DESC, goalsFor - goalsAgainst DESC")
    suspend fun getStandingsForTournamentOnce(tid: Long): List<TournamentStatsEntity>

    @Query("SELECT * FROM tournament_stats WHERE playerId = :pid")
    fun getStatsForPlayer(pid: Long): Flow<List<TournamentStatsEntity>>

    @Query("SELECT * FROM tournament_stats WHERE playerId = :pid AND tournamentId = :tid")
    suspend fun getStatsForPlayerInTournament(pid: Long, tid: Long): TournamentStatsEntity?

    @Query("UPDATE tournament_stats SET finalPosition = :position WHERE playerId = :pid AND tournamentId = :tid")
    suspend fun setFinalPosition(pid: Long, tid: Long, position: Int)
    
    @Query("SELECT p.name as playerName, SUM(s.wins) as totalWins, SUM(s.wins + s.draws + s.losses) as totalMatches, SUM(s.points) as totalPoints FROM tournament_stats s INNER JOIN players p ON s.playerId = p.id GROUP BY p.name ORDER BY totalPoints DESC")
    fun getGlobalPlayerSummary(): Flow<List<GlobalPlayerSummary>>
}
