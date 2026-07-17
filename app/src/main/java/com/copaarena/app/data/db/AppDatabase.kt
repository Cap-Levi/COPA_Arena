package com.copaarena.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.copaarena.app.data.db.dao.*
import com.copaarena.app.data.db.entity.*

@Database(
    entities = [
        TournamentEntity::class, 
        PlayerEntity::class, 
        MatchEntity::class,
        GoalEntity::class, 
        TournamentStatsEntity::class, 
        CachedTeamEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun goalDao(): GoalDao
    abstract fun statsDao(): StatsDao
    abstract fun teamCacheDao(): TeamCacheDao
}
