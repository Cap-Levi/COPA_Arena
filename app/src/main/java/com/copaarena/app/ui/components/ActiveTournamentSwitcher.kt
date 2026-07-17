package com.copaarena.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.copaarena.app.data.db.entity.TournamentEntity
import com.copaarena.app.ui.theme.AccentGold
import com.copaarena.app.ui.theme.OnBackground
import com.copaarena.app.ui.theme.SurfaceVariant

/**
 * Lets the user hop between tournaments from a screen's top bar. Always visible — even with
 * zero tournaments, it shows a "No tournaments" placeholder rather than disappearing, so the
 * switcher is a stable fixture of the Bracket/Stats top bars regardless of data state.
 */
@Composable
fun ActiveTournamentSwitcher(
    activeTournaments: List<TournamentEntity>,
    currentTournamentId: Long?,
    onSwitch: (Long) -> Unit
) {
    val canSwitch = activeTournaments.size > 1
    var expanded by remember { mutableStateOf(false) }
    val current = activeTournaments.find { it.id == currentTournamentId }
    val label = current?.name ?: if (activeTournaments.isEmpty()) "No tournaments" else "Switch"

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .let { if (canSwitch) it.clickable { expanded = true } else it }
                .background(SurfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = AccentGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp)
            )
            if (canSwitch) {
                Box(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.UnfoldMore,
                    contentDescription = "Switch tournament",
                    tint = AccentGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            activeTournaments.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Text(
                            t.name,
                            fontWeight = if (t.id == currentTournamentId) FontWeight.Bold else FontWeight.Normal,
                            color = if (t.id == currentTournamentId) AccentGold else OnBackground
                        )
                    },
                    onClick = {
                        expanded = false
                        onSwitch(t.id)
                    }
                )
            }
        }
    }
}
