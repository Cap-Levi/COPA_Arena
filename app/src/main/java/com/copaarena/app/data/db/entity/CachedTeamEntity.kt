package com.copaarena.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_teams")
data class CachedTeamEntity(
    @PrimaryKey val teamId: Int,
    val name: String,
    val badgeUrl: String,
    val overall: Int,
    val league: String,
    val leagueId: Int? = null,
    val nation: String,
    val cachedAt: Long = System.currentTimeMillis()
)
