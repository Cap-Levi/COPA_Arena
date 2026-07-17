package com.copaarena.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.copaarena.app.data.db.entity.TournamentEntity
import kotlinx.coroutines.flow.Flow

data class TournamentWinsSummary(
    val playerName: String,
    val tournamentsWon: Int,
    val lastTeamName: String?,
    val lastTeamBadgeUrl: String?
)

@Dao
interface TournamentDao {
    @Insert
    suspend fun insert(t: TournamentEntity): Long
    
    @Update
    suspend fun update(t: TournamentEntity)
    
    @Delete
    suspend fun delete(t: TournamentEntity)
    
    @Query("SELECT * FROM tournaments WHERE status = 'ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    fun getActiveTournament(): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournaments WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>
    
    @Query("SELECT * FROM tournaments WHERE id = :id")
    fun getTournamentById(id: Long): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentByIdOnce(id: Long): TournamentEntity?
    
    @Query("UPDATE tournaments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    
    @Query("UPDATE tournaments SET winnerId = :winnerId, mvpPlayerId = :mvpId, status = 'COMPLETED' WHERE id = :id")
    suspend fun completeTournament(id: Long, winnerId: Long, mvpId: Long)

    @Query("DELETE FROM tournaments")
    suspend fun deleteAll()

    // Grouped by player name (not id) — same reasoning as StatsDao.getGlobalPlayerSummary:
    // a winner's own PlayerEntity id/team is fixed per-tournament, so counting/aggregating
    // "how many times has this person won overall" has to key on their name across rows.
    @Query("""
        SELECT p.name as playerName,
               COUNT(*) as tournamentsWon,
               (SELECT p2.teamName FROM tournaments t2
                INNER JOIN players p2 ON t2.winnerId = p2.id
                WHERE p2.name = p.name AND t2.status = 'COMPLETED'
                ORDER BY t2.createdAt DESC LIMIT 1) as lastTeamName,
               (SELECT p3.teamBadgeUrl FROM tournaments t3
                INNER JOIN players p3 ON t3.winnerId = p3.id
                WHERE p3.name = p.name AND t3.status = 'COMPLETED'
                ORDER BY t3.createdAt DESC LIMIT 1) as lastTeamBadgeUrl
        FROM tournaments t
        INNER JOIN players p ON t.winnerId = p.id
        WHERE t.status = 'COMPLETED'
        GROUP BY p.name
        ORDER BY tournamentsWon DESC
    """)
    fun getTournamentWinsSummary(): Flow<List<TournamentWinsSummary>>
}
