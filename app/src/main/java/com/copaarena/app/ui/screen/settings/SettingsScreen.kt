package com.copaarena.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticEnabled.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val dataSize by viewModel.dataSize.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SETTINGS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AccentGold
                    )
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
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Preferences Section ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "PREFERENCES",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AccentGold
                )
            }
            item {
                CopaCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sound Effects", fontWeight = FontWeight.Bold, color = OnBackground)
                                Text("Goal cheers & whistle", style = MaterialTheme.typography.bodySmall, color = OnBackground.copy(alpha = 0.4f))
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { viewModel.toggleSound(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentGold, checkedThumbColor = OnPrimary)
                            )
                        }
                        HorizontalDivider(color = SurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Haptic Feedback", fontWeight = FontWeight.Bold, color = OnBackground)
                                Text("Vibration on goals & confirms", style = MaterialTheme.typography.bodySmall, color = OnBackground.copy(alpha = 0.4f))
                            }
                            Switch(
                                checked = hapticEnabled,
                                onCheckedChange = { viewModel.toggleHaptic(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentGold, checkedThumbColor = OnPrimary)
                            )
                        }
                        HorizontalDivider(color = SurfaceVariant)
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text("App Theme", fontWeight = FontWeight.Bold, color = OnBackground)
                            Spacer(modifier = Modifier.height(8.dp))
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                listOf("DARK", "LIGHT", "SYSTEM").forEachIndexed { index, theme ->
                                    SegmentedButton(
                                        selected = themePreference == theme,
                                        onClick = { viewModel.setTheme(theme) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = 3
                                        ),
                                        colors = SegmentedButtonDefaults.colors(
                                            activeContainerColor = AccentGold.copy(alpha = 0.15f),
                                            activeContentColor = AccentGold,
                                            inactiveContainerColor = Color.Transparent,
                                            inactiveContentColor = OnBackground.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text(theme, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Data Section ──
            item {
                Text(
                    "DATA",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AccentGold
                )
            }
            item {
                CopaCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("Clear All Tournament Data", color = ErrorColor, modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            "Currently using $dataSize",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                }
            }

            // ── About Section ──
            item {
                Text(
                    "ABOUT",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AccentGold
                )
            }
            item {
                CopaCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COPA Arena", fontWeight = FontWeight.Bold, color = OnBackground)
                        Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = OnBackground.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Built with ❤️ for FIFA couch gaming", color = OnBackground.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Created by Levi", style = MaterialTheme.typography.bodySmall, color = OnBackground.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "View on GitHub",
                            color = AccentGold,
                            modifier = Modifier.clickable { uriHandler.openUri("https://github.com") }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Are you sure?", color = OnBackground) },
                text = {
                    Column {
                        Text("This cannot be undone. Type DELETE to confirm.", color = OnBackground.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deleteConfirmationText,
                            onValueChange = { deleteConfirmationText = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ErrorColor,
                                cursorColor = ErrorColor
                            )
                        )
                    }
                },
                containerColor = Surface,
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllData()
                            showDeleteConfirm = false
                            deleteConfirmationText = ""
                        },
                        enabled = deleteConfirmationText == "DELETE"
                    ) { Text("Confirm", color = ErrorColor) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = OnBackground) }
                }
            )
        }
    }
}
