package com.copaarena.app.utils

import android.content.Context
import android.media.MediaPlayer
import com.copaarena.app.R
import com.copaarena.app.data.datastore.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    private var goalCheerPlayer: MediaPlayer? = null
    private var whistlePlayer: MediaPlayer? = null
    private var fanfarePlayer: MediaPlayer? = null

    init {
        try {
            goalCheerPlayer = MediaPlayer.create(context, R.raw.goal_cheer)
            whistlePlayer = MediaPlayer.create(context, R.raw.whistle)
            fanfarePlayer = MediaPlayer.create(context, R.raw.champion_fanfare)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing MediaPlayer")
        }
    }

    fun playGoalCheer() {
        playSound(goalCheerPlayer)
    }

    fun playWhistle() {
        playSound(whistlePlayer)
    }

    fun playChampionFanfare() {
        playSound(fanfarePlayer)
    }

    private fun playSound(player: MediaPlayer?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val soundEnabled = settingsDataStore.soundEnabledFlow.first()
                if (soundEnabled && player != null) {
                    if (player.isPlaying) {
                        player.seekTo(0)
                    } else {
                        player.start()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error playing sound")
            }
        }
    }

    fun release() {
        try {
            goalCheerPlayer?.release()
            whistlePlayer?.release()
            fanfarePlayer?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error releasing MediaPlayer")
        } finally {
            goalCheerPlayer = null
            whistlePlayer = null
            fanfarePlayer = null
        }
    }
}
