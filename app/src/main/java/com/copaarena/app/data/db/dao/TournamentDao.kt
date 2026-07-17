package com.copaarena.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.copaarena.app.data.db.entity.TournamentEntity
import kotlinx.coroutines.flow.Flow

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
}
