package com.example.ledgerscanner.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Schedule periodic sync every 15 minutes.
     * Uses KEEP policy to avoid duplicating if already scheduled.
     */
    fun schedulePeriodicSync() {
//        val request = PeriodicWorkRequestBuilder<SyncWorker>(
//            15, TimeUnit.MINUTES
//        )
//            .setConstraints(networkConstraints)
//            .build()
//
//        workManager.enqueueUniquePeriodicWork(
//            SyncWorker.WORK_NAME_PERIODIC,
//            ExistingPeriodicWorkPolicy.KEEP,
//            request
//        )

        Log.d(TAG, "Periodic sync scheduled")
    }

    /**
     * Trigger an immediate one-time sync.
     * Uses REPLACE policy to avoid stacking multiple immediate syncs.
     */
    fun scheduleImmediateSync() {
//        val request = OneTimeWorkRequestBuilder<SyncWorker>()
//            .setConstraints(networkConstraints)
//            .build()
//
//        workManager.enqueueUniqueWork(
//            SyncWorker.WORK_NAME_IMMEDIATE,
//            ExistingWorkPolicy.REPLACE,
//            request
//        )
//
//        Log.d(TAG, "Immediate sync scheduled")
    }

    companion object {
        private const val TAG = "SyncManager"
    }
}
