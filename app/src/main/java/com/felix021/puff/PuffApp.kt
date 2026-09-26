package com.felix021.puff

import android.app.Application
import android.util.Log

class PuffApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
