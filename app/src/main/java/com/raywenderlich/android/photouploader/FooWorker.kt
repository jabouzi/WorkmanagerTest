package com.skanderjabouzi.workmanager

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import androidx.work.WorkerParameters
import com.skanderjabouzi.workmanager.MainViewModel.Companion.PROGRESS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOG_TAG = "### FooWorker"

class FooWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                (0..100 step 10).forEach {
                    setProgressAsync(workDataOf(PROGRESS to it))
                    Thread.sleep(1000, 0)
                }
                Log.d(LOG_TAG, "Success!")
                Result.success()
            }
        } catch(e: Throwable) {
            Log.e(LOG_TAG, "Error executing work: " + e.message, e)
            Result.failure()
        }
    }
}