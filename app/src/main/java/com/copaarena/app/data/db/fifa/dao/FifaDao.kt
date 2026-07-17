package com.copaarena.app.data.db.fifa.dao

import androidx.room.Dao
import androidx.room.Query
import com.copaarena.app.data.db.fifa.entity.ClubDbEntity
import com.copaarena.app.data.db.fifa.entity.LeagueDbEntity

@Dao
interface FifaDao {
    @Query("SELECT * FROM leagues ORDER BY league_level ASC, league_name ASC")
    suspend fun getAllLeagues(): List<LeagueDbEntity>

    @Query("SELECT * FROM leagues WHERE league_id = :leagueId LIMIT 1")
    suspend fun getLeagueById(leagueId: Int): LeagueDbEntity?

    @Query("SELECT * FROM clubs WHERE league_id = :leagueId ORDER BY club_name ASC")
    suspend fun getClubsByLeague(leagueId: Int): List<ClubDbEntity>

    @Query("SELECT * FROM clubs WHERE club_team_id = :clubTeamId LIMIT 1")
    suspend fun getClubById(clubTeamId: Int): ClubDbEntity?

    @Query("SELECT * FROM clubs WHERE club_name LIKE '%' || :query || '%' ORDER BY club_name ASC LIMIT 50")
    suspend fun searchClubs(query: String): List<ClubDbEntity>

    @Query("SELECT * FROM players WHERE club_team_id = :clubTeamId ORDER BY overall DESC")
    suspend fun getPlayersByClub(clubTeamId: Int): List<com.copaarena.app.data.db.fifa.entity.PlayerDbEntity>

    @Query("SELECT * FROM players WHERE nationality_id = :nationalityId ORDER BY overall DESC")
    suspend fun getPlayersByNationality(nationalityId: Int): List<com.copaarena.app.data.db.fifa.entity.PlayerDbEntity>
}
