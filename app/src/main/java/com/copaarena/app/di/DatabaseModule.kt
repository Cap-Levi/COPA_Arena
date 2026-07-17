package com.copaarena.app.di

import android.content.Context
import androidx.room.Room
import com.copaarena.app.data.db.AppDatabase
import com.copaarena.app.data.db.dao.*
import com.copaarena.app.data.db.fifa.FifaDatabase
import com.copaarena.app.data.db.fifa.dao.FifaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "copa_arena.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTournamentDao(database: AppDatabase): TournamentDao = database.tournamentDao()

    @Provides
    fun providePlayerDao(database: AppDatabase): PlayerDao = database.playerDao()

    @Provides
    fun provideMatchDao(database: AppDatabase): MatchDao = database.matchDao()

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideStatsDao(database: AppDatabase): StatsDao = database.statsDao()

    @Provides
    fun provideTeamCacheDao(database: AppDatabase): TeamCacheDao {
        return database.teamCacheDao()
    }

    @Provides
    @Singleton
    fun provideFifaDatabase(@ApplicationContext context: Context): FifaDatabase {
        return Room.databaseBuilder(
            context,
            FifaDatabase::class.java,
            "fc26.db"
        ).createFromAsset("databases/fc26.db")
         .fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    fun provideFifaDao(database: FifaDatabase): FifaDao = database.fifaDao()
}
