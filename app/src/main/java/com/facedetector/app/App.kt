package com.facedetector.app

import android.app.Application
import com.facedetector.BuildConfig
import com.facedetector.app.startup.TimberInit
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(TimberInit.ReleaseTree())
        }
    }
}