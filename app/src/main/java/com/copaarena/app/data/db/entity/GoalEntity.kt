package com.copaarena.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "goals",
    foreignKeys = [ForeignKey(entity = MatchEntity::class,
        parentColumns = ["id"], childColumns = ["matchId"],
        onDelete = ForeignKey.CASCADE)],
    indices = [Index("matchId")])
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val scorerId: Long,                  // PlayerEntity id; -1 for own goal crediting opponent
    val fifaPlayerName: String? = null,  // Real world scorer from FIFA roster
    val creditedToId: Long,              // who gets the goal counted
    val isOwnGoal: Boolean = false,
    val minute: Int? = null,             // optional — user can skip
    val timestamp: Long = System.currentTimeMillis()
)
