package com.vent.app

import android.app.Application
import com.vent.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@VentApplication)
            modules(appModule)
        }
    }
}
