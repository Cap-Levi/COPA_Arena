package com.copaarena.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.copaarena.app.ui.theme.AccentGold

/**
 * Renders a team/club badge from a bundled local asset (see `TeamRepository.teamBadgeAssetUri`).
 * Not every club in fc26.db has a downloaded badge, so this always falls back to the existing
 * initial-letter circle style (matching PlayerMatchCard etc.) on load failure or a blank URL —
 * never shows an empty box. Uses `rememberAsyncImagePainter` (plain Painter) rather than
 * `SubcomposeAsyncImage` — the latter needs a nested composition pass per instance, which is
 * cheap for one badge but measurably janks a dropdown that renders dozens of these at once.
 */
@Composable
fun TeamBadge(
    badgeUrl: String?,
    teamName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    backgroundColor: Color = AccentGold.copy(alpha = 0.15f),
    textColor: Color = AccentGold
) {
    if (badgeUrl.isNullOrBlank()) {
        InitialBadge(teamName, modifier, size, backgroundColor, textColor)
        return
    }
    val painter = rememberAsyncImagePainter(model = badgeUrl, contentScale = ContentScale.Fit)
    Box(modifier = modifier.size(size)) {
        // AsyncImagePainter only starts its request once it's actually drawn (its onDraw
        // callback), so Image must always be composed — never gated behind `state == Success` —
        // or the load never begins in the first place. The fallback sits underneath and shows
        // through while state is Empty/Loading/Error; the real image draws over it on success.
        if (painter.state !is AsyncImagePainter.State.Success) {
            InitialBadge(teamName, Modifier, size, backgroundColor, textColor)
        }
        // No clip here — badge aspect ratio isn't always circular (banner-style league
        // logos in particular), and clipping would crop real content. Any stray white
        // background is stripped from the source asset itself (see the offline
        // corner-flood-fill pass over app/src/main/assets/badges), not masked at render time.
        Image(
            painter = painter,
            contentDescription = teamName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun InitialBadge(
    teamName: String,
    modifier: Modifier,
    size: Dp,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            teamName.firstOrNull()?.uppercase() ?: "?",
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
