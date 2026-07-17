package com.copaarena.app.data.db.fifa.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clubs")
data class ClubDbEntity(
    @PrimaryKey
    @ColumnInfo(name = "club_team_id") val clubTeamId: Int,
    @ColumnInfo(name = "club_name") val clubName: String?,
    @ColumnInfo(name = "league_id") val leagueId: Int?
)
