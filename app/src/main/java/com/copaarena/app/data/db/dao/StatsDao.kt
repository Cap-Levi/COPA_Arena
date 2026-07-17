package com.copaarena.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.copaarena.app.data.db.entity.TournamentStatsEntity
import kotlinx.coroutines.flow.Flow

data class GlobalPlayerSummary(
    val playerName: String,
    val totalWins: Int,
    val totalMatches: Int,
    val totalPoints: Int,
    val lastTeamName: String?,
    val lastTeamBadgeUrl: String?
)

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
    
    // lastTeamName/lastTeamBadgeUrl are correlated subqueries picking the player's most
    // recent tournament appearance (by tournamentId) — a player's team is fixed per
    // tournament but changes across tournaments, so "their team" for an all-time
    // leaderboard row means "the one they're using most recently," not a single fixed value.
    @Query("""
        SELECT p.name as playerName,
               SUM(s.wins) as totalWins,
               SUM(s.wins + s.draws + s.losses) as totalMatches,
               SUM(s.points) as totalPoints,
               (SELECT p2.teamName FROM tournament_stats s2
                INNER JOIN players p2 ON s2.playerId = p2.id
                WHERE p2.name = p.name ORDER BY s2.tournamentId DESC LIMIT 1) as lastTeamName,
               (SELECT p3.teamBadgeUrl FROM tournament_stats s3
                INNER JOIN players p3 ON s3.playerId = p3.id
                WHERE p3.name = p.name ORDER BY s3.tournamentId DESC LIMIT 1) as lastTeamBadgeUrl
        FROM tournament_stats s
        INNER JOIN players p ON s.playerId = p.id
        GROUP BY p.name
        ORDER BY totalPoints DESC
    """)
    fun getGlobalPlayerSummary(): Flow<List<GlobalPlayerSummary>>
}
