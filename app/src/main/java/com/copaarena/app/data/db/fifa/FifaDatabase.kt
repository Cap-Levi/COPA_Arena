package com.copaarena.app.data.db.fifa

import androidx.room.Database
import androidx.room.RoomDatabase
import com.copaarena.app.data.db.fifa.dao.FifaDao
import com.copaarena.app.data.db.fifa.entity.ClubDbEntity
import com.copaarena.app.data.db.fifa.entity.LeagueDbEntity
import com.copaarena.app.data.db.fifa.entity.NationDbEntity
import com.copaarena.app.data.db.fifa.entity.PlayerDbEntity

@Database(
    entities = [
        LeagueDbEntity::class,
        ClubDbEntity::class,
        NationDbEntity::class,
        PlayerDbEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FifaDatabase : RoomDatabase() {
    abstract fun fifaDao(): FifaDao
}
