package com.example.ledgerscanner.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun syncRepository(): SyncRepository
}

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(appContext, workerParams) {

    // Fallback constructor for cases when the default WorkManager factory is used.
    constructor(appContext: Context, workerParams: WorkerParameters) : this(
        appContext,
        workerParams,
        EntryPointAccessors.fromApplication(appContext, SyncWorkerEntryPoint::class.java)
            .syncRepository()
    )

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work...")

        // Sync exams first (scan results depend on exams existing on server)
        val examsSynced = syncRepository.syncExams()
        if (!examsSynced) {
            Log.w(TAG, "Exam sync failed, will retry")
            return Result.retry()
        }

        // Then sync scan results with images
        val scanResultsSynced = syncRepository.syncScanResults()
        if (!scanResultsSynced) {
            Log.w(TAG, "Scan results sync failed, will retry")
            return Result.retry()
        }

        Log.d(TAG, "Sync completed successfully")
        return Result.success()
    }

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME_PERIODIC = "periodic_sync"
        const val WORK_NAME_IMMEDIATE = "immediate_sync"
    }
}
