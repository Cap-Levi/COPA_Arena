package com.copaarena.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "matches",
    foreignKeys = [ForeignKey(entity = TournamentEntity::class,
        parentColumns = ["id"], childColumns = ["tournamentId"],
        onDelete = ForeignKey.CASCADE)],
    indices = [Index("tournamentId")])
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val stage: String,                   // Stage enum name: GROUP, QUARTER, SEMI, FINAL
    val matchNumber: Int,                // 1-indexed within stage
    val playerAId: Long,
    val playerBId: Long,
    val goalsA: Int = 0,
    val goalsB: Int = 0,
    val status: String = "PENDING",      // PENDING, LIVE, COMPLETED, SKIPPED
    val winnerId: Long? = null,
    val isDraw: Boolean = false,
    val leg: Int = 1,                    // 1, 2, or 3 (for multi-leg fixtures)
    val penaltyGoalsA: Int? = null,
    val penaltyGoalsB: Int? = null
)
