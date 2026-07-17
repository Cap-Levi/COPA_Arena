package com.copaarena.app.domain.model

data class Standing(
    val rank: Int,
    val playerId: Long,
    val matchesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val points: Int,
    val isQualified: Boolean = false,
    val isEliminated: Boolean = false
)
