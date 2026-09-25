package cn.felix021.navigateur

import android.app.Application
import android.util.Log

class NavigateurApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
