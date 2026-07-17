package com.copaarena.app.data.db.fifa.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerDbEntity(
    @PrimaryKey
    @ColumnInfo(name = "player_id") val playerId: Int,
    @ColumnInfo(name = "short_name") val shortName: String?,
    @ColumnInfo(name = "long_name") val longName: String?,
    @ColumnInfo(name = "player_positions") val playerPositions: String?,
    @ColumnInfo(name = "overall") val overall: Int?,
    @ColumnInfo(name = "potential") val potential: Int?,
    @ColumnInfo(name = "value_eur") val valueEur: Int?,
    @ColumnInfo(name = "wage_eur") val wageEur: Int?,
    @ColumnInfo(name = "age") val age: Int?,
    @ColumnInfo(name = "height_cm") val heightCm: Int?,
    @ColumnInfo(name = "weight_kg") val weightKg: Int?,
    @ColumnInfo(name = "preferred_foot") val preferredFoot: String?,
    @ColumnInfo(name = "weak_foot") val weakFoot: Int?,
    @ColumnInfo(name = "skill_moves") val skillMoves: Int?,
    @ColumnInfo(name = "international_reputation") val internationalReputation: Int?,
    @ColumnInfo(name = "work_rate") val workRate: String?,
    @ColumnInfo(name = "body_type") val bodyType: String?,
    @ColumnInfo(name = "real_face") val realFace: String?,
    @ColumnInfo(name = "release_clause_eur") val releaseClauseEur: Int?,
    @ColumnInfo(name = "pace") val pace: Int?,
    @ColumnInfo(name = "shooting") val shooting: Int?,
    @ColumnInfo(name = "passing") val passing: Int?,
    @ColumnInfo(name = "dribbling") val dribbling: Int?,
    @ColumnInfo(name = "defending") val defending: Int?,
    @ColumnInfo(name = "physic") val physic: Int?,
    @ColumnInfo(name = "club_team_id") val clubTeamId: Int?,
    @ColumnInfo(name = "league_id") val leagueId: Int?,
    @ColumnInfo(name = "nationality_id") val nationalityId: Int?
)
