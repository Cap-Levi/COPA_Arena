package com.copaarena.app.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copaarena.app.data.datastore.SettingsDataStore
import com.copaarena.app.data.db.AppDatabase
import com.copaarena.app.data.repository.TournamentRepository
import com.copaarena.app.data.repository.UpdateCheckResult
import com.copaarena.app.data.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val tournamentRepository: TournamentRepository,
    private val appDatabase: AppDatabase,
    private val updateRepository: UpdateRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    val soundEnabled: StateFlow<Boolean> = settingsDataStore.soundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hapticEnabled: StateFlow<Boolean> = settingsDataStore.hapticEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themePreference: StateFlow<String> = settingsDataStore.themePreferenceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")

    private val _dataSize = MutableStateFlow("0 KB")
    val dataSize: StateFlow<String> = _dataSize.asStateFlow()

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    init {
        refreshDataSize()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            _updateCheckState.value = when (val result = updateRepository.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable ->
                    UpdateCheckState.UpdateAvailable(result.latestVersion, result.releaseUrl)
                is UpdateCheckResult.UpToDate -> UpdateCheckState.UpToDate
                is UpdateCheckResult.Error -> UpdateCheckState.Error(result.message)
            }
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setSoundEnabled(enabled) }
    }

    fun toggleHaptic(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setHapticEnabled(enabled) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsDataStore.setThemePreference(theme) }
    }

    fun refreshDataSize() {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                listOf("copa_arena.db", "copa_arena.db-wal", "copa_arena.db-shm")
                    .sumOf { appContext.getDatabasePath(it).let { f -> if (f.exists()) f.length() else 0L } }
            }
            _dataSize.value = formatBytes(bytes)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            tournamentRepository.deleteAllTournaments()
            withContext(Dispatchers.IO) {
                // DELETEs free pages internally but don't shrink the file on disk;
                // VACUUM rebuilds the file so the reported size reflects actual usage.
                appDatabase.openHelper.writableDatabase.execSQL("VACUUM")
            }
            // Show 0 immediately — all tournament data is gone, so the readout shouldn't
            // wait on a file-size probe that will still report a few KB of empty schema.
            _dataSize.value = "0.0 KB"
        }
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        if (kb < 1000) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1000) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
}

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    object UpToDate : UpdateCheckState()
    data class UpdateAvailable(val version: String, val url: String) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}
