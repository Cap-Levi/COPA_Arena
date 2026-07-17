package com.copaarena.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CopaarenaApp : Application() {

    @Inject
    lateinit var soundManager: com.copaarena.app.utils.SoundManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        soundManager.release()
    }
}
