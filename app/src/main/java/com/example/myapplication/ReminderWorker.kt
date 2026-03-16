package com.example.myapplication

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Fires a reminder notification for a single task.
 *
 * Scheduled as a OneTimeWorkRequest with an initialDelay equal to
 * (reminderAt - now). Tagged "reminder-<taskId>" so it can be cancelled by tag.
 *
 * WorkManager persists this job through app kill and device reboots.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(EXTRA_TASK_ID) ?: return Result.failure()
        val taskTitle = inputData.getString(EXTRA_TASK_TITLE) ?: return Result.failure()

        ReminderNotificationHelper.notify(applicationContext, taskId, taskTitle)

        return Result.success()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
    }
}
