package com.dermalens.app.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleDailyReminder(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val reminderRequest = PeriodicWorkRequestBuilder<ScanReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ScanReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }

    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ScanReminderWorker.WORK_NAME)
    }

    fun scheduleTestReminder(context: Context) {
        val testRequest = OneTimeWorkRequestBuilder<ScanReminderWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(testRequest)
    }
}

/** Schedules/cancels [ContributionUploadWorker], gated on the same "Contribute to Research"
 *  consent toggle as the local save step in ScanResultScreen.kt. */
object ContributionUploadScheduler {

    fun scheduleUpload(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi (or otherwise unmetered) only
            .build()

        val uploadRequest = PeriodicWorkRequestBuilder<ContributionUploadWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ContributionUploadWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            uploadRequest
        )
    }

    fun cancelUpload(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ContributionUploadWorker.WORK_NAME)
    }
}