package com.copaarena.app.data.db.fifa.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nations")
data class NationDbEntity(
    @PrimaryKey
    @ColumnInfo(name = "nationality_id") val nationalityId: Int,
    @ColumnInfo(name = "nationality_name") val nationalityName: String?,
    @ColumnInfo(name = "nation_team_id") val nationTeamId: Int?
)
