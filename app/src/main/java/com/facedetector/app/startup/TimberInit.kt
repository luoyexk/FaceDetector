package com.facedetector.app.startup

import android.content.Context
import androidx.startup.Initializer
import com.facedetector.BuildConfig
import timber.log.Timber

class TimberInit : Initializer<Unit> {
    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        Timber.d("zzzz timber init")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

    class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // save log
        }
    }
}