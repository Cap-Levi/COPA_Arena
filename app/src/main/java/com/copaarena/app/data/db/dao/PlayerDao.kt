package com.copaarena.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.copaarena.app.data.db.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert
    suspend fun insertAll(players: List<PlayerEntity>): List<Long>
    
    @Query("SELECT * FROM players WHERE tournamentId = :tid ORDER BY seed ASC")
    fun getPlayersForTournament(tid: Long): Flow<List<PlayerEntity>>
    
    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): PlayerEntity?
}
