package com.copaarena.app.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.imageLoader
import coil.request.ImageRequest
import com.copaarena.app.data.db.entity.CachedTeamEntity
import com.copaarena.app.data.db.entity.PlayerEntity
import com.copaarena.app.data.repository.TeamRepository
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.components.TeamBadge
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayersScreen(
    navController: NavController,
    sharedViewModel: CreateTournamentViewModel,
    viewModel: AddPlayersViewModel = hiltViewModel()
) {
    val players by sharedViewModel.players.collectAsStateWithLifecycle()
    val isRestartMode by sharedViewModel.isRestartMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val allLeagues by viewModel.leagues.collectAsStateWithLifecycle()

    // Warm Coil's memory cache for every league badge as soon as the league list is
    // available (well before the user opens the League dropdown), so decoding ~50 small
    // PNGs doesn't all happen synchronously the moment the dropdown expands.
    val context = LocalContext.current
    LaunchedEffect(allLeagues) {
        val loader = context.imageLoader
        allLeagues.forEach { league ->
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(TeamRepository.leagueBadgeAssetUri(league.leagueId))
                    .build()
            )
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var newPlayerName by remember { mutableStateOf("") }
    var selectedPlayerToEdit by remember { mutableStateOf<PlayerEntity?>(null) }
    var selectedPlayerIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTeam by remember { mutableStateOf<CachedTeamEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isRestartMode) "UPDATE TEAMS" else "ADD PLAYERS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AccentGold
                        )
                        if (!isRestartMode) {
                            Text(
                                "${players.size} player${if (players.size != 1) "s" else ""} added",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (!isRestartMode) {
                Button(
                    onClick = {
                        selectedPlayerToEdit = null
                        selectedPlayerIndex = null
                        newPlayerName = ""
                        selectedTeam = null
                        viewModel.setSelectedLeagueId(null)
                        viewModel.setSearchQuery("")
                        showBottomSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Player", tint = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Player", color = OnBackground, fontWeight = FontWeight.Bold)
                }
            }

            if (players.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = OnBackground.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No players added yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = OnBackground.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap the button above to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnBackground.copy(alpha = 0.25f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(players) { index, player ->
                        PlayerCard(
                            player = player,
                            isRestartMode = isRestartMode,
                            onRemove = { sharedViewModel.removePlayer(player) },
                            onClick = {
                                selectedPlayerToEdit = player
                                selectedPlayerIndex = index
                                newPlayerName = if (!isRestartMode) player.name else ""
                                selectedTeam = null
                                viewModel.setSelectedLeagueId(null)
                                viewModel.setSearchQuery("")
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    navController.navigate(Screen.SelectFormat.createRoute(0L))
                },
                enabled = players.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    disabledContainerColor = SurfaceVariant
                )
            ) {
                Text("Next → Select Format", color = OnPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Bottom Sheet ──
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    newPlayerName = ""
                    selectedPlayerToEdit = null
                    selectedPlayerIndex = null
                    selectedTeam = null
                    viewModel.setSelectedLeagueId(null)
                    viewModel.setSearchQuery("")
                },
                sheetState = sheetState,
                containerColor = Surface
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                    Text(
                        when {
                            isRestartMode -> "CHANGE TEAM"
                            selectedPlayerIndex != null -> "EDIT PLAYER"
                            else -> "ADD NEW PLAYER"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = AccentGold
                    )
                    if (isRestartMode && selectedPlayerToEdit != null) {
                        Text(
                            "For: ${selectedPlayerToEdit?.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBackground.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isRestartMode) {
                        OutlinedTextField(
                            value = newPlayerName,
                            onValueChange = { newPlayerName = it },
                            label = { Text("Player Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = SurfaceVariant,
                                cursorColor = AccentGold,
                                focusedLabelColor = AccentGold
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // League dropdown
                    val selectedLeagueId by viewModel.selectedLeagueId.collectAsStateWithLifecycle()
                    var expanded by remember { mutableStateOf(false) }
                    val popularLeagues = remember(allLeagues) {
                        listOf(null to "All Leagues") + allLeagues.map { league ->
                            val rawName = league.leagueName ?: "Unknown"
                            league.leagueId to TeamRepository.leagueDisplayName(league.leagueId, rawName)
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        val currentLeagueName = popularLeagues.find { it.first == selectedLeagueId }?.second ?: "All Leagues"
                        Box(modifier = Modifier.menuAnchor().fillMaxWidth()) {
                            OutlinedTextField(
                                value = currentLeagueName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("League") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGold,
                                    unfocusedBorderColor = SurfaceVariant,
                                    focusedLabelColor = AccentGold
                                )
                            )
                            // A plain readOnly OutlinedTextField still requests focus (and pops
                            // the soft keyboard) on tap in some Compose/IME combos. A transparent
                            // click-catcher on top intercepts the touch before the field itself
                            // ever gets it, so it never focuses.
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { expanded = true }
                            )
                        }
                        if (expanded) {
                            Popup(
                                onDismissRequest = { expanded = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    modifier = Modifier.exposedDropdownSize().heightIn(max = 320.dp),
                                    shadowElevation = 3.dp,
                                    tonalElevation = 3.dp,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    LazyColumn {
                                        items(popularLeagues, key = { it.first ?: -1 }) { (id, name) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (id != null) {
                                                            TeamBadge(
                                                                badgeUrl = TeamRepository.leagueBadgeAssetUri(id),
                                                                teamName = name,
                                                                size = 24.dp
                                                            )
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                        }
                                                        Text(name)
                                                    }
                                                },
                                                onClick = {
                                                    // Only a genuine user-picked league change should
                                                    // clear the team — a programmatic prefill (editing
                                                    // an existing player) sets league+team together and
                                                    // must NOT have this wipe the team right back out.
                                                    viewModel.setSelectedLeagueId(id)
                                                    selectedTeam = null
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Editing an existing (not-yet-created) player: load their current
                    // league+team so the form isn't blank.
                    LaunchedEffect(selectedPlayerToEdit) {
                        val editing = selectedPlayerToEdit
                        if (editing != null && !isRestartMode) {
                            viewModel.prefillForEdit(editing.teamId) { team ->
                                selectedTeam = team
                            }
                        }
                    }

                    if (selectedLeagueId != null) {
                        var teamExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = teamExpanded,
                            onExpandedChange = { teamExpanded = !teamExpanded }
                        ) {
                            Box(modifier = Modifier.menuAnchor().fillMaxWidth()) {
                                // readOnly display only — like the League field, this is a pure
                                // selector. Typing to filter happens in a dedicated search box
                                // inside the opened list below, not on the anchor itself, so
                                // re-tapping an already-picked team reopens the picker instead of
                                // dropping you into text-edit mode on the current selection.
                                OutlinedTextField(
                                    value = selectedTeam?.name ?: "Select Team",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Team") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentGold,
                                        unfocusedBorderColor = SurfaceVariant,
                                        focusedLabelColor = AccentGold
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { teamExpanded = true }
                                )
                            }

                            if (teamExpanded) {
                                Popup(
                                    onDismissRequest = { teamExpanded = false },
                                    properties = PopupProperties(focusable = true)
                                ) {
                                    Surface(
                                        modifier = Modifier.exposedDropdownSize().heightIn(max = 380.dp),
                                        shadowElevation = 3.dp,
                                        tonalElevation = 3.dp,
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Column {
                                            OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = { viewModel.setSearchQuery(it) },
                                                placeholder = { Text("Search teams…") },
                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = AccentGold,
                                                    unfocusedBorderColor = SurfaceVariant,
                                                    cursorColor = AccentGold
                                                )
                                            )
                                            when {
                                                isLoading -> DropdownMenuItem(
                                                    text = { Text("Loading teams…") },
                                                    onClick = {}
                                                )
                                                searchResults.isEmpty() -> DropdownMenuItem(
                                                    text = { Text("No teams found") },
                                                    onClick = {}
                                                )
                                                else -> LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                                    items(searchResults, key = { it.teamId }) { team ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    TeamBadge(
                                                                        badgeUrl = team.badgeUrl,
                                                                        teamName = team.name,
                                                                        size = 28.dp
                                                                    )
                                                                    Spacer(modifier = Modifier.width(10.dp))
                                                                    Text(team.name, fontWeight = FontWeight.Bold)
                                                                }
                                                            },
                                                            onClick = {
                                                                selectedTeam = team
                                                                teamExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        Text(
                            "Select a league first — every player must have a valid team",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorColor
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Button(
                        onClick = {
                            if (isRestartMode && selectedPlayerIndex != null && selectedTeam != null) {
                                sharedViewModel.updatePlayerTeamAt(
                                    index = selectedPlayerIndex!!,
                                    newTeamName = selectedTeam!!.name,
                                    newBadgeUrl = selectedTeam!!.badgeUrl,
                                    newOverall = selectedTeam!!.overall,
                                    newTeamId = selectedTeam!!.teamId
                                )
                                showBottomSheet = false
                                selectedPlayerToEdit = null
                                selectedPlayerIndex = null
                                viewModel.setSelectedLeagueId(null)
                                viewModel.setSearchQuery("")
                                selectedTeam = null
                            } else if (!isRestartMode && newPlayerName.isNotBlank() && selectedTeam != null) {
                                if (selectedPlayerIndex != null) {
                                    sharedViewModel.updatePlayerAt(
                                        index = selectedPlayerIndex!!,
                                        newName = newPlayerName,
                                        newTeamName = selectedTeam!!.name,
                                        newBadgeUrl = selectedTeam!!.badgeUrl,
                                        newOverall = selectedTeam!!.overall,
                                        newTeamId = selectedTeam!!.teamId
                                    )
                                } else {
                                    sharedViewModel.addPlayer(
                                        PlayerEntity(
                                            tournamentId = 0L,
                                            name = newPlayerName,
                                            teamName = selectedTeam!!.name,
                                            teamId = selectedTeam!!.teamId,
                                            teamBadgeUrl = selectedTeam!!.badgeUrl,
                                            teamOverall = selectedTeam!!.overall,
                                            seed = 0
                                        )
                                    )
                                }
                                showBottomSheet = false
                                newPlayerName = ""
                                selectedPlayerToEdit = null
                                selectedPlayerIndex = null
                                viewModel.setSelectedLeagueId(null)
                                viewModel.setSearchQuery("")
                                selectedTeam = null
                            }
                        },
                        enabled = selectedTeam != null && (isRestartMode || newPlayerName.isNotBlank()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            disabledContainerColor = SurfaceVariant
                        )
                    ) {
                        Text(
                            when {
                                isRestartMode -> "Update Team"
                                selectedPlayerIndex != null -> "Save Changes"
                                else -> "Add Player"
                            },
                            color = OnPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerCard(player: PlayerEntity, isRestartMode: Boolean, onRemove: () -> Unit, onClick: () -> Unit) {
    CopaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row with remove button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!isRestartMode) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = ErrorColor, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }
            }

            // Player initial
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.name.firstOrNull()?.uppercase() ?: "?",
                    fontFamily = BebasNeue,
                    fontSize = 20.sp,
                    color = AccentGold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                player.name,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                player.teamName,
                style = MaterialTheme.typography.bodySmall,
                color = OnBackground.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TeamListItem(team: CachedTeamEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(team.name, fontWeight = FontWeight.Bold, color = OnBackground)
            Text(
                "${team.league} • OVR: ${team.overall}",
                style = MaterialTheme.typography.bodySmall,
                color = OnBackground.copy(alpha = 0.4f)
            )
        }
    }
}
