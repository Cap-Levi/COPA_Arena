package com.copaarena.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "players",
    foreignKeys = [ForeignKey(entity = TournamentEntity::class,
        parentColumns = ["id"], childColumns = ["tournamentId"],
        onDelete = ForeignKey.CASCADE)],
    indices = [Index("tournamentId")])
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val name: String,
    val teamName: String,
    val teamId: Int = 0,                 // fc26.db club_team_id
    val teamBadgeUrl: String,            // remote URL or local asset path
    val teamOverall: Int,                // FC26 overall rating
    val seed: Int                        // seeding order for bracket generation
)
