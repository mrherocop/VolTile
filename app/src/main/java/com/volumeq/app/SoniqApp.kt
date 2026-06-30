package com.volumeq.app

import android.app.Application
import android.content.Context
import kotlin.system.exitProcess

class SoniqApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("crash_log", throwable.stackTraceToString()).commit()
            defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(1)
        }
    }
}
