package com.copaarena.app.data.db.fifa.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leagues")
data class LeagueDbEntity(
    @PrimaryKey
    @ColumnInfo(name = "league_id") val leagueId: Int,
    @ColumnInfo(name = "league_name") val leagueName: String?,
    @ColumnInfo(name = "league_level") val leagueLevel: Int?
)
