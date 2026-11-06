package cn.neeto.guodelist.core.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.settings.AppSettings
import cn.neeto.guodelist.core.util.Dates
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Reconciles WorkManager with the task list. All reminders carry one tag, so a
 * single cancel-and-enqueue pass keeps deletions, completions and due-date
 * edits in step with what will actually ring.
 */
object ReminderScheduler {

    private const val TAG = "guodelist-reminder"

    fun reconcile(context: Context, tasks: List<TaskItem>, settings: AppSettings) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG)
        if (!settings.remindersEnabled) return

        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val leadSeconds = settings.reminderLeadMinutes * 60L

        tasks.forEach { task ->
            if (task.done) return@forEach
            val date = Dates.parse(task.dueDate) ?: return@forEach
            val time = Dates.parseTime(task.dueTime) ?: LocalTime.of(9, 0)
            val due = date.atTime(time).atZone(zone).toInstant()
            if (!due.isAfter(now)) return@forEach

            val trigger = due.minusSeconds(leadSeconds)
            val delayMillis = Duration.between(now, trigger).toMillis().coerceAtLeast(0L)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_TASK_ID to task.id,
                        ReminderWorker.KEY_TITLE to task.title,
                        ReminderWorker.KEY_DUE_DATE to task.dueDate.orEmpty(),
                        ReminderWorker.KEY_DUE_TIME to task.dueTime.orEmpty(),
                    ),
                )
                .addTag(TAG)
                .build()

            workManager.enqueueUniqueWork("reminder:${task.id}", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }
}
