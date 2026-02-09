package com.example.ledgerscanner

import android.app.Application
import androidx.work.Configuration
import com.example.ledgerscanner.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("opencv_java4")
        } catch (_: UnsatisfiedLinkError) {
        }

        // Schedule periodic sync every 15 minutes
        syncManager.schedulePeriodicSync()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
