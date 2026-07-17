package com.copaarena.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "tournament_stats",
    primaryKeys = ["playerId", "tournamentId"],
    foreignKeys = [ForeignKey(entity = TournamentEntity::class,
        parentColumns = ["id"], childColumns = ["tournamentId"],
        onDelete = ForeignKey.CASCADE)],
    indices = [Index("tournamentId")])
data class TournamentStatsEntity(
    val playerId: Long,
    val tournamentId: Long,
    val goals: Int = 0,
    val ownGoals: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0,
    val finalPosition: Int? = null
)
