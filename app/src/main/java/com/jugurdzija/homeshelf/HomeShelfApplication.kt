package com.jugurdzija.homeshelf

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class HomeShelfApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenCVLoader.initLocal()
    }
}
