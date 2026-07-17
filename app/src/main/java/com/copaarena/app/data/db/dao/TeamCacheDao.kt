package com.copaarena.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.copaarena.app.data.db.entity.CachedTeamEntity

@Dao
interface TeamCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(t: CachedTeamEntity)
    
    @Query("SELECT * FROM cached_teams WHERE name LIKE '%' || :query || '%' AND (:leagueId IS NULL OR leagueId = :leagueId) LIMIT 30")
    suspend fun searchTeams(query: String, leagueId: Int? = null): List<CachedTeamEntity>
    
    @Query("SELECT * FROM cached_teams WHERE teamId = :id")
    suspend fun getTeamById(id: Int): CachedTeamEntity?

    @Query("SELECT COUNT(*) FROM cached_teams")
    suspend fun getTeamCount(): Int
}
