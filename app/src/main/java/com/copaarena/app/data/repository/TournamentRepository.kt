package com.copaarena.app.data.repository

import com.copaarena.app.data.db.dao.TournamentDao
import com.copaarena.app.data.db.dao.TournamentWinsSummary
import com.copaarena.app.data.db.entity.TournamentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TournamentRepository @Inject constructor(
    private val tournamentDao: TournamentDao
) {
    suspend fun createTournament(name: String, date: Long, format: String, matchesPerFixture: Int): Long {
        val tournament = TournamentEntity(
            name = name,
            date = date,
            format = format,
            matchesPerFixture = matchesPerFixture,
            status = "SETUP"
        )
        return tournamentDao.insert(tournament)
    }

    fun getActiveTournament(): Flow<TournamentEntity?> {
        return tournamentDao.getActiveTournament()
    }

    fun getActiveTournaments(): Flow<List<TournamentEntity>> {
        return tournamentDao.getActiveTournaments()
    }

    fun getAllTournaments(): Flow<List<TournamentEntity>> {
        return tournamentDao.getAllTournaments()
    }

    fun getTournamentById(id: Long): Flow<TournamentEntity?> {
        return tournamentDao.getTournamentById(id)
    }

    suspend fun getTournamentByIdOnce(id: Long): TournamentEntity? {
        return tournamentDao.getTournamentByIdOnce(id)
    }

    suspend fun updateStatus(id: Long, status: String) {
        tournamentDao.updateStatus(id, status)
    }

    suspend fun completeTournament(id: Long, winnerId: Long, mvpId: Long) {
        tournamentDao.completeTournament(id, winnerId, mvpId)
    }

    suspend fun deleteTournament(tournament: TournamentEntity) {
        tournamentDao.delete(tournament)
    }

    suspend fun deleteAllTournaments() {
        tournamentDao.deleteAll()
    }

    fun getTournamentWinsSummary(): Flow<List<TournamentWinsSummary>> {
        return tournamentDao.getTournamentWinsSummary()
    }
}
