package com.copaarena.app.data.repository

import com.copaarena.app.data.db.dao.TeamCacheDao
import com.copaarena.app.data.db.entity.CachedTeamEntity
import com.copaarena.app.data.db.fifa.dao.FifaDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository @Inject constructor(
    private val teamCacheDao: TeamCacheDao,
    private val fifaDao: FifaDao
) {
    suspend fun searchTeams(query: String, leagueId: Int? = null): List<CachedTeamEntity> {
        val clubs = if (leagueId != null && query.isBlank()) {
            fifaDao.getClubsByLeague(leagueId)
        } else if (query.isNotBlank()) {
            fifaDao.searchClubs(query)
        } else {
            emptyList()
        }

        val leagueNameCache = mutableMapOf<Int, String>()
        return clubs.map { club ->
            val resolvedLeagueId = club.leagueId
            val leagueName = resolvedLeagueId?.let {
                leagueNameCache.getOrPut(it) {
                    fifaDao.getLeagueById(it)?.leagueName ?: "League $it"
                }
            } ?: "Unknown League"

            CachedTeamEntity(
                teamId = club.clubTeamId,
                name = club.clubName ?: "Unknown",
                badgeUrl = teamBadgeAssetUri(club.clubTeamId),
                overall = 80, // Default since fc26 clubs table doesn't have overall
                league = leagueName,
                leagueId = club.leagueId,
                nation = "Unknown"
            )
        }
    }

    /** Looks up a single club by id — used to pre-fill the league/team pickers when editing
     *  a player who was already assigned a team (e.g. reopening their Add Players card). */
    suspend fun getTeamById(id: Int): CachedTeamEntity? {
        val club = fifaDao.getClubById(id) ?: return null
        val leagueName = club.leagueId?.let { fifaDao.getLeagueById(it)?.leagueName ?: "League $it" } ?: "Unknown League"
        return CachedTeamEntity(
            teamId = club.clubTeamId,
            name = club.clubName ?: "Unknown",
            badgeUrl = teamBadgeAssetUri(club.clubTeamId),
            overall = 80,
            league = leagueName,
            leagueId = club.leagueId,
            nation = "Unknown"
        )
    }

    companion object {
        /** Local asset path for a downloaded club badge (bundled at `assets/badges/teams/{id}.png`).
         * Not every club has a badge — Coil's AsyncImage simply fails to load ones that don't exist,
         * so callers must render a fallback (see `ui/components/TeamBadge.kt`). */
        fun teamBadgeAssetUri(clubTeamId: Int): String = "file:///android_asset/badges/teams/$clubTeamId.png"

        /** Local asset path for a downloaded league badge (bundled at `assets/badges/leagues/{id}.png`). */
        fun leagueBadgeAssetUri(leagueId: Int): String = "file:///android_asset/badges/leagues/$leagueId.png"

        /** fc26.db's `leagues.league_name` has no country/region qualifier, so several distinct
         * leagues share an identical display name (e.g. "Bundesliga" is both Germany's and Austria's,
         * "Super League" covers Greece/Switzerland/China/India). Verified against each league_id's
         * actual club roster (not just the name), since the name alone can't disambiguate. */
        private val LEAGUE_COUNTRY: Map<Int, String> = mapOf(
            1 to "Denmark", 4 to "Belgium", 7 to "Brazil", 10 to "Netherlands",
            13 to "England", 14 to "England", 16 to "France", 17 to "France",
            19 to "Germany", 20 to "Germany", 31 to "Italy", 32 to "Italy",
            39 to "USA", 41 to "Norway", 50 to "Scotland", 53 to "Spain",
            54 to "Spain", 56 to "Sweden", 60 to "England", 61 to "England",
            63 to "Greece", 64 to "Hungary", 65 to "Ireland", 66 to "Poland",
            68 to "Turkey", 80 to "Austria", 83 to "South Korea", 189 to "Switzerland",
            308 to "Portugal", 313 to "Azerbaijan", 317 to "Croatia", 318 to "Cyprus",
            319 to "Czech Republic", 322 to "Finland", 330 to "Romania", 332 to "Ukraine",
            335 to "Chile", 336 to "Colombia", 337 to "Paraguay", 338 to "Uruguay",
            350 to "Saudi Arabia", 351 to "Australia", 353 to "Argentina", 2012 to "China",
            2013 to "UAE", 2017 to "Bolivia", 2018 to "Ecuador", 2019 to "Venezuela",
            2020 to "Peru", 2076 to "Germany", 2149 to "India"
        )

        /** Display name with a country suffix so leagues that share an identical bare name
         * (see [LEAGUE_COUNTRY]) don't look like duplicate/redundant entries in league pickers. */
        fun leagueDisplayName(leagueId: Int, leagueName: String): String =
            LEAGUE_COUNTRY[leagueId]?.let { "$leagueName ($it)" } ?: leagueName
    }

    suspend fun getAllLeagues() = fifaDao.getAllLeagues()

    suspend fun syncLeague(leagueId: Int) {
        // No longer needed, DB is fully offline
    }

    suspend fun getTeamCount(): Int {
        return teamCacheDao.getTeamCount()
    }
}
