package com.copaarena.app.domain.model

enum class TournamentFormat {
    ROUND_ROBIN,
    SEMIFINALS,
    QUARTERFINALS;

    val groupQualifierCount: Int
        get() = when (this) {
            ROUND_ROBIN -> 2
            SEMIFINALS -> 4
            QUARTERFINALS -> 8
        }
}
