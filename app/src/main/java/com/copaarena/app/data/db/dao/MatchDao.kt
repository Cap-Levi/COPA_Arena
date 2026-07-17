package com.copaarena.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.copaarena.app.data.db.entity.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Insert
    suspend fun insertAll(matches: List<MatchEntity>): List<Long>
    
    @Update
    suspend fun update(m: MatchEntity)
    
    @Query("SELECT * FROM matches WHERE tournamentId = :tid ORDER BY stage, matchNumber, leg ASC")
    fun getMatchesForTournament(tid: Long): Flow<List<MatchEntity>>
    
    @Query("SELECT * FROM matches WHERE id = :id")
    fun getMatchById(id: Long): Flow<MatchEntity?>
    
    @Query("SELECT * FROM matches WHERE tournamentId = :tid AND stage = :stage")
    fun getMatchesByStage(tid: Long, stage: String): Flow<List<MatchEntity>>
    
    @Query("UPDATE matches SET goalsA = :ga, goalsB = :gb, status = :status, winnerId = :wid, isDraw = :draw WHERE id = :id")
    suspend fun updateResult(id: Long, ga: Int, gb: Int, status: String, wid: Long?, draw: Boolean)

    @Query("UPDATE matches SET goalsA = :ga, goalsB = :gb, status = :status, winnerId = :wid, isDraw = :draw, penaltyGoalsA = :penA, penaltyGoalsB = :penB WHERE id = :id")
    suspend fun updateResultWithPenalties(id: Long, ga: Int, gb: Int, status: String, wid: Long?, draw: Boolean, penA: Int?, penB: Int?)

    @Query("SELECT * FROM matches WHERE tournamentId = :tid AND ((playerAId = :p1 AND playerBId = :p2) OR (playerAId = :p2 AND playerBId = :p1))")
    suspend fun getHeadToHead(tid: Long, p1: Long, p2: Long): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE tournamentId = :tid AND stage = :stage AND matchNumber = :num ORDER BY leg ASC")
    suspend fun getFixtureLegs(tid: Long, stage: String, num: Int): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchByIdOnce(id: Long): MatchEntity?

    @Query("UPDATE matches SET status = 'SKIPPED' WHERE id = :id")
    suspend fun markSkipped(id: Long)

    // Fills one bracket "slot" of a fixture across all its legs, preserving the odd/even
    // leg home-away parity convention used everywhere else. Slot A plays home on odd legs
    // and away on even legs; Slot B is the mirror. Two independent slot fills let group-stage
    // seeding (both known at once) and KO round-to-round advancement (one slot known at a time)
    // share the same mechanism.
    @Query("""UPDATE matches SET
        playerAId = CASE WHEN leg % 2 = 1 THEN :playerId ELSE playerAId END,
        playerBId = CASE WHEN leg % 2 = 0 THEN :playerId ELSE playerBId END
        WHERE tournamentId = :tid AND stage = :stage AND matchNumber = :num""")
    suspend fun fillSlotA(tid: Long, stage: String, num: Int, playerId: Long)

    @Query("""UPDATE matches SET
        playerBId = CASE WHEN leg % 2 = 1 THEN :playerId ELSE playerBId END,
        playerAId = CASE WHEN leg % 2 = 0 THEN :playerId ELSE playerAId END
        WHERE tournamentId = :tid AND stage = :stage AND matchNumber = :num""")
    suspend fun fillSlotB(tid: Long, stage: String, num: Int, playerId: Long)
}
