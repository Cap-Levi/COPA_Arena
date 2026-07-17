package com.copaarena.app.domain.usecase

import com.copaarena.app.data.repository.TournamentRepository
import com.copaarena.app.domain.model.TournamentFormat
import javax.inject.Inject

class CreateTournamentUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository
) {
    suspend operator fun invoke(name: String, date: Long, format: TournamentFormat, matchesPerFixture: Int): Long {
        return tournamentRepository.createTournament(
            name = name,
            date = date,
            format = format.name,
            matchesPerFixture = matchesPerFixture
        )
    }
}
