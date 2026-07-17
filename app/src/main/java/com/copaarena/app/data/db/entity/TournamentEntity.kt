package com.copaarena.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: Long,                      // epoch millis
    val format: String,                  // TournamentFormat enum name
    val matchesPerFixture: Int,          // 1, 2, or 3
    val status: String,                  // SETUP, ACTIVE, COMPLETED
    val winnerId: Long? = null,
    val mvpPlayerId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
