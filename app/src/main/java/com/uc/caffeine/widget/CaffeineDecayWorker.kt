package com.uc.caffeine.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CaffeineDecayWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        CaffeineWidgetUpdater.update(applicationContext)
        return Result.success()
    }
}
